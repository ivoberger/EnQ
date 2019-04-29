package com.ivoberger.enq.utils

import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.exceptions.UsernameTakenException
import kotlinx.coroutines.*
import timber.log.Timber

private fun AlertDialog.styleButtons(@ColorInt color: Int) {
    setOnShowListener {
        getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
    }
}

fun MainActivity.showServerDiscoveryDialog(searching: Boolean = false): Job = GlobalScope.launch {
    Timber.d("Showing Server Discovery Dialog, $searching")
    val serverDialogBuilder = AlertDialog.Builder(this@showServerDiscoveryDialog)
        .setCancelable(false)
        .setTitle(R.string.tlt_server_discovery)
    if (!searching) serverDialogBuilder
        .setTitle(R.string.tlt_no_server)
        .setPositiveButton(R.string.btn_retry) { dialog, _ ->
            JMusicBot.discoverHost()
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


fun MainActivity.showLoginDialog(
    loggingIn: Boolean = true,
    userName: String? = null,
    password: String? = null
): Job = GlobalScope.launch {
    Timber.d("Showing Basic Dialog for user $userName, Logging in: $loggingIn")
    val loginDialogBuilder = AlertDialog.Builder(this@showLoginDialog)
        .setCancelable(false)
        .setTitle(R.string.tlt_logging_in)
    if (!loggingIn) {
        val dialogView =
            withContext(Dispatchers.Main) { this@showLoginDialog.layoutInflater.inflate(R.layout.dialog_login, null) }
        withContext(Dispatchers.Main) { dialogView.findViewById<EditText>(R.id.login_username)?.setText(userName) }
        loginDialogBuilder
            .setView(dialogView)
            .setTitle(R.string.tlt_login)
            .setPositiveButton(R.string.btn_login) { dialog, _ ->
                val userNameInput =
                    dialogView.findViewById<EditText>(R.id.login_username)?.text.toString()
                val passwordInput =
                    dialogView.findViewById<EditText>(R.id.login_password)?.text.toString()
                showLoginDialog(true, userNameInput, passwordInput)
                dialog.dismiss()
            }
    }
    if (loggingIn) loginDialogBuilder.setView(R.layout.dialog_progress_spinner)
    withContext(Dispatchers.Main) {
        val loginDialog = loginDialogBuilder.create()
        if (!loggingIn) loginDialog.styleButtons(secondaryColor())
        loginDialog.show()
        if (!loggingIn) return@withContext
        try {
            withContext(Dispatchers.Default) { JMusicBot.authorize(userName, password) }
            continueWithBot()
            this@showLoginDialog.toastShort(getString(R.string.msg_logged_in, JMusicBot.user!!.name))
            loginDialog.dismiss()
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            Toast.makeText(
                this@showLoginDialog,
                R.string.msg_username_taken,
                Toast.LENGTH_LONG
            ).show()
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        } catch (e: AuthException) {
            Timber.w("Authentication error with reason ${e.reason}")
            Timber.w(e)
            this@showLoginDialog.toastLong(R.string.msg_password_wrong)
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        } catch (e: IllegalStateException) {
            Timber.w(e)
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        } catch (e: InvalidParametersException) {
            Timber.w(e)
            this@showLoginDialog.toastLong(R.string.msg_password_wrong)
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        } catch (e: ServerErrorException) {
            Timber.e(e)
            this@showLoginDialog.toastLong(R.string.msg_server_error)
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        } catch (e: Exception) {
            Timber.e(e)
            this@showLoginDialog.toastLong(R.string.msg_unknown_error)
            showLoginDialog(false, userName, password)
            loginDialog.cancel()
        }
    }
}
