/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.jmusicbot

import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivoberger.jmusicbot.api.MusicBotService
import com.ivoberger.jmusicbot.api.PORT
import com.ivoberger.jmusicbot.api.discoverHost
import com.ivoberger.jmusicbot.api.process
import com.ivoberger.jmusicbot.di.BaseComponent
import com.ivoberger.jmusicbot.di.BaseModule
import com.ivoberger.jmusicbot.di.DaggerBaseComponent
import com.ivoberger.jmusicbot.di.ServerSession
import com.ivoberger.jmusicbot.di.UserSession
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.exceptions.NotFoundException
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.Auth
import com.ivoberger.jmusicbot.model.Event
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.PlayerStates
import com.ivoberger.jmusicbot.model.QueueEntry
import com.ivoberger.jmusicbot.model.SideEffect
import com.ivoberger.jmusicbot.model.Song
import com.ivoberger.jmusicbot.model.State
import com.ivoberger.jmusicbot.model.User
import com.ivoberger.jmusicbot.model.VersionInfo
import com.tinder.StateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.timer

object JMusicBot {

    internal val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Disconnected)
        state<State.Disconnected> { on<Event.StartDiscovery> { transitionTo(State.Discovering) } }
        state<State.Discovering> {
            on<Event.ServerFound> { transitionTo(State.AuthRequired, SideEffect.StartServerSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.AuthRequired> {
            on<Event.Authorize> { transitionTo(State.Connected, SideEffect.StartUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        state<State.Connected> {
            on<Event.AuthExpired> { transitionTo(State.AuthRequired, SideEffect.EndUserSession) }
            on<Event.Disconnect> { transitionTo(State.Disconnected, SideEffect.EndServerSession) }
        }
        onTransition { transition ->
            val validTransition = transition as? StateMachine.Transition.Valid
            validTransition?.let { trans ->
                Timber.d("State transition from ${trans.fromState} to ${trans.toState} by ${trans.event}")
                when (trans.sideEffect) {
                    is SideEffect.StartServerSession -> {
                        val event = trans.event as Event.ServerFound
                        mServerSession = mBaseComponent.serverSession(event.serverModule)
                        mServiceClient = mServerSession!!.musicBotService()
                    }
                    is SideEffect.StartUserSession -> {
                        val event = trans.event as Event.Authorize
                        mUserSession = mServerSession!!.userSession(event.userModule)
                        mServiceClient = mUserSession!!.musicBotService()
                        connectionListeners.forEach { it.onConnectionRecovered() }
                    }
                    SideEffect.EndUserSession -> {
                        user = null
                        Timber.d("CLIENTS 1: SERVER ${mServerSession?.musicBotService()}, USER ${mUserSession?.musicBotService()}, USED $mServiceClient")
                        mUserSession = null
                        mServiceClient = mServerSession!!.musicBotService()
                        Timber.d("CLIENTS 2: SERVER ${mServerSession?.musicBotService()}, USER ${mUserSession?.musicBotService()}, USED $mServiceClient")
                    }
                    SideEffect.EndServerSession -> {
                        user = null
                        mUserSession = null
                        mServerSession = null
                        if (trans.fromState is State.Discovering) return@onTransition
                        val event = trans.event as Event.Disconnect
                        connectionListeners.forEach { it.onConnectionLost(event.reason) }
                    }
                }
                return@onTransition
            }
            val invalidTransition = transition as? StateMachine.Transition.Invalid
            invalidTransition?.let { Timber.d("Attempted state transition from ${it.fromState} by ${it.event}") }
        }
    }

    val state: State
        get() = stateMachine.state
    val isConnected: Boolean
        get() = state.isConnected

    internal val mBaseComponent: BaseComponent =
        DaggerBaseComponent.builder().baseModule(BaseModule(HttpLoggingInterceptor.Level.BASIC)).build()

    private val mWifiManager: WifiManager = mBaseComponent.wifiManager

    private var mServerSession: ServerSession? = null
    private var mUserSession: UserSession? = null

    var baseUrl: String? = null
        get() = mServerSession?.baseUrl() ?: field
        private set

    private var mServiceClient: MusicBotService? = null

    val connectionListeners: MutableList<ConnectionChangeListener> = mutableListOf()

    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()
    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    var user: User? = null
        get() = mUserSession?.user ?: field
        internal set(value) {
            Timber.d("Setting user to ${value?.name}")
            field = value
            if (value == null) authToken = null
        }

    internal var authToken: Auth.Token? = null
        get() = mUserSession?.authToken ?: field
        set(value) {
            Timber.d("Setting token to $value")
            field = value
            field?.let {
                user?.permissions = it.permissions
            }
        }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun connect(authUser: User, host: String) = withContext(Dispatchers.IO) {
        Timber.d("Quick connect")
        if (!state.hasServer) discoverHost(host)
        if (!state.isConnected) authorize(authUser)
    }

    suspend fun discoverHost(knownHost: String? = null) = withContext(Dispatchers.IO) {
        if (state.isDiscovering || state.running != null) return@withContext
        Timber.d("Discovering host")
        if (!state.isDisconnected) stateMachine.transition(Event.Disconnect())
        stateMachine.transition(Event.StartDiscovery)
        state.running = launch {
            val hostAddress = knownHost ?: mWifiManager.discoverHost()
            hostAddress?.let {
                baseUrl = knownHost ?: "http://$it:$PORT/"
                Timber.d("Found host: $baseUrl")
                stateMachine.transition(Event.ServerFound(baseUrl!!))
                return@launch
            }
            Timber.d("No host found")
            stateMachine.transition(Event.Disconnect())
        }
        state.running?.join()
    }

    suspend fun recoverConnection() = withContext(Dispatchers.IO) {
        Timber.d("Reconnecting")
        while (!state.hasServer) {
            state.running?.join()
            discoverHost()
        }
        authorize()
    }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun authorize(authUser: User?) = withContext(Dispatchers.IO) {
        user = authUser
        authorize()
    }

    @Throws(
        InvalidParametersException::class, NotFoundException::class,
        ServerErrorException::class, IllegalStateException::class
    )
    suspend fun authorize(userName: String? = null, password: String? = null) = withContext(Dispatchers.IO) {
        state.serverCheck()
        Timber.d("Starting authorization")
        if (tokenValid()) return@withContext
        if (userName.isNullOrBlank() && user == null) throw IllegalStateException("No username stored or supplied")
        try {
            register(userName)
            if (!password.isNullOrBlank()) changePassword(password)
            return@withContext
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            if (password.isNullOrBlank() && user?.password.isNullOrBlank()) {
                Timber.d("No passwords found, throwing exception, $password, ${user?.name}")
                throw e
            }
        }
        try {
            login(userName, password)
            if (tokenValid()) return@withContext
        } catch (e: Exception) {
            Timber.w(e)
            Timber.d("Authorization failed")
            throw e
        }
    }

    private suspend fun tokenValid(): Boolean {
        if (authToken == null) {
            Timber.d("Invalid Token: No token stored")
            return false
        }
        try {
            if (authToken!!.isExpired()) {
                Timber.d("Invalid Token: Token expired")
                authToken = null
                return false
            }
            val tmpUser = mServiceClient!!.testToken(authToken!!.toAuthHeader()).process()
            if (tmpUser?.name == user?.name) {
                Timber.d("Valid Token: ${user?.name}")
                stateMachine.transition(Event.Authorize(user!!, authToken!!))
                return true
            }
            Timber.d("Invalid Token: User changed")
        } catch (e: Exception) {
            Timber.d("Invalid Token: Test failed (${e.localizedMessage}")
        }
        authToken = null
        return false
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    private suspend fun register(userName: String? = null) = withContext(Dispatchers.IO) {
        Timber.d("Registering ${userName?.let { User(it) } ?: user}")
        state.serverCheck()
        val credentials = when {
            (!userName.isNullOrBlank()) -> {
                user = User(userName)
                Auth.Register(userName)
            }
            user != null -> Auth.Register(user!!)
            else -> throw IllegalStateException("No username stored or supplied")
        }
        val token = mServiceClient!!.registerUser(credentials).process()!!
        Timber.d("Registered $user")
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user!!, authToken!!))
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    private suspend fun login(userName: String? = null, password: String? = null) = withContext(Dispatchers.IO) {
        Timber.d("Logging in ${userName ?: user?.name}")
        state.serverCheck()
        val credentials = when {
            (!(userName.isNullOrBlank() || password.isNullOrBlank())) -> {
                user = User(userName, password)
                Auth.Basic(userName, password).toAuthHeader()
            }
            user != null -> Auth.Basic(user!!).toAuthHeader()
            else -> throw IllegalStateException("No user stored or supplied")
        }
        Timber.d("Auth: $credentials")
        val token = mServiceClient!!.loginUser(credentials).process()!!
        Timber.d("Logged in ${user?.name}")
        authToken = Auth.Token(token)
        stateMachine.transition(Event.Authorize(user!!, authToken!!))
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        authToken = Auth.Token(mServiceClient!!.changePassword(Auth.PasswordChange((newPassword))).process()!!)
        authToken?.also { user?.password = newPassword }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        stopQueueUpdates()
        stopPlayerUpdates()
        stateMachine.transition(Event.AuthExpired)
    }

    suspend fun reloadPermissions() = withContext(Dispatchers.IO) {
        stateMachine.transition(Event.AuthExpired)
        recoverConnection()
    }

    @Throws(
        InvalidParametersException::class, AuthException::class,
        NotFoundException::class, ServerErrorException::class, IllegalStateException::class
    )
    suspend fun deleteUser() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        Timber.d("Deleting user ${user?.name}")
        authToken ?: throw IllegalStateException("Auth token is null")
        mServiceClient!!.deleteUser().process()
        user = null
        stateMachine.transition(Event.AuthExpired)
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updateQueue(mServiceClient!!.enqueue(song.id, song.provider.id).process())
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updateQueue(mServiceClient!!.dequeue(song.id, song.provider.id).process())
    }

    suspend fun moveEntry(entry: QueueEntry, providerId: String, songId: String, newPosition: Int) =
        withContext(Dispatchers.IO) {
            state.connectionCheck()
            updateQueue(mServiceClient!!.moveEntry(entry, providerId, songId, newPosition).process())
        }

    suspend fun search(providerId: String, query: String): List<Song> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.searchForSong(providerId, query).process() ?: listOf()
    }

    suspend fun suggestions(suggesterId: String): List<Song> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getSuggestions(suggesterId).process() ?: listOf()
    }

    suspend fun deleteSuggestion(suggesterId: String, song: Song) = withContext(Dispatchers.IO) {
        state.connectionCheck()
        mServiceClient!!.deleteSuggestion(suggesterId, song.id, song.provider.id).process()
    }

    suspend fun pause() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.pause().process())
    }

    suspend fun play() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.play().process())
    }

    suspend fun skip() = withContext(Dispatchers.IO) {
        state.connectionCheck()
        updatePlayer(mServiceClient!!.skip().process())
    }

    suspend fun getProvider(): List<MusicBotPlugin> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getProvider().process() ?: listOf()
    }

    suspend fun getSuggesters(): List<MusicBotPlugin> = withContext(Dispatchers.IO) {
        state.connectionCheck()
        return@withContext mServiceClient!!.getSuggesters().process() ?: listOf()
    }

    suspend fun getVersionInfo(): VersionInfo = withContext(Dispatchers.IO) {
        state.serverCheck()
        return@withContext mServiceClient!!.getVersionInfo().process()!!
    }

    fun getQueue(period: Long = 500): LiveData<List<QueueEntry>> {
        startQueueUpdates(period)
        return mQueue
    }

    fun startQueueUpdates(period: Long = 500) {
        if (mQueueUpdateTimer == null) mQueueUpdateTimer = timer(period = period) { updateQueue() }
    }

    fun stopQueueUpdates() {
        mQueueUpdateTimer?.cancel()
        mQueueUpdateTimer = null
    }

    fun getPlayerState(period: Long = 500): LiveData<PlayerState> {
        startPlayerUpdates(period)
        return mPlayerState
    }

    fun startPlayerUpdates(period: Long = 500) {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer = timer(period = period) { updatePlayer() }
    }

    fun stopPlayerUpdates() {
        mPlayerUpdateTimer?.cancel()
        mPlayerUpdateTimer = null
    }

    private fun updateQueue(newQueue: List<QueueEntry>? = null) = runBlocking(Dispatchers.IO) {
        if (!mQueue.hasActiveObservers()) return@runBlocking
        if (newQueue != null) Timber.d("Manual Queue Update")
        try {
            state.connectionCheck()
            val queue = newQueue ?: mServiceClient!!.getQueue().process() ?: listOf()
            withContext(Dispatchers.Main) { mQueue.value = queue }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = runBlocking(Dispatchers.IO) {
        if (!mPlayerState.hasActiveObservers()) return@runBlocking
        if (playerState != null) Timber.d("Manual Player Update")
        try {
            state.connectionCheck()
            val state = playerState ?: mServiceClient!!.getPlayerState().process()
            ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) { mPlayerState.value = state }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
