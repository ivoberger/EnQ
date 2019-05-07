package com.ivoberger.enq.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.model.User
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.toast.longToast
import splitties.toast.toast
import timber.log.Timber

class LoginDialog(startLoggingIn: Boolean = true, private var user: User? = null, private val onLoggedIn: () -> Unit) :
    DialogFragment() {
    private var loggingIn = startLoggingIn

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = context?.alertDialog {
            isCancelable = false
            setView(R.layout.dialog_login)
            if (loggingIn) MainScope().launch { attemptLogin() }
            else showLoginMask()
        }
        return dialogBuilder ?: throw IllegalStateException("Context cannot be null")
    }

    private fun showLoginMask() {

    }

    private suspend fun attemptLogin() = try {
        // ui setup
        dialog?.apply {
            setTitle(R.string.tlt_logging_in)
            view?.apply {

            }
        }
        // actual login
        JMusicBot.authorize(user?.name, user?.password)
        context?.toast(getString(R.string.msg_logged_in, JMusicBot.user!!.name))
        onLoggedIn()
    } catch (e: UsernameTakenException) {
        Timber.w(e)
        context?.toast(R.string.msg_username_taken)
        showLoginMask()
    } catch (e: AuthException) {
        Timber.w("Authentication error with reason ${e.reason}")
        Timber.w(e)
        context?.longToast(R.string.msg_password_wrong)
        showLoginMask()
    } catch (e: IllegalStateException) {
        Timber.w(e)
        showLoginMask()
    } catch (e: InvalidParametersException) {
        Timber.w(e)
        context?.longToast(R.string.msg_password_wrong)
        showLoginMask()
    } catch (e: ServerErrorException) {
        Timber.e(e)
        context?.longToast(R.string.msg_server_error)
        showLoginMask()
    } catch (e: Exception) {
        Timber.e(e)
        context?.longToast(R.string.msg_unknown_error)
        showLoginMask()
    }
}
