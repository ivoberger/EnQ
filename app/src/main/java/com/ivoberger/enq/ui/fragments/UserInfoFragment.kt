package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.viewmodel.UserInfoViewModel

@ContentView(R.layout.fragment_user_info)
class UserInfoFragment : Fragment() {

    companion object {
        fun newInstance() = UserInfoFragment()
    }

    private val viewModel: UserInfoViewModel by lazy { ViewModelProviders.of(this).get(UserInfoViewModel::class.java) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

}
