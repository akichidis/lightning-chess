package com.lightningchess.webserver.dto

import net.corda.core.crypto.DigitalSignature

data class SignGameMoveResponse(
        val signature: DigitalSignature.WithKey
) {

}