package com.ivoberger.jmusicbot.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerState(
    @Json(name = "state") val state: PlayerStates,
    @Json(name = "songEntry") val songEntry: SongEntry?,
    @Json(name = "progress") val progress: Int = 0
) {
    constructor(state: PlayerStates) : this(state, null)
}

@JsonClass(generateAdapter = true)
data class PlayerStateChange(
    @Json(name = "action") val action: PlayerAction
)

enum class PlayerStates {
    PLAY, PAUSE, STOP, ERROR
}

enum class PlayerAction {
    PLAY, PAUSE, SKIP
}
