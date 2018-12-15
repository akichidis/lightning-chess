package com.lightningchess.flow

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.serialize
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * Signs a game move based on a sign request and sends the signed move
 * to the opponent party.
 */
object SignAndSendMoveFlow {

    @StartableByRPC
    @InitiatingFlow
    class Sign(val gameMove: GameMove, val opponent: Party) : FlowLogic<SignedGameMove>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object SIGNING_MOVE : ProgressTracker.Step("Sign the move.")
            object SENDING_MOVE_TO_OPPONENT : ProgressTracker.Step("Sending the signed move to the opponent.")
            object STORE_MOVE : ProgressTracker.Step("Storing the signed move to the database.")

            fun tracker() = ProgressTracker(
                    SIGNING_MOVE,
                    SENDING_MOVE_TO_OPPONENT,
                    STORE_MOVE
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedGameMove {
            val me = serviceHub.myInfo.legalIdentities.single()

            // Stage 1. Sign the move
            progressTracker.currentStep = SIGNING_MOVE

            val moveSignature = serviceHub.keyManagementService.sign(gameMove.serialize().bytes, me.owningKey)
            val signedGameMove = SignedGameMove(gameMove, moveSignature)

            // Stage 2. Send to opponent
            progressTracker.currentStep = SENDING_MOVE_TO_OPPONENT

            val session = initiateFlow(opponent)

            val successfullySent = session.sendAndReceive<Boolean>(signedGameMove)

            // Stage 3. Store the move on the database
            progressTracker.currentStep = STORE_MOVE

            return signedGameMove
        }
    }

    @InitiatedBy(SignAndSendMoveFlow.Sign::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<Boolean>() {
        object VALIDATING_MOVE : ProgressTracker.Step("Validating the received signed move.")

        companion object {
            fun tracker() = ProgressTracker(
                    VALIDATING_MOVE
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): Boolean {
            // Step 1. Verifying signature and validating move
            progressTracker.currentStep = VALIDATING_MOVE

            val signedGameMove = otherPartyFlow.receive<SignedGameMove>().unwrap { it }

            val gameMove = signedGameMove.gameMove
            val signature = signedGameMove.signature

            signature.verify(gameMove.serialize().bytes)

            otherPartyFlow.send(true)

            return true
        }
    }
}