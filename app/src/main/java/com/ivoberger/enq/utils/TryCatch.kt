package com.ivoberger.enq.utils

import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.exceptions.ServerErrorException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import splitties.toast.toast
import timber.log.Timber

inline fun <T> tryWithErrorToast(default: T? = null, toTry: () -> T): T? = try {
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

inline fun <T> tryWithDefault(default: T? = null, toTry: () -> T) = try {
    toTry()
} catch (e: Exception) {
    default
}

inline fun <T> retryOnError(maxAttempts: Int = 5, toTry: () -> T): T? {
    var res = tryWithDefault { toTry() }
    var attempts = 1
    while (res == null && attempts <= maxAttempts) {
        res = tryWithDefault { toTry() }
        attempts++
    }
    return res
}