package me.iberger.enq.utils

import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.listener.PlayerUpdateListener
import me.iberger.jmusicbot.listener.QueueUpdateListener

fun <T> T.startUpdates() where T : PlayerUpdateListener, T : ConnectionChangeListener {
    MusicBot.instance?.startPlayerUpdates(this)
    MusicBot.instance?.connectionChangeListeners?.remove(this)
}

fun <T> T.stopUpdates() where T : PlayerUpdateListener, T : ConnectionChangeListener {
    MusicBot.instance?.stopPlayerUpdates(this)
    MusicBot.instance?.connectionChangeListeners?.remove(this)
}

fun <T> T.startUpdates() where T : QueueUpdateListener, T : ConnectionChangeListener {
    MusicBot.instance?.startQueueUpdates(this)
    MusicBot.instance?.connectionChangeListeners?.remove(this)
}

fun <T> T.stopUpdates() where T : QueueUpdateListener, T : ConnectionChangeListener {
    MusicBot.instance?.stopQueueUpdates(this)
    MusicBot.instance?.connectionChangeListeners?.remove(this)
}
