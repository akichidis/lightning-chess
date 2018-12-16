package com.lightningchess.state

import com.lightningchess.schema.GameSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

data class GameState(val gameId: UUID,
                        val playerANickname: String,
                        val playerA: Party,
                        val playerB: Party,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = setOf(playerA, playerB).toList()

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is GameSchemaV1 -> GameSchemaV1.PersistentGame(
                    gameId,
                    playerANickname,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(GameSchemaV1)
}
