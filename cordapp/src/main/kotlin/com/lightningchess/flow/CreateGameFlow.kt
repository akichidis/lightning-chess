package com.lightningchess.flow

import co.paralleluniverse.fibers.Suspendable
import com.lightningchess.contract.GameContract
import com.lightningchess.state.GameState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

/**
 * This flow is executed when an entity (player) initiates a new game against another entity of the network (opponent)
 */
object CreateGameFlow {

    @StartableByRPC
    @InitiatingFlow
    class Initiator(var playerANickname: String,
                    val opponent: Party) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
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
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val me = serviceHub.myInfo.legalIdentities.single()

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION

            val session = initiateFlow(opponent)

            //Creating the output state
            val gameState = GameState(UUID.randomUUID(), playerANickname, me, opponent)

            val txCommand = Command(GameContract.Commands.Create(), gameState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(gameState, GameContract.GAME_CONTRACT_ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            //Stage 3.
            progressTracker.currentStep = GATHERING_SIGS
            val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, setOf(session), GATHERING_SIGS.childProgressTracker()))

            // Stage 4.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(CreateGameFlow.Initiator::class)
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