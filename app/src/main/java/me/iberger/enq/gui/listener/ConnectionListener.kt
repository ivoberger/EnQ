package me.iberger.enq.gui.listener

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import timber.log.Timber

class ConnectionListener(private val mainActivity: MainActivity) : ConnectionChangeListener {
    override fun onConnectionLost(e: Exception) {
        Timber.e(e)
        MainActivity.connected = false
        GlobalScope.launch(Dispatchers.Main) {
            mainActivity.main_bottom_navigation.setBackgroundResource(R.color.md_red_500)
        }
    }

    override fun onConnectionRecovered() {
        MainActivity.connected = true
        GlobalScope.launch(Dispatchers.Main) {
            mainActivity.main_bottom_navigation.setBackgroundResource(R.color.background)
        }
    }
}