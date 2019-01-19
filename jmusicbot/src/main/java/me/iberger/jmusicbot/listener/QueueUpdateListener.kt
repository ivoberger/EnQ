package me.iberger.jmusicbot.listener

import me.iberger.jmusicbot.model.QueueEntry

interface QueueUpdateListener : UpdateListener {
    fun onQueueChanged(newQueue: List<QueueEntry>)
}
