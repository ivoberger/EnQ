package me.iberger.jmusicbot.data

import android.graphics.Bitmap

data class Song(
    val id: String,
    val title: String,
    val description: String,
    val albumArtUrl: Bitmap,
    val duration: Int,
    val provider: MusicBotPlugin
)