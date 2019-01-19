package me.iberger.jmusicbot.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QueueEntry(
    @Json(name = "song") val song: Song,
    @Json(name = "userName") val userName: String
)