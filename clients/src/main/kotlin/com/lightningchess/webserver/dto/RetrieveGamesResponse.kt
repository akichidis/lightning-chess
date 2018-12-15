package com.lightningchess.webserver.dto

import com.lightningchess.state.GameState
import net.corda.core.contracts.StateAndRef

data class RetrieveGamesResponse(
        val gameStates: List<StateAndRef<GameState>>
) {

}