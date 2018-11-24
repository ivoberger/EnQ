package me.iberger.jmusicbot.data

import com.squareup.moshi.Json

data class MusicBotPlugin(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)