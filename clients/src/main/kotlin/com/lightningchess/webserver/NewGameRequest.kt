package com.lightningchess.webserver

import com.fasterxml.jackson.annotation.JsonCreator

data class NewGameRequest @JsonCreator constructor(
    val opponentX500Name: String,
    val userNickname: String) {

}
