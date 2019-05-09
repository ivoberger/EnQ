package com.ivoberger.enq.utils

import com.ivoberger.enq.model.ServerInfo
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.dialogs.LoginDialog
import com.ivoberger.enq.ui.dialogs.ServerDiscoveryDialog
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.User
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
fun MainActivity.showServerDiscoveryDialog(searching: Boolean = false) =
    ServerDiscoveryDialog(searching) {
        lifecycleScope.launch {
            try {
                val serverInfo = ServerInfo(JMusicBot.baseUrl!!, JMusicBot.getVersionInfo())
                AppSettings.addServer(serverInfo)
                Timber.d("Added server $serverInfo")
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
        showLoginDialog()
    }.show(supportFragmentManager, null)

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
fun MainActivity.showLoginDialog(
    loggingIn: Boolean = true, user: User? = AppSettings.getLatestUser()
) = LoginDialog(loggingIn, user) { continueWithBot() }.show(supportFragmentManager, null)
