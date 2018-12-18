package com.lightningchess.state

import com.lightningchess.schema.GameSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.util.*

data class GameState(val playerANickname: String,
                     val playerA: Party,
                     val playerB: Party,
                     val winner: Winner = Winner.NOT_FINISHED,
                     override val linearId: UniqueIdentifier = UniqueIdentifier(),
                     val gameId: UUID = linearId.id):
        LinearState, QueryableState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = setOf(playerA, playerB).toList()

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is GameSchemaV1 -> GameSchemaV1.PersistentGame(
                    gameId,
                    winner,
                    playerANickname,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(GameSchemaV1)

    @CordaSerializable
    enum class Winner(val value:String) {
        PLAYER_A("PlayerA"),
        PLAYER_B("PlayerB"),
        DRAW("Draw"),
        NOT_FINISHED("Not finished")
    }
}
