package com.ivoberger.enq.ui.listener

import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectionListener(private val mainActivity: MainActivity) : ConnectionChangeListener {
    override fun onConnectionLost(e: Exception?) {
        mainActivity.mainScope.launch(Dispatchers.Main) {
            mainActivity.main_bottom_navigation.setBackgroundResource(R.color.red_500)
        }
    }

    override fun onConnectionRecovered() {
        mainActivity.mainScope.launch(Dispatchers.Main) {
            mainActivity.main_bottom_navigation.setBackgroundResource(R.color.background)
        }
    }
}
