package me.iberger.jmusicbot.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerState(
    val state: PlayerAction,
    val songEntry: Song,
    val userName: String
)

@JsonClass(generateAdapter = true)
data class PlayerStateChange(val action: PlayerAction)

enum class PlayerAction {
    PLAY, PAUSE, SKIP
}