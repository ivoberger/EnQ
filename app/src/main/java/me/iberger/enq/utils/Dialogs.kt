package me.iberger.enq.utils

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.exceptions.UsernameTakenException
import timber.log.Timber

private fun styleButtons(context: Context, alertDialog: AlertDialog, @ColorRes colorResource: Int) {
    alertDialog.setOnShowListener {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(context, colorResource))
    }
}

fun showServerDiscoveryDialog(
    activity: AppCompatActivity, searching: Boolean = false
): Job = GlobalScope.launch {
    Timber.d("Showing Server Discovery Dialog, $searching")
    val serverDialogBuilder = AlertDialog.Builder(activity)
        .setCancelable(false)
        .setTitle(R.string.tlt_server_discovery)
    if (!searching) serverDialogBuilder
        .setTitle(R.string.tlt_no_server)
        .setPositiveButton(R.string.btn_retry) { dialog, _ ->
            showServerDiscoveryDialog(activity, true)
            dialog.dismiss()
        }
    if (searching) serverDialogBuilder.setView(R.layout.dialog_progress_spinner)
    withContext(Dispatchers.Main) {
        val serverDialog = serverDialogBuilder.create()
        if (!searching) styleButtons(activity, serverDialog, R.color.colorAccent)
        serverDialog.show()
        if (!searching) return@withContext
        if (async(Dispatchers.IO) { MusicBot.hasServer(activity.applicationContext) }.await()) {
            (activity as MainActivity).continueWithLogin()
            serverDialog.dismiss()
        } else {
            showServerDiscoveryDialog(activity, false).join()
            serverDialog.cancel()
        }
    }
}


fun showLoginDialog(
    activity: AppCompatActivity,
    loggingIn: Boolean,
    userName: String? = null,
    password: String? = null
): Job = GlobalScope.launch {
    Timber.d("Showing Login Dialog for user $userName, Logging in: $loggingIn")
    val loginDialogBuilder = AlertDialog.Builder(activity)
        .setCancelable(false)
        .setTitle(R.string.tlt_logging_in)
    if (!loggingIn) {
        val dialogView =
            async(Dispatchers.Main) { activity.layoutInflater.inflate(R.layout.dialog_login, null) }.await()
        loginDialogBuilder
            .setView(dialogView)
            .setTitle(R.string.tlt_login)
            .setPositiveButton(R.string.btn_login) { dialog, _ ->
                val userNameInput = dialogView.findViewById<EditText>(R.id.login_username)?.text.toString()
                val passwordInput = dialogView.findViewById<EditText>(R.id.login_password)?.text.toString()
                showLoginDialog(activity, true, userNameInput, passwordInput)
                dialog.dismiss()
            }
    }
    if (loggingIn) loginDialogBuilder.setView(R.layout.dialog_progress_spinner)
    withContext(Dispatchers.Main) {
        val loginDialog = loginDialogBuilder.create()
        if (!loggingIn) styleButtons(activity, loginDialog, R.color.colorAccent)
        loginDialog.show()
        if (!loggingIn) return@withContext
        try {
            val musicBot = MusicBot.init(activity, userName, password).await()
            (activity as MainActivity).continueWithBot()
            Toast.makeText(activity, activity.getString(R.string.msg_logged_in, musicBot.user.name), Toast.LENGTH_SHORT)
                .show()
            loginDialog.dismiss()
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            Toast.makeText(activity, activity.getString(R.string.msg_username_taken), Toast.LENGTH_LONG).show()
            showLoginDialog(activity, false)
            loginDialog.cancel()
        } catch (e: AuthException) {
            Timber.w("Authentication error with reason ${e.reason}")
            Timber.w(e)
            Toast.makeText(activity, activity.getString(R.string.msg_password_wrong), Toast.LENGTH_LONG).show()
            showLoginDialog(activity, false)
            loginDialog.cancel()
        }
    }
}