package com.ivoberger.enq.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.ivoberger.enq.R
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.model.User
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.onShow
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.toast.longToast
import splitties.toast.toast
import splitties.views.onClick
import timber.log.Timber

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class LoginDialog(
    startLoggingIn: Boolean,
    private var user: User?,
    private val onLoggedIn: () -> Unit
) :
    DialogFragment() {
    private var loggingIn = startLoggingIn
    private lateinit var mLoginMaskViews: List<View?>
    private lateinit var mLoginProgressViews: List<View?>
    private var mUserNameInput: TextView? = null
    private var mPasswordInput: TextView? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("Showing login dialog")
        val dialog = context?.alertDialog {
            titleResource = R.string.tlt_login
            isCancelable = false
            setView(R.layout.dialog_login)
            positiveButton(R.string.btn_login) {}
        }
        dialog?.onShow {
            positiveButton.onClick { attemptLogin() }
            positiveButton.setTextColor(context.secondaryColor())

            mUserNameInput = findViewById(R.id.login_username)
            mPasswordInput = findViewById(R.id.login_password)
            mLoginMaskViews = listOf(
                mUserNameInput,
                findViewById(R.id.login_lbl_username),
                mPasswordInput,
                findViewById(R.id.login_lbl_password),
                findViewById(R.id.login_message)
            )
            mLoginProgressViews = listOf(findViewById(R.id.login_progress))

            if (loggingIn && user != null) activity?.lifecycleScope?.launch { attemptLogin() }
            else showLoginMask()
        }

        return dialog ?: throw IllegalStateException("Context cannot be null")
    }

    private fun showLoginMask() {
        dialog?.apply {
            setTitle(R.string.tlt_login)
            mLoginMaskViews.forEach { it?.visibility = View.VISIBLE }
            mLoginProgressViews.forEach { it?.visibility = View.GONE }
            mUserNameInput?.text = user?.name
        }
    }

    private fun attemptLogin() = MainScope().launch {
        try {
            // ui setup
            dialog?.apply {
                setTitle(R.string.tlt_logging_in)
                mLoginMaskViews.forEach { it?.visibility = View.GONE }
                mLoginProgressViews.forEach { it?.visibility = View.VISIBLE }
            }
            // actual login
            user = User(mUserNameInput?.text.toString(), mPasswordInput?.text.toString())
            JMusicBot.authorize(user)
            context?.toast(getString(R.string.msg_logged_in, JMusicBot.user!!.name))
            onLoggedIn()
            Timber.d("Dismissing Dialog")
            dialog?.dismiss()
        } catch (e: UsernameTakenException) {
            Timber.w(e)
            context?.toast(R.string.msg_username_taken)
            showLoginMask()
        } catch (e: AuthException) {
            Timber.w("Authentication error with reason ${e.reason}")
            context?.longToast(R.string.msg_password_wrong)
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
}
