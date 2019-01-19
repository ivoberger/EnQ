package me.iberger.jmusicbot.listener

import me.iberger.jmusicbot.model.PlayerState

interface PlayerUpdateListener : UpdateListener {
    fun onPlayerStateChanged(newState: PlayerState)
}
