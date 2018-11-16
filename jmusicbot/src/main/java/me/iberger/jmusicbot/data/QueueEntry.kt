package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QueueEntry(
    val song: Song,
    val userName: String
)