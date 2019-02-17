package com.ivoberger.jmusicbot.listener

import com.ivoberger.jmusicbot.model.PlayerState

interface PlayerUpdateListener : UpdateListener {
    fun onPlayerStateChanged(newState: PlayerState)
}
