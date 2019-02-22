package com.ivoberger.jmusicbot

import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivoberger.jmusicbot.api.MusicBotAPI
import com.ivoberger.jmusicbot.api.discoverHost
import com.ivoberger.jmusicbot.api.process
import com.ivoberger.jmusicbot.api.withToken
import com.ivoberger.jmusicbot.exceptions.*
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import splitties.systemservices.wifiManager
import timber.log.Timber
import java.util.*
import kotlin.concurrent.timer


object JMusicBot {
    var state: MusicBotState = MusicBotState.DISCONNECTED
        set(newState) {
            Timber.d("State changed from $field to $newState")
            if (newState == MusicBotState.NEEDS_AUTH || newState == MusicBotState.DISCONNECTED) {
                stopQueueUpdates()
                stopPlayerUpdates()
                mOkHttpClient = getDefaultOkHttpClient()
                mRetrofit = getDefaultRetrofitClient(mOkHttpClient, baseUrl)
            }
            if (newState == MusicBotState.CONNECTED) isConnected = true
            field = newState
        }
    private val mWifiManager: WifiManager? by lazy { wifiManager }
    internal val mMoshi: Moshi by lazy { Moshi.Builder().build() }

    private var baseUrl: String? = null
        set(value) {
            field = value
            value?.let { mRetrofit = mRetrofit.newBuilder().baseUrl(it).build() }
        }

    private var mOkHttpClient: OkHttpClient = getDefaultOkHttpClient()
        set(value) {
            field = value
            // rebuild retrofit with new okHttpClient
            mRetrofit = mRetrofit.newBuilder().client(value).build()
        }

    private var mRetrofit: Retrofit = getDefaultRetrofitClient(mOkHttpClient, baseUrl)
        set(value) {
            field = value
            // rebuild API client with new retrofit
            mApiClient = value.create(MusicBotAPI::class.java)
        }

    private var mApiClient: MusicBotAPI = mRetrofit.create(MusicBotAPI::class.java)

    val connectionChangeListeners: MutableList<ConnectionChangeListener> = mutableListOf()
    var isConnected: Boolean = false
        get() = state.isConnected()
        set(value) {
            field = value
            if (value) connectionChangeListeners.forEach { it.onConnectionRecovered() }
            if (!value) {
                connectionChangeListeners.forEach { it.onConnectionLost() }
                state = MusicBotState.DISCONNECTED
            }
        }

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

    var authToken: AuthTypes.Token? = BotPreferences.authToken
        get() = BotPreferences.authToken
        set(value) {
            Timber.d("Setting Token to $value")
            BotPreferences.authToken = value
            if (value == null) state = MusicBotState.NEEDS_AUTH
            field = value
            field?.let {
                state = MusicBotState.CONNECTED
                user?.permissions = it.permissions
                mOkHttpClient = mOkHttpClient.withToken(it)
            }
        }

    fun discoverHost() = GlobalScope.launch {
        Timber.d("Discovering host")
        state = MusicBotState.CONNECTING
        state.job = launch {
            baseUrl = mWifiManager?.discoverHost()
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
        discoverHost()
        authorize()
    }

    @Throws(
        InvalidParametersException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun authorize(userName: String? = null, password: String? = null) {
        state.serverCheck()
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
            Timber.d("Invalid Token: No jwt stored")
            return false
        }
        try {
            if (authToken!!.isExpired()) {
                Timber.d("Invalid Token: Token expired")
                authToken = null
                return false
            }
            val tmpUser = mApiClient.testToken(authToken!!.toAuthHeader()).process()
            if (tmpUser?.name == user?.name) {
                Timber.d("Valid Token: $user")
                mOkHttpClient = mOkHttpClient.withToken(authToken!!)
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
                AuthTypes.Register(userName)
            }
            user != null -> AuthTypes.Register(user!!)
            else -> throw IllegalStateException("No username stored or supplied")
        }
        val token = mApiClient.registerUser(credentials).process()!!
        Timber.d("Registered $user")
        authToken = AuthTypes.Token(token)
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
                AuthTypes.Basic(userName, password).toAuthHeader()
            }
            user != null -> AuthTypes.Basic(user!!).toAuthHeader()
            else -> throw IllegalStateException("No user stored or supplied")
        }
        Timber.d("AuthTypes: $credentials")
        val token = mApiClient.loginUser(credentials).process()!!
        Timber.d("Logged in $user")
        authToken = AuthTypes.Token(token)
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) {
        state.connectionCheck()
        authToken = AuthTypes.Token(mApiClient.changePassword(AuthTypes.PasswordChange((newPassword))).process()!!)
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
        authToken ?: throw IllegalStateException("Auth jwt is null")
        mApiClient.deleteUser().process()
        user = null
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) {
        state.connectionCheck()
        updateQueue(mApiClient.enqueue(song.id, song.provider.id).process())
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song) {
        state.connectionCheck()
        updateQueue(mApiClient.dequeue(song.id, song.provider.id).process())
    }

    suspend fun moveEntry(entry: QueueEntry, newPosition: Int) {
        state.connectionCheck()
        updateQueue(mApiClient.moveEntry(entry, newPosition).process())
    }

    suspend fun search(providerId: String, query: String): List<Song> {
        state.connectionCheck()
        return mApiClient.searchForSong(providerId, query).process() ?: listOf()
    }

    suspend fun suggestions(suggesterId: String): List<Song> {
        state.connectionCheck()
        return mApiClient.getSuggestions(suggesterId).process() ?: listOf()
    }

    suspend fun deleteSuggestion(suggesterId: String, song: Song) {
        state.connectionCheck()
        mApiClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process()
    }

    suspend fun pause() {
        state.connectionCheck()
        updatePlayer(mApiClient.pause().process())
    }

    suspend fun play() {
        state.connectionCheck()
        updatePlayer(mApiClient.play().process())
    }

    suspend fun skip() {
        state.connectionCheck()
        updatePlayer(mApiClient.skip().process())
    }

    suspend fun getProvider(): List<MusicBotPlugin> {
        state.connectionCheck()
        return mApiClient.getProvider().process() ?: listOf()
    }

    suspend fun getSuggesters(): List<MusicBotPlugin> {
        state.connectionCheck()
        return mApiClient.getSuggesters().process() ?: listOf()
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
        if (!mQueue.hasObservers()) {
            mQueueUpdateTimer?.cancel()
            mQueueUpdateTimer = null
        }
    }

    fun getPlayerState(period: Long = 500): LiveData<PlayerState> {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer = timer(period = period) { updatePlayer() }
        return mPlayerState
    }

    fun startPlayerUpdates() {
        if (mPlayerState.hasObservers()) mPlayerUpdateTimer = timer(period = 500) { updatePlayer() }
    }

    fun stopPlayerUpdates() {
        if (!mPlayerState.hasObservers()) {
            mPlayerUpdateTimer?.cancel()
            mPlayerUpdateTimer = null
        }
    }

    private fun updateQueue(newQueue: List<QueueEntry>? = null) = GlobalScope.launch {
        if (newQueue != null) Timber.d("Manual Queue Update")
        try {
            state.connectionCheck()
            val queue = newQueue ?: mApiClient.getQueue().process() ?: listOf()
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
            val state = playerState ?: mApiClient.getPlayerState().process() ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) {
                mPlayerState.value = state
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
