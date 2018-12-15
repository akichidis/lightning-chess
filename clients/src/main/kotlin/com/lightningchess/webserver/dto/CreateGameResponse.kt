package com.lightningchess.webserver.dto

import java.util.*

data class CreateGameResponse(
        val transactionId: String,
        val gameId: UUID
) {

}