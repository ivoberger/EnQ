package me.iberger.jmusicbot.model

import kotlinx.coroutines.Job

/**
 * Possible states for the music bot client to be in
 * NEEDS_INIT: Initial state, no user and no url set
 * DISCONNECTED: Client has no server connection
 * CONNECTING: client is trying to find a server
 * NEEDS_AUTH: Client has found a server but needs login/new auth token
 * CONNECTED: Server connection and authentication is successfully set up, client is ready to send command
 */
enum class MusicBotState {
    NEEDS_INIT, NEEDS_AUTH, CONNECTED, DISCONNECTED, CONNECTING;

    var job: Job? = null

    fun isInitialized() = this != NEEDS_INIT
    fun isConnected() = this != NEEDS_INIT && this != DISCONNECTED && this != CONNECTING

    @Throws(IllegalStateException::class)
    fun connectionCheck() {
        check(isInitialized()) { "Client not initialized" }
        check(isConnected()) { "Client has no server" }
    }
}
