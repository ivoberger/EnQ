package com.ivoberger.enq.persistence

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import com.ivoberger.jmusicbot.model.MusicBotPluginJsonAdapter
import com.ivoberger.jmusicbot.model.User
import com.squareup.moshi.Moshi
import timber.log.Timber

class Configuration(private val preferences: SharedPreferences) {

    constructor(context: Context) : this(PreferenceManager.getDefaultSharedPreferences(context))

    companion object {
        const val KEY_LAST_PROVIDER = "lastProvider"
        const val KEY_LAST_SUGGESTER = "lastSuggester"
        const val KEY_SAVED_USERS = "savedUsers"
    }

    private val mMoshi by lazy { Moshi.Builder().build() }
    private val mMusicBotPluginAdapter: MusicBotPluginJsonAdapter by lazy { MusicBotPluginJsonAdapter(mMoshi) }
    private val mUserListAdapter = mMoshi.adapter<List<User>>(List::class.java)

    var lastProvider: MusicBotPlugin?
        get() = loadString(KEY_LAST_PROVIDER)?.let {
            Timber.d("Getting provider")
            mMusicBotPluginAdapter.fromJson(it)
        }
        set(value) = saveString(KEY_LAST_PROVIDER, mMusicBotPluginAdapter.toJson(value))
    var lastSuggester: MusicBotPlugin?
        get() = loadString(KEY_LAST_SUGGESTER)?.let { mMusicBotPluginAdapter.fromJson(it) }
        set(value) = value?.let { saveString(KEY_LAST_SUGGESTER, mMusicBotPluginAdapter.toJson(it)) } ?: Unit


    var savedUsers: List<User>?
        get() = loadString(KEY_SAVED_USERS)?.let { mUserListAdapter.fromJson(it) }
        set(value) = saveString(KEY_SAVED_USERS, mUserListAdapter.toJson(value ?: listOf()))

    private fun saveString(key: String, value: String) = preferences.edit { putString(key, value) }
    private fun loadString(key: String): String? = preferences.getString(key, null)
}
