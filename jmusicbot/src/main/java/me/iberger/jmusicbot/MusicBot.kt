package me.iberger.jmusicbot

import android.content.Context
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
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.IOException

class MusicBot(context: Context, baseUrl: String, user: User) {

    private val mPreferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder().addHeader(
                    KEY_AUTHORIZATION,
                    user.authorization.token
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
        user.authorization = apiClient.changePassword(
            Credentials.PasswordChange((newPassword))
        ).execute().process()
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
        ): Deferred<MusicBot> =
            mCRScope.async {
                val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
                val hostJob = verifyHostAddress(context, hostAddress)
                val botUser = User.load(preferences, mMoshi)
                    ?: run {
                        hostJob.join()
                        return@run registerUser(
                            userName ?: throw IllegalArgumentException("No user saved and no username given")
                        ).await()
                    }
                    ?: throw UnknownError("Registration Failed")
                hostJob.join()
                instance = MusicBot(context.applicationContext, baseUrl!!, botUser)
                return@async instance
            }

        fun hasUser(context: Context) =
            context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER)

        private fun registerUser(name: String): Deferred<User> =
            mCRScope.async {
                Timber.d("Registering user $name")
                val token = Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                    .baseUrl(baseUrl!!)
                    .build()
                    .create(MusicBotAPI::class.java)
                    .registerUser(Credentials.Register(name)).execute()
                    .process(errorCodes = mapOf(409 to UsernameTakenException()))
                return@async User(name, token)
            }
    }
}