package com.ivoberger.enq.persistence

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ivoberger.enq.R
import com.ivoberger.enq.model.ServerInfo
import com.ivoberger.enq.utils.addToEnd
import com.ivoberger.jmusicbot.model.*
import com.squareup.moshi.Moshi
import kotlinx.android.parcel.Parceler
import kotlinx.coroutines.*
import splitties.init.appCtx
import splitties.toast.toast
import timber.log.Timber

object AppSettings {

    private val preferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(appCtx) }

    private const val KEY_LAST_PROVIDER = "lastProvider"
    private const val KEY_LAST_SUGGESTER = "lastSuggester"
    private const val KEY_SAVED_USERS = "savedUsers"
    private const val KEY_SAVED_SERVERS = "savedServers"
    private const val KEY_FAVORITES = "favorites"

    // serialization util vars
    val mMoshi by lazy { Moshi.Builder().build() }
    private val mMusicBotPluginAdapter: MusicBotPluginJsonAdapter by lazy { MusicBotPluginJsonAdapter(mMoshi) }
    private val mUserListAdapter by lazy { mMoshi.adapter<List<User>>(MoshiTypes.UserList) }
    private val mFavoritesAdapter by lazy { mMoshi.adapter<List<Song>>(MoshiTypes.SongList) }
    private val mServerInfoListAdapter by lazy { mMoshi.adapter<List<ServerInfo>>(ServerInfo.listMoshiType) }
    private val mVersionInfoAdapter by lazy { mMoshi.adapter<VersionInfo>(VersionInfo::class.java) }

    var lastProvider: MusicBotPlugin?
        get() = loadString(KEY_LAST_PROVIDER)?.let { mMusicBotPluginAdapter.fromJson(it) }
        set(value) = saveString(KEY_LAST_PROVIDER, mMusicBotPluginAdapter.toJson(value))
    var lastSuggester: MusicBotPlugin?
        get() = loadString(KEY_LAST_SUGGESTER)?.let { mMusicBotPluginAdapter.fromJson(it) }
        set(value) = value?.let { saveString(KEY_LAST_SUGGESTER, mMusicBotPluginAdapter.toJson(it)) } ?: Unit


    // favorites management

    var favorites: List<Song>
        get() = loadString(KEY_FAVORITES)?.let { mFavoritesAdapter.fromJson(it) } ?: listOf()
        set(value) {
            saveString(KEY_FAVORITES, mFavoritesAdapter.toJson(value))
            MainScope().launch { favoritesLiveData.value = value }
        }

    private val favoritesLiveData by lazy { MutableLiveData<List<Song>>(favorites) }

    fun getFavoritesLiveData() = favoritesLiveData as LiveData<List<Song>>

    fun changeFavoriteStatus(context: Context, song: Song) = GlobalScope.launch {
        if (song in favorites) {
            Timber.d("Removing $song from favorites")
            favorites = favorites - song
            withContext(Dispatchers.Main) {
                context.toast(context.getString(R.string.msg_removed_from_favs, song.title))
            }
        } else {
            Timber.d("Adding $song to favorites")
            favorites = favorites + song
            withContext(Dispatchers.Main) { context.toast(context.getString(R.string.msg_added_to_favs, song.title)) }
        }
    }


    // saved user management

    private var savedUsers: List<User>
        get() = loadString(KEY_SAVED_USERS)?.let { mUserListAdapter.fromJson(it) } ?: listOf()
        set(value) = saveString(KEY_SAVED_USERS, mUserListAdapter.toJson(value))

    fun addUser(newUser: User) {
        Timber.d("Adding user ${newUser.name}")
        savedUsers = savedUsers.addToEnd(newUser)
    }

    fun getLatestUser(): User? = savedUsers.lastOrNull()

    fun clearSavedUsers() = run { savedUsers = listOf() }


    // saved server management

    private var savedServers: List<ServerInfo>
        get() = loadString(KEY_SAVED_SERVERS)?.let { mServerInfoListAdapter.fromJson(it) } ?: listOf()
        set(value) = saveString(KEY_SAVED_SERVERS, mServerInfoListAdapter.toJson(value))

    fun addServer(newServer: ServerInfo) = run { savedServers = savedServers.addToEnd(newServer) }
    fun isServerKnown(toCheck: ServerInfo) = savedServers.contains(toCheck)
    fun getLatestServer(): ServerInfo? = savedServers.lastOrNull()


    // utils

    private fun saveString(key: String, value: String) = preferences.edit { putString(key, value) }
    private fun loadString(key: String): String? = preferences.getString(key, null)

    object VersionInfoParceler : Parceler<VersionInfo> {
        override fun create(parcel: Parcel): VersionInfo =
            mVersionInfoAdapter.fromJson(parcel.readString()!!)!!

        override fun VersionInfo.write(parcel: Parcel, flags: Int) =
            parcel.writeString(mVersionInfoAdapter.toJson(this))
    }
}
