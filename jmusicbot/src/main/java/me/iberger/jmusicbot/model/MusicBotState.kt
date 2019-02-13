package me.iberger.jmusicbot.model

import kotlinx.coroutines.Job

/**
 * Possible states for the music bot client to be in
 * DISCONNECTED: Client has no server connection
 * CONNECTING: client is trying to find a server
 * NEEDS_AUTH: Client has found a server but needs login/new auth token
 * CONNECTED: Server connection and authentication is successfully set up, client is ready to send command
 */
enum class MusicBotState {
    NEEDS_AUTH, CONNECTED, DISCONNECTED, CONNECTING;

    var job: Job? = null

    fun hasServer() = this != DISCONNECTED && this != CONNECTING
    fun isConnected() = hasServer() && this != NEEDS_AUTH

    fun serverCheck() {
        check(hasServer()) { "Client has no server" }
    }

    @Throws(IllegalStateException::class)
    fun connectionCheck() {
        serverCheck()
        check(isConnected()) { "Client needs authorization" }
    }
}
