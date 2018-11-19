package me.iberger.jmusicbot.listener

import me.iberger.jmusicbot.data.QueueEntry

interface QueueChangeListener {
    fun onQueueChanged(newQueue: List<QueueEntry>)
}