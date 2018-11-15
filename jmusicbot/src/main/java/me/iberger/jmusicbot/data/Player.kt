package me.iberger.jmusicbot.data

data class PlayerState(
    val state: PlayerAction,
    val songEntry: Song,
    val userName: String
)

data class PlayerStateChange(val action: PlayerAction)

enum class PlayerAction {
    PLAY, PAUSE, SKIP
}