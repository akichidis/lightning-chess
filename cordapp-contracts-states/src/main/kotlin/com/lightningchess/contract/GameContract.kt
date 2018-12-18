package com.lightningchess.contract

import com.lightningchess.state.GameState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.DigitalSignature
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.util.*

open class GameContract : Contract {
    companion object {
        @JvmStatic
        val GAME_CONTRACT_ID = "com.lightningchess.contract.GameContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val inputGameStates = tx.inputsOfType<GameState>()
        val outputGameStates = tx.outputsOfType<GameState>()

        val command = tx.commands.requireSingleCommand<Commands>()

        when(command.value) {

            //Create a new game
            is Commands.Create -> {

                requireThat {
                    "No inputs should be consumed when creating a new game." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                }

            }

            //A game is finished - abandon
            is Commands.Abandon -> {
                requireThat {
                    "Only one GameState input should be given" using (inputGameStates.size == 1)
                    "Only one output GameState should be created." using (outputGameStates.size == 1)
                    "Input state should be unfinished" using (inputGameStates[0].winner == GameState.Winner.NOT_FINISHED)
                    "Input & output state linear ids should be the same" using (inputGameStates[0].linearId == inputGameStates[1].linearId)

                    val abandonSigner = (command.value as Commands.Abandon).abandonSignature.signature.by
                    val participants = inputGameStates[0].participants

                    "Abandon signer should be one of the players." using (participants.map { it.owningKey }.contains(abandonSigner))

                    val winner = getWinner(outputGameStates[0])

                    "Abandon signer can not be the winner" using (abandonSigner != winner.owningKey)
                }
            }

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    private fun getWinner(gameState: GameState): Party {
        return if (gameState.winner == GameState.Winner.PLAYER_A) {
            gameState.playerA
        } else {
            gameState.playerB
        }
    }

    @CordaSerializable
    data class AbandonSignature(val payload: AbandonPayload,
                                val signature: DigitalSignature.WithKey) {

        @CordaSerializable
        data class AbandonPayload(val gameId: UUID) {
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Abandon(val abandonSignature: AbandonSignature): Commands
    }
}