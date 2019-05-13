/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.ui.listener

import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.utils.attributeColor
import com.ivoberger.jmusicbot.listener.ConnectionListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.views.backgroundColor

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class ConnectionListener(private val mainActivity: MainActivity) : ConnectionListener {
    private var connected = false
    override fun onConnectionLost(e: Exception?) {
        mainActivity.lifecycleScope.launch {
            mainActivity.bottom_navigation.setBackgroundResource(R.color.red_500)
        }
        if (!connected) mainActivity.reset()
        connected = false
    }

    override fun onConnectionRecovered() {
        mainActivity.lifecycleScope.launch {
            mainActivity.bottom_navigation.backgroundColor = mainActivity.attributeColor(R.attr.colorPrimary)
        }
        connected = true
    }
}
