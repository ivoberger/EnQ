package me.iberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.model.PlayerState
import me.iberger.jmusicbot.model.QueueEntry
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
