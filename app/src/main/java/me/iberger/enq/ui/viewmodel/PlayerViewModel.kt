package me.iberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.model.PlayerState

class PlayerViewModel : ViewModel() {
    val playerState: LiveData<PlayerState> = JMusicBot.getPlayerState()
}
