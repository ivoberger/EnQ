package com.ivoberger.jmusicbot.model

import com.ivoberger.jmusicbot.api.PORT
import com.ivoberger.jmusicbot.di.ServerModule
import com.ivoberger.jmusicbot.di.UserModule
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

internal sealed class State {
    object Disconnected : State()
    object Discovering : State()
    object AuthRequired : State()
    object Connecting : State()
    object Connected : State()

    val isDisconnected
        get() = this == Disconnected
    val isDiscovering
        get() = this == Discovering
    val isAuthRequired
        get() = this == AuthRequired
    val isConnecting
        get() = this == Connecting
    val isConnected: Boolean
        get() = this == Connected

    fun connectionCheck() {
        check(isConnected) { "Client not connected" }
    }

    fun serverCheck() {
        check(isAuthRequired) { "Client has no server" }
    }
}

internal sealed class Event {
    object OnStartDiscovery : Event()
    class OnServerFound(
        baseUrl: String,
        port: Int = PORT,
        val serverModule: ServerModule = ServerModule(baseUrl, port)
    ) : Event()

    class OnGotCredentials(
        user: User,
        authToken: Auth.Token
    ) : Event() {
        val userModule: UserModule

        init {
            user.permissions = authToken.permissions
            userModule = UserModule(user, authToken)
        }
    }

    object OnAuthorize : Event()
    object OnAuthExpired : Event()
    object OnDisconnect : Event()
}

internal sealed class SideEffect {
    object StartServerSession : SideEffect()
    object StartUserSession : SideEffect()
    object EndUserSession : SideEffect()
    object EndServerSession : SideEffect()
}
