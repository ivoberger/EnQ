package me.iberger.jmusicbot.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerState(
    @Json(name = "state") val state: PlayerStates,
    @Json(name = "songEntry") val songEntry: SongEntry?
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
