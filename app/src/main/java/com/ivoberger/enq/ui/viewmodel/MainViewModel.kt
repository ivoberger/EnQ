package com.ivoberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.QueueEntry
import timber.log.Timber

class MainViewModel : ViewModel(), ConnectionChangeListener {

    override fun onConnectionLost(e: Exception) {
        Timber.w(e)
        connected = false
    }

    override fun onConnectionRecovered() {
        connected = true
    }

    var connected = false
    var playerCollapsed = false
    var bottomNavCollapsed = false


    val playerState: LiveData<PlayerState> by lazy { JMusicBot.getPlayerState() }
    val queue: LiveData<List<QueueEntry>>by lazy { JMusicBot.getQueue() }
}
