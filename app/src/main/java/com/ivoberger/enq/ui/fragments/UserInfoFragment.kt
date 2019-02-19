package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
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
import splitties.toast.toast
import splitties.views.onClick

@ContentView(R.layout.fragment_user_info)
class UserInfoFragment : Fragment() {

    val mMainScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private val mUserViewModel by lazy { ViewModelProviders.of(this).get(UserInfoViewModel::class.java) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mUserViewModel.user?.let {
            txt_username.text = str(R.string.txt_username, it.name)
            if (it.password != null) {
                btn_change_password.text = str(R.string.btn_change_password)
                input_password.hint = str(R.string.hint_new_password)
            }
            btn_change_password.onClick { toast("This could be your password change!") }
            btn_reload_permissions.onClick {
                if (it.password != null) {
                    mBackgroundScope.launch {
                        JMusicBot.reloadPermissions()
                        withContext(mMainScope.coroutineContext) { toast("Permissions successfully reloaded") }
                    }
                } else toast("You need to set a password first")
            }
            btn_logout.onClick {
                JMusicBot.user = null
                (context as MainActivity).reset()
            }
            btn_delete_user.onClick {
                mBackgroundScope.launch {
                    JMusicBot.deleteUser()
                    withContext(mMainScope.coroutineContext) { btn_logout.callOnClick() }
                }
            }
        }
    }

}
