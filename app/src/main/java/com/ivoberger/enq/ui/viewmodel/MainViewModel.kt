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
package com.ivoberger.enq.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.utils.retryOnError
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.android.playerStateLiveData
import com.ivoberger.jmusicbot.client.android.queueLiveData
import com.ivoberger.jmusicbot.client.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.mercari.remotedata.RemoteData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel : ViewModel(), ConnectionChangeListener {

    var playerCollapsed = false
    var bottomNavCollapsed = false

    val playerState: LiveData<RemoteData<PlayerState, Exception>> by lazy { JMusicBot.playerStateLiveData }
    val queue: LiveData<RemoteData<List<QueueEntry>, Exception>> by lazy { JMusicBot.queueLiveData }
    var mainActivityConnectionListener: ConnectionChangeListener? = null

    init {
        Timber.d("MainViewModel initialized")
    }

    override fun onConnectionLost(e: Exception?) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.w(e, "Lost Connection")
            JMusicBot.stopPlayerUpdates()
            JMusicBot.stopQueueUpdates()
            retryOnError {
                JMusicBot.discoverHost(AppSettings.getLatestServer()?.baseUrl)
                AppSettings.getLatestUser()?.let {
                    JMusicBot.authorize(it) // TODO: add auth token
                }
            }
            mainActivityConnectionListener?.onConnectionLost()
        }
    }

    override fun onConnectionRecovered() {
        Timber.d("Recovered Connection")
        viewModelScope.launch(Dispatchers.IO) {
            JMusicBot.startPlayerUpdates()
            JMusicBot.startQueueUpdates()
        }
    }

    override fun onCleared() {
        super.onCleared()
        JMusicBot.connectionListeners.remove(this)
    }
}
