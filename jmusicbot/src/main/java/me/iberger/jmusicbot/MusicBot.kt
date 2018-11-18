package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import me.iberger.jmusicbot.data.*
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.process
import me.iberger.jmusicbot.network.verifyHostAddress
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class MusicBot(
    private val mPreferences: SharedPreferences,
    baseUrl: String,
    user: User,
    var authToken: String
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
    }.build()
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
        }

    @Throws(InvalidParametersException::class, AuthException::class)
    fun changePassword(newPassword: String) = mCRScope.async {
        authToken = apiClient.changePassword(
            Credentials.PasswordChange((newPassword))
        ).execute().process()
        user.password = newPassword
        user.save(mPreferences)
    }

    var queue: List<QueueEntry> = listOf()
        get() = apiClient.getQueue().execute().process()

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun enqueue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        queue = apiClient.enqueue(song.id, song.provider.id).execute().process()
        return@async queue
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun dequeue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        queue = apiClient.dequeue(song.id, song.provider.id).execute().process()
        return@async queue
    }

    val history: List<QueueEntry>
        get() = apiClient.getHistory().execute().process()

    fun getSuggestions(suggester: MusicBotPlugin): Deferred<List<Song>> = mCRScope.async {
        apiClient.getSuggestions(suggester.id).execute().process()
    }

    fun deleteSuggestion(suggester: MusicBotPlugin, song: Song, provider: MusicBotPlugin): Deferred<Unit> =
        mCRScope.async { apiClient.deleteSuggestion(suggester.id, song.id, provider.id).execute().process() }

    fun changePlayerState(action: PlayerAction) = mCRScope.async {
        apiClient.setPlayerState(PlayerStateChange(action)).execute().process()
    }

    val provider: List<MusicBotPlugin>
        get() = apiClient.getProvider().execute().process()

    val suggesters: List<MusicBotPlugin>
        get() = apiClient.getSuggesters().execute().process()

    val playerState: PlayerState
        get() = apiClient.getPlayerState().execute().process()

    companion object {

        internal val mCRScope = CoroutineScope(Dispatchers.IO)
        lateinit var instance: MusicBot
        internal var baseUrl: String? = null

        private val mMoshi = Moshi.Builder()
            .build()

        @Throws(IllegalArgumentException::class, UsernameTakenException::class)
        fun init(
            context: Context, userName: String? = null, password: String? = null, hostAddress: String? = null
        ): Deferred<MusicBot> = mCRScope.async {
            val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
            verifyHostAddress(context, hostAddress)
            val apiClient = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                .baseUrl(baseUrl!!)
                .build()
                .create(MusicBotAPI::class.java)
            val user: User
            val authToken: String
            if (hasUser(context).await()) {
                user = User.load(preferences, mMoshi)!!
                authToken = if (user.password == null) registerUser(apiClient, user.name)
                else loginUser(apiClient, user)
            } else {
                user = User(
                    userName ?: throw IllegalArgumentException("No user saved and no username given"),
                    password = password
                )
                authToken = if (password.isNullOrBlank()) registerUser(apiClient, userName)
                else try {
                    loginUser(apiClient, user)
                } catch (e: NotFoundException) {
                    Timber.w(e)
                    val tmpToken = registerUser(apiClient, userName)
                    instance = MusicBot(preferences, baseUrl!!, user, tmpToken)
                    instance.changePassword(password).await()
                    return@async instance
                }
            }
            instance = MusicBot(preferences, baseUrl!!, user, authToken)
            return@async instance
        }

        fun hasUser(context: Context) =
            mCRScope.async { context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER) }

        fun hasServer(context: Context): Deferred<Boolean> = mCRScope.async {
            verifyHostAddress(context)
            return@async baseUrl != null
        }

        private fun loginUser(apiClient: MusicBotAPI, user: User): String {
            Timber.d("Logging in user ${user.name}")
            Timber.d(mMoshi.adapter<Credentials.Login>(Credentials.Login::class.java).toJson(Credentials.Login(user)))
            return apiClient
                .login(Credentials.Login(user)).execute()
                .process()
        }

        private fun registerUser(apiClient: MusicBotAPI, name: String): String {
            Timber.d("Registering user $name")
            return apiClient
                .registerUser(Credentials.Register(name)).execute()
                .process(errorCodes = mapOf(409 to UsernameTakenException()))
        }
    }
}