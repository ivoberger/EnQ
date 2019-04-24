package com.ivoberger.jmusicbot.model

import kotlinx.coroutines.Job

/**
 * Possible states for the musicBotService bot client to be in
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

sealed class State {
    object Disconnected : State()
    object AuthRequired : State()
    object Connecting : State()
    object Connected : State()
}

sealed class Event {
    object OnServerFound : Event()
    object OnGotCredentials : Event()
    object OnAuthorize : Event()
    object OnAuthExpired : Event()
    object OnDisconnect : Event()
}

sealed class SideEffect {
    object LogMelted : SideEffect()
    object LogFrozen : SideEffect()
    object LogVaporized : SideEffect()
    object LogCondensed : SideEffect()
}
