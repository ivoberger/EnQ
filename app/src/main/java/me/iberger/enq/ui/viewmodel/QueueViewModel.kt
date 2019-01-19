package me.iberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.QueueEntry

class QueueViewModel : ViewModel() {
    val queue: LiveData<List<QueueEntry>> = MusicBot.instance!!.getQueue()
}
