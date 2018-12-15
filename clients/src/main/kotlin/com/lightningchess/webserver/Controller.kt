package com.lightningchess.webserver

import com.lightningchess.flow.CreateGameFlow.Initiator
import com.lightningchess.state.GameState
import com.lightningchess.webserver.dto.CreateGameResponse
import com.lightningchess.webserver.dto.NewGameRequest
import com.lightningchess.webserver.dto.RetrieveGamesResponse
import net.corda.core.identity.CordaX500Name
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
    @PostMapping(value = "/create-game", produces = arrayOf("application/json"))
    fun createGame(@RequestBody gameRequest: NewGameRequest): ResponseEntity<CreateGameResponse> {
        print(gameRequest)

        val opponent = proxy.wellKnownPartyFromX500Name(gameRequest.opponentX500Name) ?:
        return ResponseEntity.badRequest().build()

        return try {
            val signedTx = proxy.startTrackedFlow(::Initiator, gameRequest.userNickname, opponent).returnValue.getOrThrow()

            return ResponseEntity.created(URI(""))
                    .body(CreateGameResponse(signedTx.id.toString(), retrieveGameId(signedTx)))
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            return ResponseEntity.badRequest().build()
        }
    }

    /**
     * Retrieves the created game states that haven't been spent (finished) yet. It returns them in RECORDED_TIME
     * Desc order.
     */
    @GetMapping(value = "/games", produces = arrayOf("application/json"))
    fun retrieveGames(@RequestParam("size") pageSize: Int): ResponseEntity<RetrieveGamesResponse> {

        val sortByRecordedTime = SortAttribute.Standard(attribute = Sort.VaultStateAttribute.RECORDED_TIME)
        val criteria = QueryCriteria.LinearStateQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        val pageSpecification = PageSpecification(DEFAULT_PAGE_NUM, pageSize)

        val gameStates = proxy.vaultQueryBy<GameState>(
                paging = pageSpecification,
                criteria = criteria,
                sorting = Sort(setOf(Sort.SortColumn(sortByRecordedTime, Sort.Direction.DESC)))).states

        return ResponseEntity.ok(RetrieveGamesResponse(gameStates))
    }

    private fun retrieveGameId(signedTx: SignedTransaction): UUID {
        val gameState = signedTx.coreTransaction.outputsOfType<GameState>().first()

        return gameState.gameId
    }
}