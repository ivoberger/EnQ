package com.ivoberger.jmusicbot.model

import com.ivoberger.jmusicbot.api.PORT
import com.ivoberger.jmusicbot.di.ServerModule
import com.ivoberger.jmusicbot.di.UserModule

/**
 * Possible states for the musicBotService bot client to be in
 * DISCONNECTED: Client has no server connection
 * CONNECTING: client is trying to find a server
 * NEEDS_AUTH: Client has found a server but needs login/new auth token
 * CONNECTED: Server connection and authentication is successfully set up, client is ready to send command
 */
enum class MusicBotState {
    NEEDS_AUTH, CONNECTED, DISCONNECTED, CONNECTING;

    fun hasServer() = this != DISCONNECTED && this != CONNECTING
}

/**
 * Possible states for the musicBotService bot client to be in
 */
sealed class State {
    override fun toString(): String = this::class.java.simpleName

    /** Client has no server connection */
    object Disconnected : State()

    /** Client is in the trying to find a host */
    object Discovering : State()

    /** Client has found a server but needs login/new auth token */
    object AuthRequired : State()

    /** Server connection and authentication is successfully established */
    object Connected : State()

    val isDisconnected
        get() = this == Disconnected
    val isDiscovering
        get() = this == Discovering
    val isAuthRequired
        get() = this == AuthRequired
    val hasServer
        get() = isAuthRequired
    val isConnected: Boolean
        get() = this == Connected

    @Throws(IllegalStateException::class)
    fun connectionCheck() {
        check(isConnected) { "Client not connected" }
    }

    @Throws(IllegalStateException::class)
    fun serverCheck() {
        check(isAuthRequired) { "Client has no server" }
    }
}

internal sealed class Event {
    override fun toString(): String = this::class.java.simpleName

    object OnStartDiscovery : Event()
    class OnServerFound(
        baseUrl: String,
        port: Int = PORT,
        val serverModule: ServerModule = ServerModule(baseUrl, port)
    ) : Event()

    class OnAuthorize(
        user: User,
        authToken: Auth.Token
    ) : Event() {
        val userModule: UserModule

        init {
            user.permissions = authToken.permissions
            userModule = UserModule(user, authToken)
        }
    }

    object OnAuthExpired : Event()
    object OnDisconnect : Event()
}

internal sealed class SideEffect {
    override fun toString(): String = this::class.java.simpleName

    object StartServerSession : SideEffect()
    object StartUserSession : SideEffect()
    object EndUserSession : SideEffect()
    object EndServerSession : SideEffect()
}
