package com.lightningchess.flow

import co.paralleluniverse.fibers.Suspendable
import com.lightningchess.contract.GameContract
import com.lightningchess.contract.GameContract.AbandonSignature
import com.lightningchess.contract.GameContract.AbandonSignature.AbandonPayload
import com.lightningchess.contract.GameContract.Commands
import com.lightningchess.state.GameState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.serialize
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

object AbandonGameFlow {

    @StartableByRPC
    @InitiatingFlow
    class Abandon(val gameId: UUID) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object SIGNING_ABANDON : ProgressTracker.Step("Signing abandon payload")
            object SEARCHING_FOR_STATE : ProgressTracker.Step("Searching for game state")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the opponent's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }
            fun tracker() = ProgressTracker(
                    SIGNING_ABANDON,
                    SEARCHING_FOR_STATE,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val me = serviceHub.myInfo.legalIdentities.single()

            // Stage 1. Sign the abandon message
            progressTracker.currentStep = SIGNING_ABANDON

            val abandonPayload = AbandonPayload(gameId)
            val signedPayload = serviceHub.keyManagementService.sign(abandonPayload.serialize().bytes, me.owningKey)

            val abandonSignature = AbandonSignature(abandonPayload, signedPayload)

            // Stage 2. Searching for the game state
            progressTracker.currentStep = SEARCHING_FOR_STATE

            // search on the vault for the state
            val gameState = serviceHub.vaultService
                            .queryBy(GameState::class.java, QueryCriteria.LinearStateQueryCriteria(null, listOf(gameId)))
                            .states.first()


            // find the opponent - Party
            val opponent = findOpponent(me, gameState.state.data)
            val winner = findWinner(me, gameState.state.data)

            // create output state
            val outputGameState = gameState.state.data.copy(winner = winner)

            val txCommand = Command(Commands.Abandon(abandonSignature), outputGameState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                            .addInputState(gameState)
                            .addOutputState(outputGameState, GameContract.GAME_CONTRACT_ID)
                            .addCommand(txCommand)

            // Stage 3. Sign the transaction.
            progressTracker.currentStep = SIGNING_TRANSACTION

            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS

            // create the session with opponent
            val session = initiateFlow(opponent)

            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, setOf(session), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION

            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }

        private fun findOpponent(me: Party, gameState: GameState): Party {
            return gameState.participants.first { it.owningKey != me.owningKey } as Party
        }

        private fun findWinner(me: Party, gameState: GameState): GameState.Winner {
            return if (gameState.playerA == me) {
                GameState.Winner.PLAYER_B
            } else {
                GameState.Winner.PLAYER_A
            }
        }
     }

    @InitiatedBy(Abandon::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")

        companion object {
            fun tracker() = ProgressTracker(
                    SIGNING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = SIGNING_TRANSACTION

            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val outputGame = stx.tx.outputsOfType<GameState>().single()
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}