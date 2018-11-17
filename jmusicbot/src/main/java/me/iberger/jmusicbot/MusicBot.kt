package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import me.iberger.jmusicbot.data.Credentials
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.data.Song
import me.iberger.jmusicbot.data.User
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.InvalidParametersException
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.process
import me.iberger.jmusicbot.network.verifyHostAddress
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException

class MusicBot(
    private val mPreferences: SharedPreferences,
    baseUrl: String,
    user: User,
    var authToken: String
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            Timber.d("Url: ${chain.request().url()}")
            val body = Buffer()
            chain.request().body()!!.writeTo(body)
            Timber.d("Body: ${body.readString(charset("UTF-8"))}")
            Timber.d("Method: ${chain.request().method()}")
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

    private val queue: MutableList<Song> = mutableListOf()

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun enqueue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        apiClient.enqueue(song.id, song.provider.id).execute().process()
    }

    @Throws(InvalidParametersException::class, AuthException::class, NotFoundException::class)
    fun dequeue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        apiClient.dequeue(song.id, song.provider.id).execute().process()
    }

    companion object {

        internal val mCRScope = CoroutineScope(Dispatchers.IO)
        lateinit var instance: MusicBot
        internal var baseUrl: String? = null

        private val mMoshi = Moshi.Builder()
            .build()

        @Throws(
            IllegalArgumentException::class,
            UnknownError::class,
            UsernameTakenException::class,
            IOException::class
        )
        fun init(
            context: Context,
            userName: String? = null,
            hostAddress: String? = null
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
            if (hasUser(context)) {
                user = User.load(preferences, mMoshi)!!
                authToken = if (user.password == null) registerUser(apiClient, user.name)
                else loginUser(apiClient, user)
            } else {
                user = User(userName ?: throw IllegalArgumentException("No user saved and no username given"))
                authToken = registerUser(apiClient, userName)
            }

            instance = MusicBot(preferences, baseUrl!!, user, authToken)
            return@async instance
        }

        fun hasUser(context: Context) =
            context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER)

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