package me.iberger.jmusicbot

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import me.iberger.jmusicbot.data.Credentials
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.data.Song
import me.iberger.jmusicbot.data.User
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.discoverHost
import me.iberger.jmusicbot.network.process
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

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

    var user: User = user
        set(newUser) {
            newUser.save(mPreferences)
        }
    private val queue: MutableList<Song> = mutableListOf()

    fun enqueue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        apiClient.enqueue(song.id, song.provider.id).execute().process()
    }

    fun dequeue(song: Song): Deferred<List<QueueEntry>> = mCRScope.async {
        apiClient.dequeue(song.id, song.provider.id).execute().process()
    }

    companion object {

        internal val mCRScope = CoroutineScope(Dispatchers.IO)
        lateinit var instance: MusicBot
        internal var hostAddress: String? = null
        private val mMoshi = Moshi.Builder()
            .add(Credentials.Token)
            .add(KotlinJsonAdapterFactory())
            .build()

        fun init(context: Context, hostAddress: String? = null, user: User? = null): Deferred<MusicBot?> =
            mCRScope.async {
                val preferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
                val botUser = user
                    ?: User.load(preferences, mMoshi)
                    ?: throw IllegalStateException("No User saved")
                val baseUrl = hostAddress
                    ?: discoverHost(context).await()
                instance = MusicBot(context.applicationContext, baseUrl, botUser)
                return@async instance
            }

        fun hasUser(context: Context) =
            context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE).contains(KEY_USER)


        fun registerUser(context: Context, name: String, baseUrl: String? = null): Deferred<User?> = mCRScope.async {
            hostAddress = baseUrl ?: hostAddress ?: discoverHost(context).await()
            return@async try {
                val token = Retrofit.Builder()
                    .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
                    .baseUrl(hostAddress!!)
                    .build()
                    .create(MusicBotAPI::class.java)
                    .registerUser(Credentials.Register(name)).execute()
                    .process(errorCodes = mapOf(409 to Exception("User taken")))
                User(name, token)
            } catch (e: java.lang.Exception) {
                null
            }
        }
    }
}