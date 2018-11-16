package me.iberger.jmusicbot.data

import android.graphics.Bitmap
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Song(
    val id: String,
    val title: String,
    val description: String,
    val albumArtUrl: Bitmap,
    val duration: Int,
    val provider: MusicBotPlugin
)