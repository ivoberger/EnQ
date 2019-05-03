package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.jmusicbot.JMusicBot
import kotlinx.android.synthetic.main.fragment_user_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.resources.str
import splitties.systemservices.inputMethodManager
import splitties.toast.toast
import splitties.views.onClick
import timber.log.Timber


@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class UserInfoFragment : Fragment(R.layout.fragment_user_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        JMusicBot.user?.let {
            txt_username.text = it.name
            if (it.password != null) {
                btn_change_password.text = str(R.string.btn_change_password)
                input_password.hint = str(R.string.hint_new_password)
            }
            txt_permissions.text = it.permissions.toString()
            btn_change_password.onClick {
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                val input = input_password.editText?.text?.toString()
                if (input.isNullOrBlank()) {
                    toast(R.string.msg_inavlid_password)
                    return@onClick
                }
                val toast = Toast.makeText(context, "", LENGTH_SHORT)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        JMusicBot.changePassword(input)
                        Timber.d("Password for user ${it.name} successfully changed")
                        withContext(Dispatchers.Main) {
                            input_password.editText?.text = null
                            toast.setText(R.string.msg_password_changed)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error changing password")
                        withContext(Dispatchers.Main) { toast.setText(R.string.msg_error_password_change) }
                    }
                    withContext(Dispatchers.Main) { toast.show() }
                }
            }
            btn_reload_permissions.onClick {
                if (it.password != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        JMusicBot.reloadPermissions()
                        withContext(Dispatchers.Main) {
                            toast(R.string.msg_permissions_reloaded)
                            txt_permissions.text = JMusicBot.user?.permissions.toString()
                        }

                    }
                } else toast(R.string.msg_set_password_needed)
            }
            btn_logout.onClick { logout() }
            btn_delete_user.onClick {
                //                toast(R.string.msg_function_unsupported)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        JMusicBot.deleteUser()
                        logout()
                    } catch (e: Exception) {
                        Timber.e(e, "Deletion failed")
                        withContext(Dispatchers.Main) { toast(R.string.msg_server_error) }
                    }
                }
            }
        }
    }

    private fun logout() {
        JMusicBot.user = null
        (context as MainActivity).reset()
    }

}
