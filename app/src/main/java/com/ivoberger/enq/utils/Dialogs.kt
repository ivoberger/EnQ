package com.ivoberger.enq.utils

import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.dialogs.LoginDialog
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.User
import kotlinx.coroutines.*
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber

fun AlertDialog.styleButtons(@ColorInt color: Int) {
    setOnShowListener {
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
    }
}

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
fun MainActivity.showServerDiscoveryDialog(searching: Boolean = false): Job = GlobalScope.launch {
    Timber.d("Showing Server Discovery Dialog, $searching")
    val serverDialogBuilder = AlertDialog.Builder(this@showServerDiscoveryDialog)
        .setCancelable(false)
        .setTitle(R.string.tlt_server_discovery)
    if (!searching) serverDialogBuilder
        .setTitle(R.string.tlt_no_server)
        .setPositiveButton(R.string.btn_retry) { dialog, _ ->
            lifecycleScope.launch { JMusicBot.discoverHost() }
            showServerDiscoveryDialog(true)
            dialog.dismiss()
        }
    if (searching) serverDialogBuilder.setView(R.layout.dialog_progress_spinner)
    withContext(Dispatchers.Main) {
        val serverDialog = serverDialogBuilder.create()
        if (!searching) serverDialog.styleButtons(secondaryColor())
        serverDialog.show()
        if (!searching) return@withContext
        if (withContext(Dispatchers.IO) { delay(2000); JMusicBot.state.hasServer }) {
            this@showServerDiscoveryDialog.continueToLogin()
            serverDialog.dismiss()
        } else {
            showServerDiscoveryDialog(false).join()
            serverDialog.cancel()
        }
    }
}


@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
fun MainActivity.showLoginDialog(
    loggingIn: Boolean = true, user: User? = AppSettings.savedUsers?.first()
) = LoginDialog(loggingIn, user) { continueWithBot() }.show(supportFragmentManager, null)
