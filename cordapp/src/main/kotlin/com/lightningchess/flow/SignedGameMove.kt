package com.lightningchess.flow

import net.corda.core.crypto.DigitalSignature
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class SignedGameMove(val gameMove: GameMove,
                          val signature: DigitalSignature.WithKey) {}