package com.ivoberger.enq.utils

import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.dialogs.LoginDialog
import com.ivoberger.enq.ui.dialogs.ServerDiscoveryDialog
import com.ivoberger.jmusicbot.model.User
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
fun MainActivity.showServerDiscoveryDialog(searching: Boolean = false) =
    ServerDiscoveryDialog(searching) { showLoginDialog() }.show(supportFragmentManager, null)

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
fun MainActivity.showLoginDialog(
    loggingIn: Boolean = true, user: User? = AppSettings.savedUsers?.first()
) = LoginDialog(loggingIn, user) { continueWithBot() }.show(supportFragmentManager, null)
