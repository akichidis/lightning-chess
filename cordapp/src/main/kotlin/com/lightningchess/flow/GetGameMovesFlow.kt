package com.lightningchess.flow

import co.paralleluniverse.fibers.Suspendable
import com.lightningchess.flow.cache.GameMoveCache
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.util.*

object GetGameMovesFlow {

    @StartableByRPC
    @InitiatingFlow
    class GetMoves(val gameId: UUID) : FlowLogic<SignedGameMove>() {

        @Suspendable
        @Throws(NoGameMoveFoundException::class)
        override fun call(): SignedGameMove {
            try {
                return GameMoveCache.getLatestAndMove(gameId)
            } catch (ex: NoSuchElementException) {
                throw NoGameMoveFoundException()
            }
        }
    }

    class NoGameMoveFoundException: FlowException("No game move found") {
    }
}