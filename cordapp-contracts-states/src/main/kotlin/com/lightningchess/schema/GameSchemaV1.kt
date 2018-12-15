package com.lightningchess.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for GameState.
 */
object GameSchema

/**
 * A GameState schema.
 */
object GameSchemaV1 : MappedSchema(
        schemaFamily = GameSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentGame::class.java)) {
    @Entity
    @Table(name = "game_states")
    class PersistentGame(
            @Column(name = "gameId")
            var gameId: UUID,

            @Column(name = "player_a_nickname")
            var playerANickname: String,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(UUID.randomUUID(), "", UUID.randomUUID())
    }
}