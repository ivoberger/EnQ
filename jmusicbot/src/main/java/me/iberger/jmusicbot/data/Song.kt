package me.iberger.jmusicbot.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Song(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "albumArtUrl") val albumArtUrl: String?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "provider") val provider: MusicBotPlugin
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
    @Json(name = "song") val song: Song,
    @Json(name = "userName") val userName: String?
)