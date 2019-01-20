package me.iberger.jmusicbot

import android.content.Context
import android.net.wifi.WifiManager
import androidx.core.content.edit
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.iberger.jmusicbot.exceptions.NotFoundException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.model.Credentials
import me.iberger.jmusicbot.model.User
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.process
import me.iberger.jmusicbot.network.verifyHostAddress
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber

class MusicBot {


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
            Timber.d(mMoshi.adapter<Credentials.Login>(Credentials.Login::class.java).toJson(Credentials.Login(user)))
            return apiClient.login(Credentials.Login(user)).process().await()!!
        }

        private suspend fun registerUser(name: String): String {
            Timber.d("Registering user $name")
            return apiClient.registerUser(Credentials.Register(name)).process(errorCodes = mapOf(409 to UsernameTakenException())).await()!!
        }
    }
}
