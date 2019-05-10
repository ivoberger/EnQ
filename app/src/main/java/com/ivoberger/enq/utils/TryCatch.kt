package com.ivoberger.enq.utils

import android.content.Context
import androidx.fragment.app.Fragment
import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import splitties.toast.toast
import timber.log.Timber

inline fun <T> Context.tryWithErrorToast(default: T? = null, toTry: () -> T): T? = try {
    toTry()
} catch (e: ServerErrorException) {
    Timber.w(e)
    MainScope().launch { toast(R.string.msg_server_error) }
    default
} catch (e: Exception) {
    Timber.w(e)
    MainScope().launch { toast(R.string.msg_unknown_error) }
    default
}

inline fun <T> Fragment.tryWithErrorToast(default: T? = null, toTry: () -> T): T? =
    context?.tryWithErrorToast(default, toTry)