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
import me.iberger.jmusicbot.model.*
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.TokenAuthenticator
import me.iberger.jmusicbot.network.discoverHost
import me.iberger.jmusicbot.network.process
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.*
import kotlin.concurrent.fixedRateTimer


object NewMusicBot {
    var state: MusicBotState = MusicBotState.NEEDS_INIT

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mWifiManager: WifiManager
    private val mMoshi: Moshi by lazy { Moshi.Builder().build() }

    var baseUrl: String? = null
        set(value) {
            field = value
            value?.let { mRetrofit = mRetrofit.newBuilder().baseUrl(it).build() }
        }

    private var mOkHttpClient: OkHttpClient =
        OkHttpClient.Builder().authenticator(TokenAuthenticator()).cache(null).build()
        set(value) {
            field = value
            mRetrofit = mRetrofit.newBuilder().client(value).build()
        }

    private var mRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(baseUrl ?: "localhost")
        .client(mOkHttpClient)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()
        set(value) {
            field = value
            // rebuild API client with new retrofit
            mApiClient = value.create(MusicBotAPI::class.java)
        }

    private var mApiClient: MusicBotAPI = mRetrofit.create(MusicBotAPI::class.java)

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
                mOkHttpClient = mOkHttpClient.newBuilder().addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, it).build())
                }.build()
            }
        }

    private val mQueue: MutableLiveData<List<QueueEntry>> = MutableLiveData()
    private val mPlayerState: MutableLiveData<PlayerState> = MutableLiveData()

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    fun init(context: Context, startAutoDiscover: Boolean = true) {
        Timber.d("Initiating MusicBot")
        mPreferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        mWifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        state = MusicBotState.DISCONNECTED
        if (startAutoDiscover) discoverHost()
    }

    fun discoverHost() = GlobalScope.launch {
        state = MusicBotState.CONNECTING
        state.job = async { discoverHost(mWifiManager) }
        baseUrl = (state.job as? Deferred<String?>)?.await()
        state = if (baseUrl != null) MusicBotState.NEEDS_AUTH else MusicBotState.DISCONNECTED
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
        state.connectionCheck()
        val credentials = when {
            (userName != null) -> Credentials.Register(userName)
            user != null -> Credentials.Register(user!!)
            else -> throw IllegalArgumentException("No username stored or supplied")
        }
        authToken = mApiClient.registerUser(credentials).process().await()
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
        state.connectionCheck()
        val credentials = when {
            (userName != null && password != null) -> Credentials.Login(userName, password)
            user != null -> Credentials.Login(user!!)
            else -> throw IllegalArgumentException("No user stored or supplied")
        }
        authToken = mApiClient.login(credentials).process().await()
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
        mApiClient.deleteUser().process().await()
    }

    @Throws(InvalidParametersException::class, AuthException::class)
    suspend fun changePassword(newPassword: String) {
        state.connectionCheck()
        authToken = mApiClient.changePassword(Credentials.PasswordChange((newPassword))).process().await()
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
    suspend fun refreshToken() {
        state.connectionCheck()
        user?.let { authToken = mApiClient.login(Credentials.Login(it)).process().await() }
        throw java.lang.IllegalStateException("User is null")
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    suspend fun enqueue(song: Song) {
        state.connectionCheck()
        updateQueue(mApiClient.enqueue(song.id, song.provider.id).process().await())
    }

    suspend fun pause(): Unit = updatePlayer(mApiClient.pause().process().await())
    suspend fun play(): Unit = updatePlayer(mApiClient.play().process().await())
    suspend fun skip(): Unit = updatePlayer(mApiClient.skip().process().await())

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
            val queue = newQueue ?: mApiClient.getQueue().process().await() ?: listOf()
            withContext(Dispatchers.Main) { mQueue.value = queue }
        } catch (e: Exception) {
            Timber.w(e)
            // TODO: propagate error
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) = runBlocking {
        try {
            val state = playerState ?: mApiClient.getPlayerState().process().await() ?: PlayerState(PlayerStates.ERROR)
            withContext(Dispatchers.Main) {
                mPlayerState.value = state
            }
        } catch (e: Exception) {
            // TODO: propagate error
        }
    }
}
