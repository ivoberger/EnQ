package com.ivoberger.enq.ui.listener

import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.utils.attributeColor
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.views.backgroundColor

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class ConnectionListener(private val mainActivity: MainActivity) : ConnectionChangeListener {
    override fun onConnectionLost(e: Exception?) {
        mainActivity.lifecycleScope.launch {
            mainActivity.bottom_navigation.setBackgroundResource(R.color.red_500)
        }
    }

    override fun onConnectionRecovered() {
        mainActivity.lifecycleScope.launch {
            mainActivity.bottom_navigation.backgroundColor = mainActivity.attributeColor(R.attr.colorPrimary)
        }
    }
}
