package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Song(
    val id: String,
    val title: String,
    val description: String,
    val albumArtUrl: String,
    val duration: Int,
    val provider: MusicBotPlugin
)

@JsonClass(generateAdapter = true)
data class SongEntry(
    val song: Song,
    val userName: String?
)