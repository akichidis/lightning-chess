package com.lightningchess.contract

import com.lightningchess.state.GameState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

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
        val inputStates = tx.inputStates
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

            //A game is finished
            is Commands.Finish -> {

            }

            else -> throw IllegalArgumentException("Unrecognised command")
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Finish: Commands
    }
}