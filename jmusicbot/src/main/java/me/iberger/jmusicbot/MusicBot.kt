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
import me.iberger.jmusicbot.data.*
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.TokenAuthenticator
import me.iberger.jmusicbot.network.process
import me.iberger.jmusicbot.network.verifyHostAddress
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MusicBot(
    private val mPreferences: SharedPreferences,
    private val mWifiManager: WifiManager,
    baseUrl: String,
    user: User,
    initToken: String
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder().addHeader(
                    KEY_AUTHORIZATION,
                    authToken
                ).build()
            )
        }
    }.authenticator(TokenAuthenticator()).cache(null).build()

    private var apiClient: MusicBotAPI

    var user: User = user
        set(newUser) {
            newUser.save(mPreferences)
            field = newUser
        }

    var authToken: String = initToken
        set(newToken) {
            mPreferences.edit { putString(KEY_AUTHORIZATION, newToken) }
            field = newToken
        }

    init {
        this.user = user
        authToken = initToken
        apiClient = initApi(baseUrl)
    }

    private fun initApi(url: String) = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(url)
        .client(okHttpClient)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()
        .create(MusicBotAPI::class.java)

    val provider: Deferred<List<MusicBotPlugin>?>
        get() = apiClient.getProvider().process()

    val suggesters: Deferred<List<MusicBotPlugin>?>
        get() = apiClient.getSuggesters().process()

    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()
    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    val connectionChangeListeners: MutableList<ConnectionChangeListener> = mutableListOf()

    fun deleteUser(): Deferred<Unit?> = apiClient.deleteUser().process()

    @Throws(InvalidParametersException::class, AuthException::class)
    fun changePassword(newPassword: String) = GlobalScope.async {
        authToken = apiClient.changePassword(
            Credentials.PasswordChange((newPassword))
        ).process().await()!!
        user.password = newPassword
        user.save(mPreferences)
    }

    suspend fun refreshToken(): Response<String> = apiClient.login(Credentials.Login(user)).await()

    fun search(providerId: String, query: String): Deferred<List<Song>?> =
        apiClient.searchForSong(providerId, query).process()

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song): Unit = updateQueue(apiClient.enqueue(song.id, song.provider.id).process().await())

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun dequeue(song: Song): Unit = updateQueue(apiClient.dequeue(song.id, song.provider.id).process().await())

    suspend fun moveSong(entry: QueueEntry, newPosition: Int): Unit =
        updateQueue(apiClient.moveSong(entry, newPosition).process().await())

    fun lookupSong(providerId: String, songId: String) = apiClient.lookupSong(providerId, songId).process()

//    val history: List<QueueEntry?>
//        get() = apiClient.getHistory().process()

    fun getSuggestions(suggesterId: String): Deferred<List<Song>?> = apiClient.getSuggestions(suggesterId).process()

    fun deleteSuggestion(suggesterId: String, song: Song): Deferred<Unit?> =
        apiClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process()

    private suspend fun changePlayerState(action: PlayerAction): Unit =
        updatePlayer(apiClient.setPlayerState(PlayerStateChange(action)).process().await())

    suspend fun pause(): Unit = updatePlayer(apiClient.pause().process().await())
    suspend fun play(): Unit = updatePlayer(apiClient.play().process().await())
    suspend fun skip(): Unit = updatePlayer(apiClient.skip().process().await())

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
            val queue = newQueue ?: apiClient.getQueue().process().await()!!
            withContext(Dispatchers.Main) { mQueue.value = queue }
        } catch (e: Exception) {
            Timber.w(e)
            // TODO: propagate error
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = runBlocking {
        try {
            val state = playerState ?: apiClient.getPlayerState().process().await()!!
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
                    verifyHostAddress(mWifiManager)
                    if (baseUrl != null) return@runBlocking
                    delay(500L)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
        apiClient = initApi(baseUrl!!)
        connectionChangeListeners.forEach { it.onConnectionRecovered() }
    }

// ########## Companion object with init functions ########## //

    companion object {

        var instance: MusicBot? = null
        internal var baseUrl: String? = null

        private val mMoshi = Moshi.Builder().build()
        private lateinit var apiClient: MusicBotAPI

        @Throws(IllegalArgumentException::class, UsernameTakenException::class)
        fun init(
            context: Context,
            userName: String? = null,
            password: String? = null,
            hostAddress: String? = null
        ): Deferred<MusicBot> = GlobalScope.async {
            Timber.d("Initiating MusicBot")
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            verifyHostAddress(wifiManager, hostAddress)
            Timber.d("User setup")
            if (!hasUser(context) || userName != null) {
                User(
                    userName
                        ?: throw IllegalArgumentException("No user saved and no username given"),
                    password = password
                ).save(preferences)
            }
            authorize(context).let {
                instance = MusicBot(preferences, wifiManager, baseUrl!!, it.first, it.second)
                return@async instance!!
            }
        }

        suspend fun hasAuthorization(context: Context) = try {
            authorize(context)
            true
        } catch (e: Exception) {
            Timber.w(e)
            false
        }

        private suspend fun authorize(context: Context): Pair<User, String> {
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            if (!hasUser(context)) throw NotFoundException(
                NotFoundException.Type.USER,
                "No user saved"
            )
            preferences.getString(KEY_AUTHORIZATION, null)?.also {
                try {
                    apiClient.testToken(it).await()
                    return User.load(preferences)!! to it
                } catch (e: Exception) {
                    Timber.w(e)
                    return@also
                }
            }
            User.load(preferences, mMoshi)!!.let { user ->
                val token = if (!user.password.isNullOrBlank()) try {
                    loginUser(user)
                } catch (e: Exception) {
                    Timber.e(e)
                    apiClient.changePasswordWithToken(
                        registerUser(user.name),
                        Credentials.PasswordChange((user.password!!))
                    ).process().await()!!
                } else registerUser(user.name)
                preferences.edit { putString(KEY_AUTHORIZATION, token) }
                return user to token
            }
        }

        private fun hasUser(context: Context) =
            context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER)

        fun hasServer(context: Context): Boolean {
            verifyHostAddress(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            baseUrl?.also {
                apiClient = Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                    .baseUrl(it)
                    .addCallAdapterFactory(CoroutineCallAdapterFactory())
                    .build()
                    .create(MusicBotAPI::class.java)
                return true
            }
            return false
        }

        private suspend fun loginUser(user: User): String {
            Timber.d("Logging in user ${user.name}")
            Timber.d(
                mMoshi.adapter<Credentials.Login>(Credentials.Login::class.java).toJson(
                    Credentials.Login(user)
                )
            )
            return apiClient.login(Credentials.Login(user)).process().await()!!
        }

        private suspend fun registerUser(name: String): String {
            Timber.d("Registering user $name")
            return apiClient.registerUser(Credentials.Register(name)).process(errorCodes = mapOf(409 to UsernameTakenException())).await()!!
        }
    }
}
