package com.lightningchess.webserver

import java.util.*

data class CreateGameResponse(
        val transactionId: String,
        val gameId: UUID
) {

}