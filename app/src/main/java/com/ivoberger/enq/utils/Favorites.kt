package com.ivoberger.enq.utils

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.ivoberger.enq.KEY_FAVORITES
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.jmusicbot.model.MoshiTypes
import com.ivoberger.jmusicbot.model.Song
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.toast.toast
import timber.log.Timber

fun changeFavoriteStatus(context: Context, song: Song) = GlobalScope.launch {
    if (song in MainActivity.favorites) {
        Timber.d("Removing $song from favorites")
        MainActivity.favorites.remove(song)
        withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.msg_removed_from_favs, song.title))
        }
    } else {
        Timber.d("Adding $song to favorites")
        MainActivity.favorites.add(song)
        withContext(Dispatchers.Main) { context.toast(context.getString(R.string.msg_added_to_favs, song.title)) }
    }
    saveFavorites(context, MainActivity.favorites)
}

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

    val favoritesString =
        PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_FAVORITES, "")
    if (!favoritesString.isNullOrBlank()) adapter.fromJson(favoritesString)?.also { return it.toMutableList() }
    return mutableListOf()
}
