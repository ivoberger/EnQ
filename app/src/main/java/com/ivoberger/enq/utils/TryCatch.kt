/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.utils

import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.client.exceptions.ServerErrorException
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
    Timber.w(e)
    default
}

suspend fun <T> retryOnError(maxAttempts: Int = 5, delay: Long = 1000, toTry: suspend () -> T): T? {
    var res = tryWithDefault { toTry() }
    var attempts = 1
    while (res == null && attempts <= maxAttempts) {
        kotlinx.coroutines.delay(delay)
        res = tryWithDefault { toTry() }
        attempts++
    }
    return res
}
