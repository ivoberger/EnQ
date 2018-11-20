package me.iberger.enq.utils

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import me.iberger.enq.KEY_FAVORITES
import me.iberger.jmusicbot.data.MoshiTypes
import me.iberger.jmusicbot.data.Song
import timber.log.Timber

fun saveFavorites(context: Context, favorites: List<Song>) {
    Timber.d("Saving Favorites")
    val adapter = Moshi.Builder().build().adapter<List<Song>>(MoshiTypes.SongList)

    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putString(KEY_FAVORITES, adapter.toJson(favorites))
    }
}

fun loadFavorites(context: Context): MutableList<Song> {
    Timber.d("Loading Favorites")
    val adapter = Moshi.Builder().build().adapter<List<Song>>(MoshiTypes.SongList)

    val favoritesString = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_FAVORITES, "")
    if (!favoritesString.isNullOrBlank()) adapter.fromJson(favoritesString)?.also { return it.toMutableList() }
    return mutableListOf()
}