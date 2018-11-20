package me.iberger.jmusicbot.listener

import me.iberger.jmusicbot.data.PlayerState

interface PlayerUpdateListener : UpdateListener {
    fun onPlayerStateChanged(newState: PlayerState)
}