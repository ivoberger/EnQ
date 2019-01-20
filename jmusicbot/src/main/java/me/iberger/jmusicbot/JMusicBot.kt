package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.ServerErrorException
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.model.*
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.TokenAuthenticator
import me.iberger.jmusicbot.network.discoverHost
import me.iberger.jmusicbot.network.process
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import splitties.systemservices.wifiManager
import timber.log.Timber
import java.util.*
import kotlin.concurrent.fixedRateTimer


object JMusicBot {
    var state: MusicBotState = MusicBotState.NEEDS_INIT
        set(newState) {
            Timber.d("State changed from $field to $newState")
            field = newState
        }

    private lateinit var mPreferences: SharedPreferences
    private val mWifiManager: WifiManager by lazy { wifiManager }
    private val mMoshi: Moshi by lazy { Moshi.Builder().build() }

    private var baseUrl: String? = null
        set(value) {
            field = value
            value?.let { mRetrofit = mRetrofit.newBuilder().baseUrl(it).build() }
        }

    private var mOkHttpClient: OkHttpClient = OkHttpClient.Builder().cache(null).build()
        set(value) {
            field = value
            mRetrofit = mRetrofit.newBuilder().client(value).build()
        }

    private var mRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(baseUrl ?: "http://localhost")
        .client(mOkHttpClient)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()
        set(value) {
            field = value
            // rebuild API client with new retrofit
            mApiClient = value.create(MusicBotAPI::class.java)
        }

    private var mApiClient: MusicBotAPI = mRetrofit.create(MusicBotAPI::class.java)
    val connectionChangeListeners: MutableList<ConnectionChangeListener> = mutableListOf()

    var user: User? = null
        get() = if (state.isInitialized() && mPreferences.contains(KEY_USER)) User.load(mPreferences, mMoshi)
        else field
        set(newUser) {
            if (state.isInitialized()) newUser?.save(mPreferences, mMoshi)
            field = newUser
        }

    var authToken: String? = null
        get() = if (state.isInitialized()) mPreferences.getString(KEY_AUTHORIZATION, field) else field
        set(newToken) {
            if (state.isInitialized()) mPreferences.edit { putString(KEY_AUTHORIZATION, newToken) }
            field = newToken
            // rebuild networking client if new token is not null
            newToken?.let {
                Timber.d("Setting new token")
                mOkHttpClient = mOkHttpClient.newBuilder().addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, it).build())
                }
                    .authenticator(TokenAuthenticator()).build()
            }
        }

    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()
    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    fun init(context: Context, startAutoDiscover: Boolean = true) {
        Timber.d("Initializing MusicBot")
        mPreferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
//        mPreferences.edit().remove(KEY_AUTHORIZATION).apply()
        state = MusicBotState.DISCONNECTED
        if (startAutoDiscover) discoverHost()
        Timber.d("MusicBot successfully initialized")
    }

    fun discoverHost() = GlobalScope.launch {
        Timber.d("Discovering host")
        state = MusicBotState.CONNECTING
        state.job = launch {
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

    suspend fun authorize(userName: String? = null, password: String? = null): Boolean {
        Timber.d("Starting authorization")
        if (tokenValid()) return true
        try {
            register(userName)
            password?.let { changePassword(it) }
            if (tokenValid()) return true
        } catch (e: Exception) {
            Timber.w(e)
        }
        try {
            login(userName, password)
            if (tokenValid()) return true
        } catch (e: Exception) {
            Timber.w(e)
        }
        Timber.d("Authorization failed")
        return false
    }

    private suspend fun tokenValid(): Boolean {
        try {
            authToken?.let { mApiClient.testToken(it).process() }
        } catch (e: Exception) {
            if (e !is AuthException) {
                Timber.d("Valid Token")
                state = MusicBotState.CONNECTED
                return true
            } else Timber.w(e.localizedMessage)
        }
        Timber.d("Invalid Token")
        return false
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class,
        IllegalArgumentException::class
    )
    suspend fun register(userName: String? = null) {
        Timber.d("Registering user")
        state.serverCheck()
        val credentials = when {
            (userName != null) -> {
                user = User(userName)
                Credentials.Register(userName)
            }
            user != null -> Credentials.Register(user!!)
            else -> throw IllegalArgumentException("No username stored or supplied")
        }
        authToken = mApiClient.registerUser(credentials).process()
        Timber.d("Registered user")
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class,
        IllegalArgumentException::class
    )
    suspend fun login(userName: String? = null, password: String? = null) {
        Timber.d("Logging in user")
        state.serverCheck()
        val credentials = when {
            (userName != null && password != null) -> {
                user = User(userName, password)
                Credentials.Login(userName, password)
            }
            user != null -> Credentials.Login(user!!)
            else -> throw IllegalArgumentException("No user stored or supplied")
        }
        authToken = mApiClient.login(credentials).process()
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) {
        state.connectionCheck()
        authToken = mApiClient.changePassword(Credentials.PasswordChange((newPassword))).process()
        authToken?.also {
            user?.password = newPassword
            user?.save(mPreferences)
        }
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
        authToken ?: throw IllegalStateException("Auth token is null")
        mApiClient.deleteUser().process()
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun refreshToken() {
        state.connectionCheck()
        user?.let { authToken = mApiClient.login(Credentials.Login(it)).process() }
        throw java.lang.IllegalStateException("User is null")
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

    suspend fun moveSong(entry: QueueEntry, newPosition: Int) {
        state.connectionCheck()
        updateQueue(mApiClient.moveSong(entry, newPosition).process())
    }

    suspend fun search(providerId: String, query: String): List<Song> {
        state.connectionCheck()
        return mApiClient.searchForSong(providerId, query).process()
    }

    suspend fun suggestions(suggesterId: String): List<Song> {
        state.connectionCheck()
        return mApiClient.getSuggestions(suggesterId).process()
    }

    suspend fun deleteSuggestion(suggesterId: String, song: Song) {
        state.connectionCheck()
        return mApiClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process()
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
        return mApiClient.getProvider().process()
    }

    suspend fun getSuggesters(): List<MusicBotPlugin> {
        state.connectionCheck()
        return mApiClient.getSuggesters().process()
    }

    fun getQueue(period: Long = 500): LiveData<List<QueueEntry>> {
        if (mQueueUpdateTimer == null) mQueueUpdateTimer = fixedRateTimer(period = period) { updateQueue() }
        return mQueue
    }

    fun stopQueueUpdates() {
        if (!mQueue.hasObservers()) {
            mQueueUpdateTimer?.cancel()
            mQueueUpdateTimer = null
        }
    }

    fun getPlayerState(period: Long = 500): LiveData<PlayerState> {
        if (mPlayerUpdateTimer == null) mPlayerUpdateTimer = fixedRateTimer(period = period) { updatePlayer() }
        return mPlayerState
    }

    fun stopPlayerUpdates() {
        if (!mPlayerState.hasObservers()) {
            mPlayerUpdateTimer?.cancel()
            mPlayerUpdateTimer = null
        }
    }

    private fun updateQueue(newQueue: List<QueueEntry>? = null) = runBlocking {
        try {
            state.connectionCheck()
            val queue = newQueue ?: mApiClient.getQueue().process() ?: listOf()
            withContext(Dispatchers.Main) { mQueue.value = queue }
        } catch (e: Exception) {
            Timber.w(e)
            // TODO: propagate error
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = runBlocking {
        try {
            state.connectionCheck()
            val state = playerState ?: mApiClient.getPlayerState().process() ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) {
                mPlayerState.value = state
            }
        } catch (e: Exception) {
            // TODO: propagate error
        }
    }

    fun onConnectionLost(e: Exception) {
        stopQueueUpdates()
        stopPlayerUpdates()
        baseUrl = null
        connectionChangeListeners.forEach { it.onConnectionLost(e) }
        runBlocking {
            while (true) {
                try {
                    discoverHost().join()
                    if (baseUrl != null) return@runBlocking
                    delay(500L)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
        connectionChangeListeners.forEach { it.onConnectionRecovered() }
    }
}
