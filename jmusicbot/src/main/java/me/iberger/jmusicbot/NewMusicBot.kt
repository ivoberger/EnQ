package me.iberger.jmusicbot

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import androidx.core.content.edit
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import me.iberger.jmusicbot.model.MusicBotState
import me.iberger.jmusicbot.model.User
import me.iberger.jmusicbot.network.MusicBotAPI
import me.iberger.jmusicbot.network.TokenAuthenticator
import me.iberger.jmusicbot.network.verifyHostAddress
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber


object NewMusicBot {
    var state: MusicBotState = MusicBotState.NEEDS_INIT

    private lateinit var mPreferences: SharedPreferences
    private lateinit var mWifiManager: WifiManager

    var baseUrl: String? = null
    var authToken: String? = null
        set(newToken) {
            mPreferences.edit { putString(KEY_AUTHORIZATION, newToken) }
            field = newToken
        }

    private val mMoshi: Moshi by lazy { Moshi.Builder().build() }

    private var mOkHttpClient: OkHttpClient = OkHttpClient.Builder().apply {
        authToken?.let {
            addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, it).build()
                )
            }
        }
    }.authenticator(TokenAuthenticator()).cache(null).build()

    private var mRetrofit: Retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(mMoshi).asLenient())
        .baseUrl(baseUrl ?: "localhost")
        .client(mOkHttpClient)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    private var mApiClient: MusicBotAPI = mRetrofit.create(MusicBotAPI::class.java)

    @Throws(IllegalArgumentException::class, UsernameTakenException::class)
    fun init(
        context: Context,
        userName: String? = null,
        password: String? = null,
        hostAddress: String? = null
    ): Deferred<MusicBot> = GlobalScope.async {
        Timber.d("Initiating MusicBot")
        mPreferences = context.getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        mWifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        verifyHostAddress(mWifiManager, hostAddress)
        Timber.d("User setup")
        if (!MusicBot.hasUser(context) || userName != null) {
            User(
                userName
                    ?: throw IllegalArgumentException("No user saved and no username given"),
                password = password
            ).save(mPreferences)
        }
        MusicBot.authorize(context).let {
            MusicBot.instance = MusicBot(mPreferences, mWifiManager, MusicBot.baseUrl!!, it.first, it.second)
            return@async MusicBot.instance!!
        }
    }
}
