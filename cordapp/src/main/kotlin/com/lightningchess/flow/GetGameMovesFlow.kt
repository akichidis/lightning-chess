package com.lightningchess.flow

import co.paralleluniverse.fibers.Suspendable
import com.lightningchess.flow.cache.GameMoveCache
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

object GetGameMovesFlow {

    /**
     * Returns the latest game moves for the provided [gameId]. The order should be from the last played to the
     * earliest played.
     */
    @StartableByRPC
    @InitiatingFlow
    class GetMoves(val gameId: UUID) : FlowLogic<List<SignedGameMove>>() {

        @Suspendable
        override fun call(): List<SignedGameMove> {
            return try {
                listOf(GameMoveCache.getLatestGameMove(gameId))
            } catch (ex: NoSuchElementException) {
                listOf()
            }
        }
    }
}