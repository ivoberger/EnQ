package com.ivoberger.jmusicbot.listener

interface ConnectionChangeListener {

    fun onConnectionLost(e: Exception? = null)
    fun onConnectionRecovered()
}
