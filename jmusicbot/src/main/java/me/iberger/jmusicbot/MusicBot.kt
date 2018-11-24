package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.iberger.jmusicbot.data.*
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.listener.PlayerUpdateListener
import me.iberger.jmusicbot.listener.QueueUpdateListener
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
    baseUrl: String,
    user: User,
    initToken: String
) {

    init {
        user.save(mPreferences)
        mPreferences.edit { putString(KEY_AUTHORIZATION, initToken) }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, authToken).build())
        }
    }.authenticator(TokenAuthenticator()).cache(null).build()

    private val apiClient: MusicBotAPI = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()
        .create(MusicBotAPI::class.java)

    // User operations

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

    val provider: List<MusicBotPlugin>
        get() = apiClient.getProvider().process()!!

    val suggesters: List<MusicBotPlugin>
        get() = apiClient.getSuggesters().process()!!

    private var mQueueUpdateTimer: Timer? = null
    private var mPlayerUpdateTimer: Timer? = null

    private val mQueueUpdateListeners: MutableList<QueueUpdateListener> = mutableListOf()
    private val mPlayerUpdateListeners: MutableList<PlayerUpdateListener> = mutableListOf()

    @Throws(InvalidParametersException::class, AuthException::class)
    fun changePassword(newPassword: String) = GlobalScope.async {
        authToken = apiClient.changePassword(
            Credentials.PasswordChange((newPassword))
        ).process()!!
        user.password = newPassword
        user.save(mPreferences)
    }

    fun refreshToken(): Response<String> = apiClient.login(Credentials.Login(user)).execute()

    fun search(providerId: String, query: String): Deferred<List<Song>> =
        GlobalScope.async { apiClient.searchForSong(providerId, query).process()!! }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun enqueue(song: Song) =
        GlobalScope.async { updateQueue(apiClient.enqueue(song.id, song.provider.id).process()) }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun dequeue(song: Song) =
        GlobalScope.async { updateQueue(apiClient.dequeue(song.id, song.provider.id).process()) }

    val history: List<QueueEntry>
        get() = apiClient.getHistory().process()!!

    fun getSuggestions(suggesterId: String): Deferred<List<Song>> = GlobalScope.async {
        apiClient.getSuggestions(suggesterId).process()!!
    }

    fun deleteSuggestion(suggesterId: String, song: Song): Deferred<Unit?> =
        GlobalScope.async { apiClient.deleteSuggestion(suggesterId, song.id, song.provider.id).process() }

    private fun changePlayerState(action: PlayerAction) =
        GlobalScope.async { updatePlayer(apiClient.setPlayerState(PlayerStateChange(action)).process()) }

    fun pause() = GlobalScope.async { apiClient.pause().process()!! }
    fun play() = GlobalScope.async { apiClient.play().process()!! }
    fun skip() = GlobalScope.async { apiClient.skip().process()!! }

    fun startQueueUpdates(listener: QueueUpdateListener, period: Long = 500) {
        mQueueUpdateListeners.add(listener)
        mQueueUpdateTimer = fixedRateTimer(period = period) { updateQueue() }
    }

    fun stopQueueUpdates(listener: QueueUpdateListener) {
        mQueueUpdateListeners.remove(listener)
        if (mQueueUpdateListeners.isEmpty()) {
            mQueueUpdateTimer?.cancel()
            mQueueUpdateTimer = null
        }
    }

    fun startPlayerUpdates(listener: PlayerUpdateListener, period: Long = 500) {
        mPlayerUpdateListeners.add(listener)
        mPlayerUpdateTimer = fixedRateTimer(period = period) { updatePlayer() }
    }

    fun stopPlayerUpdates(listener: PlayerUpdateListener) {
        mPlayerUpdateListeners.remove(listener)
        if (mPlayerUpdateListeners.isEmpty()) {
            mPlayerUpdateTimer?.cancel()
            mPlayerUpdateTimer = null
        }
    }

    private fun updateQueue(queue: List<QueueEntry>? = null) {
        try {
            val newQueue = queue ?: apiClient.getQueue().process()!!
            mQueueUpdateListeners.forEach { it.onQueueChanged(newQueue) }
        } catch (e: Exception) {
            mQueueUpdateListeners.forEach { it.onUpdateError(e) }
        }
    }

    private fun updatePlayer(playerState: PlayerState? = null) {
        try {
            val newState = playerState ?: apiClient.getPlayerState().process()!!
            mPlayerUpdateListeners.forEach { it.onPlayerStateChanged(newState) }
        } catch (e: Exception) {
            mPlayerUpdateListeners.forEach { it.onUpdateError(e) }
        }
    }


    // ########## Companion object with init functions ########## //

    companion object {

        lateinit var instance: MusicBot
        internal var baseUrl: String? = null

        private val mMoshi = Moshi.Builder()
            .build()

        @Throws(IllegalArgumentException::class, UsernameTakenException::class)
        fun init(
            context: Context, userName: String? = null, password: String? = null, hostAddress: String? = null
        ): Deferred<MusicBot> = GlobalScope.async {
            Timber.d("Initiating MusicBot")
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            verifyHostAddress(context, hostAddress)
            val apiClient = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                .baseUrl(baseUrl!!)
                .build()
                .create(MusicBotAPI::class.java)

            val authToken: String
            Timber.d("User setup")
            val user: User = if (hasUser(context).await()) {
                User.load(preferences, mMoshi)!!
            } else {
                User(
                    userName ?: throw IllegalArgumentException("No user saved and no username given"),
                    password = password
                )
            }
            try {
                val tmpToken = preferences.getString(KEY_AUTHORIZATION, null)
                tmpToken?.also {
                    Timber.d("Trying saved token")
                    apiClient.attemptLogin(it)
                    instance = MusicBot(preferences, baseUrl!!, user, it)
                    return@async instance
                }
                throw AuthException(AuthException.Reason.NEEDS_AUTH)
            } catch (e: AuthException) {
                authToken = if (user.password.isNullOrBlank()) registerUser(apiClient, user.name)
                else try {
                    loginUser(apiClient, user)
                } catch (e: NotFoundException) {
                    val tmpToken = registerUser(apiClient, user.name)
                    instance = MusicBot(preferences, baseUrl!!, user, tmpToken)
                    instance.changePassword(user.password!!).await()
                    return@async instance
                }
            }
            instance = MusicBot(preferences, baseUrl!!, user, authToken)
            return@async instance
        }

        fun hasUser(context: Context) =
            GlobalScope.async { context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER) }

        fun hasServer(context: Context): Deferred<Boolean> = GlobalScope.async {
            verifyHostAddress(context)
            return@async baseUrl != null
        }

        private fun loginUser(apiClient: MusicBotAPI, user: User): String {
            Timber.d("Logging in user ${user.name}")
            Timber.d(mMoshi.adapter<Credentials.Login>(Credentials.Login::class.java).toJson(Credentials.Login(user)))
            return apiClient.login(Credentials.Login(user)).process()!!
        }

        private fun registerUser(apiClient: MusicBotAPI, name: String): String {
            Timber.d("Registering user $name")
            return apiClient.registerUser(Credentials.Register(name)).process(errorCodes = mapOf(409 to UsernameTakenException()))!!
        }
    }
}