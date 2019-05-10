package com.ivoberger.enq.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.utils.hideKeyboard
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.exceptions.InvalidParametersException
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import com.ivoberger.jmusicbot.exceptions.UsernameTakenException
import com.ivoberger.jmusicbot.model.User
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
class LoginDialog : DialogFragment() {

    private var loggingIn: Boolean = true
    private var user: User? = AppSettings.getLatestUser()
    private lateinit var mLoginMaskViews: List<View?>
    private lateinit var mLoginProgressViews: List<View?>
    private var mUserNameInput: TextView? = null
    private var mPasswordInput: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val arguments: LoginDialogArgs by navArgs()
        loggingIn = arguments.startLoggingIn
        super.onCreate(savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Timber.d("Showing login dialog")
        val dialog = context?.alertDialog {
            titleResource = R.string.tlt_login
            isCancelable = false
            setView(R.layout.dialog_login)
            positiveButton(R.string.btn_login) {}
        }
        dialog?.onShow {
            positiveButton.onClick {
                user = User(mUserNameInput?.text.toString(), mPasswordInput?.text.toString())
                attemptLogin()
            }
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

            if (loggingIn && user != null) attemptLogin()
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

    private fun attemptLogin() = lifecycleScope.launch {
        try {
            // ui setup
            hideKeyboard()
            dialog?.apply {
                setTitle(R.string.tlt_logging_in)
                mLoginMaskViews.forEach { it?.visibility = View.GONE }
                mLoginProgressViews.forEach { it?.visibility = View.VISIBLE }
            }
            // actual login
            JMusicBot.authorize(user)
            context?.toast(getString(R.string.msg_logged_in, JMusicBot.user!!.name))
            val mainActivity = activity as MainActivity
            mainActivity.continueWithBot()
            dismiss()
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
