package me.iberger.enq.utils

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.MusicBot
import timber.log.Timber

fun showServerNotFoundDialog(
    activity: AppCompatActivity,
    coroutineScope: CoroutineScope,
    searching: Boolean = false
): Job =
    coroutineScope.launch {
        Timber.d("Showing Dialog, $searching")
        val serverDialogBuilder = AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle(R.string.tlt_server_discovery)
        if (!searching) serverDialogBuilder
            .setTitle(R.string.tlt_no_server)
            .setPositiveButton(R.string.btn_retry) { dialog, _ ->
                showServerNotFoundDialog(activity, coroutineScope, true)
                dialog.dismiss()
            }
        if (searching) serverDialogBuilder.setView(R.layout.dialog_no_server)
        val serverDialog = serverDialogBuilder.create()
        if (!searching) serverDialog.setOnShowListener {
            serverDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(activity, R.color.colorAccent))
        }
        serverDialog.show()
        if (!searching) return@launch
        if (MusicBot.hasServer(activity).await()) {
            serverDialog.dismiss()
            Toast.makeText(activity, R.string.msg_server_found, Toast.LENGTH_LONG).show()
            (activity as MainActivity).continueWithLogin()
        } else {
            serverDialog.cancel()
            showServerNotFoundDialog(activity, coroutineScope, false)
        }
    }