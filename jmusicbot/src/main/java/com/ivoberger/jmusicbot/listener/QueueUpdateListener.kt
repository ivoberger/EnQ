package com.ivoberger.jmusicbot.listener

import com.ivoberger.jmusicbot.model.QueueEntry

interface QueueUpdateListener : UpdateListener {
    fun onQueueChanged(newQueue: List<QueueEntry>)
}
