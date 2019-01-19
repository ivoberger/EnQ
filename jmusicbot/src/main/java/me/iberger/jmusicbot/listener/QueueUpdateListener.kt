package me.iberger.jmusicbot.listener

import me.iberger.jmusicbot.data.QueueEntry

interface QueueUpdateListener : UpdateListener {
    fun onQueueChanged(newQueue: List<QueueEntry>)
}