package com.ivoberger.jmusicbot.model

import com.ivoberger.jmusicbot.KEY_PREFERENCES
import splitties.experimental.ExperimentalSplittiesApi
import splitties.preferences.Preferences

@ExperimentalSplittiesApi
internal object BotPreferences : Preferences(KEY_PREFERENCES) {
    var user: User? = null
        get() = serializedUser?.let { User.fromString(it) }
        set(value) {
            field = value
            serializedUser = value?.toString()
        }
    private var serializedUser: String? by stringOrNullPref()

    var authToken: Auth.Token? = null
        get() {
            return mAuthToken?.let { Auth.Token(it) }
        }
        set(value) {
            field = value
            mAuthToken = field?.toString()
        }
    private var mAuthToken: String? by stringOrNullPref()
}
