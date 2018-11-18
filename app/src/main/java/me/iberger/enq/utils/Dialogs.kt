package me.iberger.enq.utils

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.MusicBot
import timber.log.Timber

private fun styleButtons(context: Context, alertDialog: AlertDialog, colorResource: Int) {
    alertDialog.setOnShowListener {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
    }
}

fun showServerDiscoveryDialog(
    activity: AppCompatActivity, coroutineScope: CoroutineScope, searching: Boolean = false
): Job = coroutineScope.launch {
    Timber.d("Showing Dialog, $searching")
    val serverDialogBuilder = AlertDialog.Builder(activity)
        .setCancelable(false)
        .setTitle(R.string.tlt_server_discovery)
    if (!searching) serverDialogBuilder
        .setTitle(R.string.tlt_no_server)
        .setPositiveButton(R.string.btn_retry) { dialog, _ ->
            showServerDiscoveryDialog(activity, coroutineScope, true)
            dialog.dismiss()
        }
    if (searching) serverDialogBuilder.setView(R.layout.dialog_no_server)
    withContext(Dispatchers.Main) {
        val serverDialog = serverDialogBuilder.create()
        if (!searching) styleButtons(activity, serverDialog, R.color.colorAccent)
        serverDialog.show()
        if (!searching) return@withContext
        if (MusicBot.hasServer(activity).await()) {
            Toast.makeText(activity, R.string.msg_server_found, Toast.LENGTH_LONG).show()
            (activity as MainActivity).continueWithLogin()
            serverDialog.dismiss()
        } else {
            showServerDiscoveryDialog(activity, coroutineScope, false).join()
            serverDialog.cancel()
        }
    }
}