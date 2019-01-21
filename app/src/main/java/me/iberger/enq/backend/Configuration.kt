package me.iberger.enq.backend

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import me.iberger.jmusicbot.model.MusicBotPlugin
import me.iberger.jmusicbot.model.MusicBotPluginJsonAdapter
import timber.log.Timber

class Configuration(private val preferences: SharedPreferences) {

    constructor(context: Context) : this(PreferenceManager.getDefaultSharedPreferences(context))

    companion object {
        const val KEY_LAST_PROVIDER = "lastProvider"
        const val KEY_LAST_SUGGESTER = "lastSuggester"
    }

    private val mMoshi by lazy { Moshi.Builder().build() }
    private val mMusicBotPluginAdapter: MusicBotPluginJsonAdapter by lazy {
        Timber.d("Getting JSON adapter")
        MusicBotPluginJsonAdapter((mMoshi))
    }

    var lastProvider: MusicBotPlugin?
        get() = loadString(KEY_LAST_PROVIDER)?.let {
            Timber.d("Getting provider")
            mMusicBotPluginAdapter.fromJson(it)
        }
        set(value) = saveString(KEY_LAST_PROVIDER, mMusicBotPluginAdapter.toJson(value))
    var lastSuggester: MusicBotPlugin?
        get() = loadString(KEY_LAST_SUGGESTER)?.let {
            Timber.d("Getting suggester")
            mMusicBotPluginAdapter.fromJson(it)
        }
        set(value) {
            Timber.d("Saving suggester")
            saveString(KEY_LAST_SUGGESTER, mMusicBotPluginAdapter.toJson(value))
        }

    private fun saveString(key: String, value: String) = preferences.edit { putString(key, value) }
    private fun loadString(key: String): String? = preferences.getString(key, null)
}
