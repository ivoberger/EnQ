package com.ivoberger.enq.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.UiThread

@UiThread
fun Context.toastShort(@StringRes text: Int) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

@UiThread
fun Context.toastShort(@StringRes text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

@UiThread
fun Context.toastLong(@StringRes text: Int) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

@UiThread
fun Context.toastLong(@StringRes text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
