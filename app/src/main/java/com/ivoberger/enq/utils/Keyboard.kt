package com.ivoberger.enq.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.systemservices.inputMethodManager


fun View.hideKeyboard() {
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
}

fun AppCompatActivity.hideKeyboard() = lifecycleScope.launch {
    //Find the currently focused view, so we can grab the correct window token from it.
    val view = currentFocus ?: findViewById<View>(android.R.id.content).rootView
    view.hideKeyboard()
}

fun Fragment.hideKeyboard() = view?.rootView?.hideKeyboard()