package com.ivoberger.jmusicbot.model

import com.ivoberger.jmusicbot.di.ServerModule
import com.ivoberger.jmusicbot.di.UserModule

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
    val hasServer
        get() = this == AuthRequired
    val isConnected: Boolean
        get() = this == Connected

    @Throws(IllegalStateException::class)
    fun connectionCheck() {
        check(isConnected) { "Client not connected" }
    }

    @Throws(IllegalStateException::class)
    fun serverCheck() {
        check(hasServer) { "Client has no server" }
    }
}

sealed class Event {
    override fun toString(): String = this::class.java.simpleName

    object OnStartDiscovery : Event()
    class OnServerFound(baseUrl: String) : Event() {
        internal val serverModule: ServerModule = ServerModule(baseUrl)
    }

    class OnAuthorize(
        user: User,
        authToken: Auth.Token
    ) : Event() {
        internal val userModule: UserModule

        init {
            user.permissions = authToken.permissions
            userModule = UserModule(user, authToken)
        }
    }

    object OnAuthExpired : Event()
    class OnDisconnect(val reason: Exception? = null) : Event()
}

sealed class SideEffect {
    override fun toString(): String = this::class.java.simpleName

    object StartServerSession : SideEffect()
    object StartUserSession : SideEffect()
    object EndUserSession : SideEffect()
    object EndServerSession : SideEffect()
}
