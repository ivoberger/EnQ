package com.ivoberger.jmusicbot.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MusicBotPlugin(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)
