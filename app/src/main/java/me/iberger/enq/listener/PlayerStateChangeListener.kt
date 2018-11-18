package me.iberger.enq.listener

import me.iberger.jmusicbot.data.PlayerState

interface PlayerStateChangeListener {
    fun onPlayerStateChanged(newState: PlayerState)
}