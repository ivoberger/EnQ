package me.iberger.jmusicbot

import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.auth0.android.jwt.JWT
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import me.iberger.jmusicbot.exceptions.*
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.model.*
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.discoverHost
import me.iberger.jmusicbot.network.process
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import splitties.systemservices.wifiManager
import timber.log.Timber
import java.util.*
import kotlin.concurrent.timer


object JMusicBot {
    var state: MusicBotState = MusicBotState.DISCONNECTED
        set(newState) {
            Timber.d("State changed from $field to $newState")
            field = newState
        }
    private val mWifiManager: WifiManager? by lazy { wifiManager }
    internal val mMoshi: Moshi by lazy { Moshi.Builder().build() }

    private var baseUrl: String? = null
        set(value) {
            field = value
            value?.let { mRetrofit = mRetrofit.newBuilder().baseUrl(it).build() }
        }

    internal var mOkHttpClient: OkHttpClient = OkHttpClient.Builder().cache(null).build()
        set(value) {
            field = value
            // rebuild retrofit with new okHttpClient
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

    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()

    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()
    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    var user: User?
        get() = BotPreferences.user
        set(value) {
            BotPreferences.user = value
        }
    val userPermissions: MutableList<Permissions> = mutableListOf()

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
        try {
            register(userName)
            if (!password.isNullOrBlank()) changePassword(password)
            state = MusicBotState.CONNECTED
            return
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            if (password.isNullOrBlank() && BotPreferences.user?.password == null) {
                Timber.d("No passwords found, throwing exception, $password, ${BotPreferences.user}")
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

    private fun tokenValid(): Boolean {
        if (BotPreferences.authToken != null && !BotPreferences.authToken!!.isExpired(60) && user?.password == null) {
            state = MusicBotState.CONNECTED
            return true
        }
        return false
//        try {
//            BotPreferences.authToken?.let {
//                if (it.isExpired(60)) return false
//                mApiClient.testToken(it.toString()).process()
//            }
//        } catch (e: Exception) {
//            if (e !is AuthException && e !is ServerErrorException) {
//                Timber.d("Valid Token")
//                state = MusicBotState.CONNECTED
//                return true
//            } else Timber.w(e.localizedMessage)
//        }
//        Timber.d("Invalid Token")
//        return false
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun register(userName: String? = null) {
        Timber.d("Registering ${BotPreferences.user}")
        state.serverCheck()
        val credentials = when {
            (!userName.isNullOrBlank()) -> {
                BotPreferences.user = User(userName)
                Credentials.Register(userName)
            }
            BotPreferences.user != null -> Credentials.Register(BotPreferences.user!!)
            else -> throw IllegalStateException("No username stored or supplied")
        }
        BotPreferences.authToken = JWT(mApiClient.registerUser(credentials).process())
        Timber.d("Registered ${BotPreferences.user}")
    }

    @Throws(
        InvalidParametersException::class,
        AuthException::class,
        NotFoundException::class,
        ServerErrorException::class,
        IllegalStateException::class
    )
    suspend fun login(userName: String? = null, password: String? = null) {
        Timber.d("Logging in ${BotPreferences.user}")
        state.serverCheck()
        val credentials = when {
            (!(userName.isNullOrBlank() || password.isNullOrBlank())) -> {
                BotPreferences.user = User(userName, password)
                Credentials.Login(userName, password)
            }
            BotPreferences.user != null -> Credentials.Login(BotPreferences.user!!)
            else -> throw IllegalStateException("No user stored or supplied")
        }
        BotPreferences.authToken = JWT(mApiClient.login(credentials).process())
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) {
        state.connectionCheck()
        BotPreferences.authToken =
                JWT(mApiClient.changePassword(Credentials.PasswordChange((newPassword))).process())
        BotPreferences.authToken?.also { BotPreferences.user?.password = newPassword }
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
        BotPreferences.authToken ?: throw IllegalStateException("Auth token is null")
        mApiClient.deleteUser().process()
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) {
        state.connectionCheck()
        try {
            val res = mApiClient.enqueue(song.id, song.provider.id).process()
            updateQueue(res)
        } catch (e: ServerErrorException) {
            onConnectionLost(e)
        }

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
                    state.job?.join()
                    if (baseUrl != null) {
                        authorize()
                        return@runBlocking
                    }
                    delay(500L)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
        connectionChangeListeners.forEach { it.onConnectionRecovered() }
        startQueueUpdates()
        startPlayerUpdates()
    }
}
