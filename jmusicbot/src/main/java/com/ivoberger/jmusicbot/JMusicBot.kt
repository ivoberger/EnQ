package com.ivoberger.jmusicbot

import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivoberger.jmusicbot.api.MusicBotService
import com.ivoberger.jmusicbot.api.discoverHost
import com.ivoberger.jmusicbot.api.process
import com.ivoberger.jmusicbot.di.BaseComponent
import com.ivoberger.jmusicbot.di.BaseModule
import com.ivoberger.jmusicbot.di.DaggerBaseComponent
import com.ivoberger.jmusicbot.exceptions.*
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.*
import com.tinder.StateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.*
import kotlin.concurrent.timer


object JMusicBot {

    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Disconnected)
        state<State.Disconnected> {
            on<Event.OnServerFound> { transitionTo(State.AuthRequired) }
        }
        state<State.AuthRequired> {
            on<Event.OnGotCredentials> { transitionTo(State.Connecting) }
            on<Event.OnDisconnect> { transitionTo(State.Disconnected) }
        }
        state<State.Connecting> {
            on<Event.OnAuthorize> { transitionTo(State.Connected) }
            on<Event.OnDisconnect> { transitionTo(State.Disconnected) }
        }
        state<State.Connected> {
            on<Event.OnAuthExpired> { transitionTo(State.AuthRequired) }
            on<Event.OnDisconnect> { transitionTo(State.Disconnected) }
        }
        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid
            val invalidTransition = it as? StateMachine.Transition.Invalid
            validTransition?.let { trans ->
                Timber.d("State transition from ${trans.fromState} to ${trans.toState} by ${trans.event}")
            }
            invalidTransition?.let { trans ->
                Timber.d("Attempted state transition from ${trans.fromState} by ${trans.event}")
            }
        }
    }

    val newState: State
        get() = stateMachine.state

    private val isDisconnected
        get() = newState == State.Disconnected
    private val isAuthRequired
        get() = newState == State.AuthRequired
    private val isConnecting
        get() = newState == State.Connecting

    var isConnected: Boolean = false
        get() = newState == State.Connected
        set(value) {
            field = value
            if (value) connectionChangeListeners.forEach { it.onConnectionRecovered() }
            if (!value) {
                connectionChangeListeners.forEach { it.onConnectionLost() }
                state = MusicBotState.DISCONNECTED
            }
        }

    var state: MusicBotState = MusicBotState.DISCONNECTED
        set(newState) {
            Timber.d("State changed from $field to $newState")
            if (newState == MusicBotState.NEEDS_AUTH || newState == MusicBotState.DISCONNECTED) {
                stopQueueUpdates()
                stopPlayerUpdates()
            }
            if (newState == MusicBotState.CONNECTED) isConnected = true
            field = newState
        }

    internal val mBaseComponent: BaseComponent =
        DaggerBaseComponent.builder().baseModule(BaseModule(HttpLoggingInterceptor.Level.BODY)).build()

    private val mWifiManager: WifiManager = mBaseComponent.wifiManager

    private var baseUrl: String? = null
        set(value) {
            field = value
            value?.let { mRetrofit = mRetrofit.newBuilder().baseUrl(it).build() }
        }

    private lateinit var mRetrofit: Retrofit
    private lateinit var mServiceClient: MusicBotService

    val connectionChangeListeners: MutableList<ConnectionChangeListener> = mutableListOf()


    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()
    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    var user: User?
        get() = BotPreferences.user
        set(value) {
            Timber.d("Setting user to ${value?.name}")
            BotPreferences.user = value
            if (value == null) authToken = null
        }

    var authToken: Auth.Token? = BotPreferences.authToken
        get() = BotPreferences.authToken
        set(value) {
            Timber.d("Setting Token to $value")
            BotPreferences.authToken = value
            if (value == null) state = MusicBotState.NEEDS_AUTH
            field = value
            field?.let {
                state = MusicBotState.CONNECTED
                user?.permissions = it.permissions
                // TODO: create user module with token
            }
        }

    fun discoverHost() {
        Timber.d("Discovering host")
        state = MusicBotState.CONNECTING
        state.job = GlobalScope.launch {
            baseUrl = mWifiManager.discoverHost()
            state = if (baseUrl != null) {
                Timber.d("Found host: $baseUrl")
                MusicBotState.NEEDS_AUTH
            } else {
                Timber.d("No host found")
                MusicBotState.DISCONNECTED
            }
        }

    }

    suspend fun recoverConnection() {
        Timber.d("Reconnecting")
        while (!state.hasServer()) {
            discoverHost()
            state.job?.join()
        }
        authorize()
    }

    @Throws(
        InvalidParametersException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun authorize(userName: String? = null, password: String? = null) {
        check(newState == State.Connected)
        Timber.d("Starting authorization")
        if (tokenValid()) return
        if (userName == null && user == null) throw IllegalStateException("No username stored or supplied")
        try {
            register(userName)
            if (!password.isNullOrBlank()) changePassword(password)
            state = MusicBotState.CONNECTED
            return
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            if (password.isNullOrBlank() && user?.password == null) {
                Timber.d("No passwords found, throwing exception, $password, $user")
                throw e
            }
        }
        try {
            login(userName, password)
            if (tokenValid()) return
        } catch (e: Exception) {
            Timber.w(e)
            Timber.d("Authorization failed")
            throw e
        }
        state = MusicBotState.CONNECTED
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
            val tmpUser = mServiceClient.testToken(authToken!!.toAuthHeader()).process()
            if (tmpUser?.name == user?.name) {
                Timber.d("Valid Token: $user")
                // TODO: create user module with token
                state = MusicBotState.CONNECTED
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
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun register(userName: String? = null) {
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
        val token = mServiceClient.registerUser(credentials).process()!!
        Timber.d("Registered $user")
        authToken = Auth.Token(token)
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun login(userName: String? = null, password: String? = null) {
        Timber.d("Logging in $user")
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
        val token = mServiceClient.loginUser(credentials).process()!!
        Timber.d("Logged in $user")
        authToken = Auth.Token(token)
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) {
        state.connectionCheck()
        authToken = Auth.Token(mServiceClient.changePassword(Auth.PasswordChange((newPassword))).process()!!)
        authToken?.also { user?.password = newPassword }
    }

    suspend fun reloadPermissions() {
        authToken = null
        authorize()
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun deleteUser() {
        state.connectionCheck()
        Timber.d("Deleting user ${user?.name}")
        authToken ?: throw IllegalStateException("Auth token is null")
        mServiceClient.deleteUser().process()
        user = null
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) {
        state.connectionCheck()
        updateQueue(mServiceClient.enqueue(song.id, song.provider.id).process())
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song) {
        state.connectionCheck()
        updateQueue(mServiceClient.dequeue(song.id, song.provider.id).process())
    }

    suspend fun moveEntry(entry: QueueEntry, providerId: String, songId: String, newPosition: Int) {
        state.connectionCheck()
        updateQueue(mServiceClient.moveEntry(entry, providerId, songId, newPosition).process())
    }

    suspend fun search(providerId: String, query: String): List<Song> {
        state.connectionCheck()
        return mServiceClient.searchForSong(providerId, query).process() ?: listOf()
    }

    suspend fun suggestions(suggesterId: String): List<Song> {
        state.connectionCheck()
        return mServiceClient.getSuggestions(suggesterId).process() ?: listOf()
    }

    suspend fun deleteSuggestion(suggesterId: String, song: Song) {
        state.connectionCheck()
        mServiceClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process()
    }

    suspend fun pause() {
        state.connectionCheck()
        updatePlayer(mServiceClient.pause().process())
    }

    suspend fun play() {
        state.connectionCheck()
        updatePlayer(mServiceClient.play().process())
    }

    suspend fun skip() {
        state.connectionCheck()
        updatePlayer(mServiceClient.skip().process())
    }

    suspend fun getProvider(): List<MusicBotPlugin> {
        state.connectionCheck()
        return mServiceClient.getProvider().process() ?: listOf()
    }

    suspend fun getSuggesters(): List<MusicBotPlugin> {
        state.connectionCheck()
        return mServiceClient.getSuggesters().process() ?: listOf()
    }

    fun getQueue(period: Long = 500): LiveData<List<QueueEntry>> {
        if (mQueueUpdateTimer == null) mQueueUpdateTimer = timer(period = period) { updateQueue() }
        return mQueue
    }

    fun startQueueUpdates() {
        if (mQueue.hasObservers())
            mQueueUpdateTimer = timer(period = 500) { updateQueue() }
    }

    fun stopQueueUpdates() {
        mQueueUpdateTimer?.cancel()
        mQueueUpdateTimer = null
    }

    fun getPlayerState(period: Long = 500): LiveData<PlayerState> {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer = timer(period = period) { updatePlayer() }
        return mPlayerState
    }

    fun startPlayerUpdates() {
        if (mPlayerState.hasObservers()) mPlayerUpdateTimer = timer(period = 500) { updatePlayer() }
    }

    fun stopPlayerUpdates() {
        mPlayerUpdateTimer?.cancel()
        mPlayerUpdateTimer = null
    }

    private fun updateQueue(newQueue: List<QueueEntry>? = null) = GlobalScope.launch {
        if (newQueue != null) Timber.d("Manual Queue Update")
        try {
            state.connectionCheck()
            val queue = newQueue ?: mServiceClient.getQueue().process() ?: listOf()
            withContext(Dispatchers.Main) { mQueue.value = queue }
        } catch (e: Exception) {
            Timber.w(e)
            // TODO: propagate error
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = GlobalScope.launch {
        if (playerState != null) Timber.d("Manual Player Update")
        try {
            state.connectionCheck()
            val state = playerState ?: mServiceClient.getPlayerState().process() ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) {
                mPlayerState.value = state
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
