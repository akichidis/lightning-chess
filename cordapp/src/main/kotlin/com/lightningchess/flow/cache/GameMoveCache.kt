package com.lightningchess.flow.cache

import com.lightningchess.flow.SignedGameMove
import java.util.*
import kotlin.collections.ArrayList

class GameMoveCache {

    companion object {
        val gamesMoves = mutableMapOf<UUID, ArrayList<SignedGameMove>>()

        fun addGameMove(signedGameMove: SignedGameMove) {
            gamesMoves.getOrPut(signedGameMove.gameMove.gameId) { ArrayList() }.add(signedGameMove)
        }

        fun getLatestAndMove(gameId: UUID): SignedGameMove? {
            return gamesMoves.get(gameId)!!.lastOrNull()
        }
    }

}