package com.lightningchess.webserver.dto

import net.corda.core.crypto.DigitalSignature
import net.corda.core.identity.CordaX500Name
import java.util.*

data class SignGameMoveRequest(
        val gameId: UUID,
        val opponentX500Name: CordaX500Name,
        val index: Int,
        val fen: String,
        val move: String,
        val previousSignature: DigitalSignature.WithKey?
) {
}