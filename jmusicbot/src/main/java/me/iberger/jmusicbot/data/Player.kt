package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerState(
    val state: PlayerStates,
    val songEntry: SongEntry?
)

@JsonClass(generateAdapter = true)
data class PlayerStateChange(val action: PlayerAction)

enum class PlayerStates {
    PLAY, PAUSE, STOP, ERROR
}

enum class PlayerAction {
    PLAY, PAUSE, SKIP
}