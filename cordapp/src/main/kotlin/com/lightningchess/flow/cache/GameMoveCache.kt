package com.lightningchess.flow.cache

import com.lightningchess.flow.SignedGameMove
import java.util.*
import kotlin.collections.ArrayList

/**
 * A very simple "cache" which keeps the user signed moves. Everything is kept in memory, hence
 * when a node shutdown the data are lost.
 */
class GameMoveCache {

    companion object {
        val gamesMoves = mutableMapOf<UUID, ArrayList<SignedGameMove>>()

        fun addGameMove(signedGameMove: SignedGameMove) {
            gamesMoves.getOrPut(signedGameMove.gameMove.gameId) { ArrayList() }.add(signedGameMove)
        }

        fun getLatestAndMove(gameId: UUID): SignedGameMove {
            return gamesMoves.getOrPut(gameId) { ArrayList() }.last()
        }
    }

}