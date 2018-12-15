package com.lightningchess.webserver

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.identity.CordaX500Name

data class NewGameRequest @JsonCreator constructor(
        val opponentX500Name: CordaX500Name,
        val userNickname: String) {

}
