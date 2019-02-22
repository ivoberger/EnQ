package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.viewmodel.UserInfoViewModel
import com.ivoberger.jmusicbot.JMusicBot
import kotlinx.android.synthetic.main.fragment_user_info.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.resources.str
import splitties.systemservices.inputMethodManager
import splitties.toast.toast
import splitties.views.onClick
import timber.log.Timber


@ContentView(R.layout.fragment_user_info)
class UserInfoFragment : Fragment() {

    val mMainScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private val mUserViewModel by lazy { ViewModelProviders.of(this).get(UserInfoViewModel::class.java) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mUserViewModel.user?.let {
            txt_username.text = it.name
            if (it.password != null) {
                btn_change_password.text = str(R.string.btn_change_password)
                input_password.hint = str(R.string.hint_new_password)
            }
            btn_change_password.onClick {
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                val input = input_password.editText?.text?.toString()
                if (input.isNullOrBlank()) {
                    toast(R.string.msg_inavlid_password)
                    return@onClick
                }
                val toast = Toast.makeText(context, "", LENGTH_SHORT)
//                toast.setGravity(Gravity.CENTER, 0, 0)
                mBackgroundScope.launch {
                    try {
                        JMusicBot.changePassword(input)
                        Timber.d("Password for user ${it.name} successfully changed")
                        withContext(mMainScope.coroutineContext) {
                            input_password.editText?.text = null
                            toast.setText(R.string.msg_password_changed)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error changing password")
                        withContext(mMainScope.coroutineContext) { toast.setText(R.string.msg_server_error) }
                    }
                    withContext(mMainScope.coroutineContext) { toast.show() }
                }
            }
            btn_reload_permissions.onClick {
                if (it.password != null) {
                    mBackgroundScope.launch {
                        JMusicBot.reloadPermissions()
                        withContext(mMainScope.coroutineContext) { toast("Permissions successfully reloaded") }
                    }
                } else toast("You need to set a password first")
            }
            btn_logout.onClick { logout() }
            btn_delete_user.onClick {
                toast(R.string.msg_function_unsupported)
//                mBackgroundScope.launch {
//                    JMusicBot.deleteUser()
//                    logout()
//                }
            }
        }
    }

    private fun logout() {
        JMusicBot.user = null
        (context as MainActivity).reset()
    }

}
