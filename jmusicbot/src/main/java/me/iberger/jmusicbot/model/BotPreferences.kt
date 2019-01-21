package me.iberger.jmusicbot.model

import com.auth0.android.jwt.JWT
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.KEY_AUTHORIZATION
import me.iberger.jmusicbot.KEY_PREFERENCES
import me.iberger.jmusicbot.network.TokenAuthenticator
import splitties.preferences.Preferences

internal object BotPreferences : Preferences(KEY_PREFERENCES) {
    var user: User? = null
        get() = serializedUser?.let { User.fromString(it) }
        set(value) {
            field = value
            serializedUser = value?.toString()
        }
    private var serializedUser: String? by stringOrNullPref()

    var authToken: JWT? = null
        get() {
            return mAuthToken?.let { JWT(it) }
        }
        set(value) {
            field = value
            mAuthToken = field?.toString()
            JMusicBot.userPermissions.clear()
            field?.let {
                JMusicBot.userPermissions.addAll(Permissions.fromClaims(it.claims))
                JMusicBot.mOkHttpClient = JMusicBot.mOkHttpClient.newBuilder().addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().addHeader(KEY_AUTHORIZATION, it.toString()).build())
                }.authenticator(TokenAuthenticator()).build()
            }
        }
    private var mAuthToken: String? by stringOrNullPref()
}
