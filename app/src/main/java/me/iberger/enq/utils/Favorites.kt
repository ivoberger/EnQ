package me.iberger.enq.utils

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.KEY_FAVORITES
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.jmusicbot.data.MoshiTypes
import me.iberger.jmusicbot.data.Song
import timber.log.Timber

fun changeFavoriteStatus(context: Context, song: Song) = GlobalScope.launch {
    if (song in MainActivity.favorites) {
        Timber.d("Removing $song from favorites")
        MainActivity.favorites.remove(song)
        withContext(Dispatchers.Main) {
            context.toastShort(context.getString(R.string.msg_removed_from_favs, song.title))
        }
    } else {
        Timber.d("Adding $song to favorites")
        MainActivity.favorites.add(song)
        withContext(Dispatchers.Main) {
            context.toastShort(context.getString(R.string.msg_added_to_favs, song.title))
        }
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
