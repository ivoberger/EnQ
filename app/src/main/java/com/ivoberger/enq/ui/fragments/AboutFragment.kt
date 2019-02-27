package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import com.ivoberger.enq.BuildConfig
import com.ivoberger.enq.R
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


@ContentView(R.layout.fragment_about)
class AboutFragment : Fragment() {

    val mMainScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        aboutVersion.text = BuildConfig.VERSION_NAME
        txt_about_description.movementMethod = LinkMovementMethod.getInstance()
    }
}
