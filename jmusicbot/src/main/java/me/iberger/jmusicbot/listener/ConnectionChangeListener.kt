package me.iberger.jmusicbot.listener

interface ConnectionChangeListener {

    fun onConnectionLost(e: Exception)
    fun onConnectionRecovered()
}