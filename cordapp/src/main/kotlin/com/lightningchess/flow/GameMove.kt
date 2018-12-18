package com.lightningchess.flow

import net.corda.core.crypto.DigitalSignature
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
data class GameMove(val move: String,
                    val gameId: UUID,
                    val index: Int,
                    val fen: String,
                    val previousSignature: DigitalSignature.WithKey?) {
}