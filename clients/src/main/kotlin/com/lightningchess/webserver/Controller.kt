package com.lightningchess.webserver

import com.lightningchess.flow.AbandonGameFlow.Abandon
import com.lightningchess.flow.CreateGameFlow.Initiator
import com.lightningchess.flow.GameMove
import com.lightningchess.flow.GetGameMovesFlow.GetMoves
import com.lightningchess.flow.SignAndSendMoveFlow.Sign
import com.lightningchess.flow.SignedGameMove
import com.lightningchess.state.GameState
import com.lightningchess.webserver.dto.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * Define your API endpoints here.
 */
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

@RestController
@CrossOrigin
@RequestMapping("/api")
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy
    private val myLegalName: CordaX500Name = proxy.nodeInfo().legalIdentities.first().name

    @GetMapping(value = "/me", produces = arrayOf("application/json"))
    fun me() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping(value = "/peers", produces = arrayOf("application/json"))
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Creates a new game by providing the opponent's X500 name. If the opponent (node) is up and running and sign the
     * contract, then the game will be considered as started.
     */
    @PostMapping(value = "/games", produces = arrayOf("application/json"))
    fun createGame(@RequestBody gameRequest: NewGameRequest): ResponseEntity<CreateGameResponse> {
        print(gameRequest)

        val opponent = proxy.wellKnownPartyFromX500Name(gameRequest.opponentX500Name) ?:
        return ResponseEntity.badRequest().build()

        try {
            val signedTx = proxy.startTrackedFlow(::Initiator, gameRequest.userNickname, opponent).returnValue.getOrThrow()

            return ResponseEntity.created(URI(""))
                    .body(CreateGameResponse(signedTx.id.toString(), retrieveGameId(signedTx)))
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Retrieves the created game states and returns them in RECORDED_TIME Desc order. By default, only the non finished
     * states are returned. If the [finishedOnly] is passed as true, then only the finished games will be returned instead.
     */
    @GetMapping(value = "/games", produces = arrayOf("application/json"))
    fun retrieveGames(@RequestParam("size") pageSize: Int, @RequestParam("finishedOnly") finishedOnly: Boolean = false): ResponseEntity<RetrieveGamesResponse> {

        val sortByRecordedTime = SortAttribute.Standard(attribute = Sort.VaultStateAttribute.RECORDED_TIME)
        val criteria = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val pageSpecification = PageSpecification(DEFAULT_PAGE_NUM, pageSize)

        val gameStates = proxy.vaultQueryBy<GameState>(
                paging = pageSpecification,
                criteria = criteria,
                sorting = Sort(setOf(Sort.SortColumn(sortByRecordedTime, Sort.Direction.DESC))))
                .states
                .filter { if (finishedOnly) it.state.data.winner != GameState.Winner.NOT_FINISHED
                            else it.state.data.winner == GameState.Winner.NOT_FINISHED }

        return ResponseEntity.ok(RetrieveGamesResponse(gameStates))
    }

    @PostMapping(value = "/games/{id}/moves", produces = arrayOf("application/json"))
    fun signGameMove(@PathVariable("id") gameId: UUID, @RequestBody signGameMoveRequest: SignGameMoveRequest): ResponseEntity<SignGameMoveResponse> {
        val opponent = proxy.wellKnownPartyFromX500Name(signGameMoveRequest.opponentX500Name) ?:
        return ResponseEntity.badRequest().build()

        val gameMove = GameMove(signGameMoveRequest.move, signGameMoveRequest.gameId,
                signGameMoveRequest.index, signGameMoveRequest.fen, signGameMoveRequest.previousSignature)

        // Sign the move & Initiate the flow and send it to the opponent
        val signedGameMove = proxy.startTrackedFlow(::Sign, gameMove, opponent).returnValue.getOrThrow()

        return ResponseEntity.created(URI("")).body(SignGameMoveResponse(signedGameMove.signature!!))
    }

    @GetMapping(value = "/games/{id}/moves", produces = arrayOf("application/json"))
    fun getLastGameMove(@PathVariable("id") gameId: UUID) :ResponseEntity<List<SignedGameMove>> {
        try {
            val signedGameMoves = proxy.startFlow(::GetMoves, gameId).returnValue.get()

            return ResponseEntity.ok(signedGameMoves)
        } catch (e: ExecutionException) {
            return ResponseEntity.ok().build()
        }
    }

    @PostMapping(value = "/games/{id}/abandon", produces = arrayOf("application/json"))
    fun abandonGame(@PathVariable("id") gameId: UUID) :ResponseEntity<SecureHash> {
        try {
            val signedTx = proxy.startFlow(::Abandon, gameId).returnValue.get()

            return ResponseEntity.ok(signedTx.tx.id)
        } catch (e: ExecutionException) {
            return ResponseEntity.badRequest().build()
        }
    }

    private fun retrieveGameId(signedTx: SignedTransaction): UUID {
        val gameState = signedTx.coreTransaction.outputsOfType<GameState>().first()

        return gameState.gameId
    }
}