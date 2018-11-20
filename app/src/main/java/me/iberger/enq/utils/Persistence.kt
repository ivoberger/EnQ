package me.iberger.enq.utils

import android.preference.PreferenceManager
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import me.iberger.enq.KEY_FAVOURITES
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.data.MoshiTypes
import me.iberger.jmusicbot.data.Song
import timber.log.Timber

fun MainActivity.saveFavorites(favorites: List<Song>) {
    Timber.d("Saving Favorites")
    val adapter = Moshi.Builder().build().adapter<List<Song>>(MoshiTypes.SongList)

    PreferenceManager.getDefaultSharedPreferences(this).edit {
        putString(KEY_FAVOURITES, adapter.toJson(favorites))
    }
}

fun MainActivity.loadFavorites(): MutableList<Song> {
    Timber.d("Loading Favorites")
    val adapter = Moshi.Builder().build().adapter<List<Song>>(MoshiTypes.SongList)

    val favoritesString = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_FAVOURITES, "")
    if (!favoritesString.isNullOrBlank()) adapter.fromJson(favoritesString)?.also { return it.toMutableList() }
    return mutableListOf()
}