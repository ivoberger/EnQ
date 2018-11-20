package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Song(
    val id: String,
    val title: String,
    val description: String,
    val albumArtUrl: String?,
    val duration: Int?,
    val provider: MusicBotPlugin
) {
    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Song

        if (id != other.id) return false
        return true
    }
}

@JsonClass(generateAdapter = true)
data class SongEntry(
    val song: Song,
    val userName: String?
)