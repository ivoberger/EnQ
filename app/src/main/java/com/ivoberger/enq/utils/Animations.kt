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

import android.view.animation.Animation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class KotlinAnimationListener : Animation.AnimationListener {
    private var _onAnimationRepeat: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationEnd: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationStart: ((animation: Animation?) -> Unit)? = null

    override fun onAnimationRepeat(animation: Animation?) {
        _onAnimationRepeat?.invoke(animation)
    }

    fun onAnimationRepeat(func: (animation: Animation?) -> Unit) {
        _onAnimationRepeat = func
    }

    override fun onAnimationEnd(animation: Animation?) {
        _onAnimationEnd?.invoke(animation)
    }

    fun onAnimationEnd(func: (animation: Animation?) -> Unit) {
        _onAnimationEnd = func
    }

    override fun onAnimationStart(animation: Animation?) {
        _onAnimationStart?.invoke(animation)
    }

    fun onAnimationStart(func: (animation: Animation?) -> Unit) {
        _onAnimationStart = func
    }
}

inline fun Animation.setAnimationListener(func: KotlinAnimationListener.() -> Unit) {
    val listener = KotlinAnimationListener()
    listener.func()
    setAnimationListener(listener)
}

suspend inline fun Animation?.awaitEnd() = suspendCoroutine<Unit?> { continuation ->
    if (this != null)
        if (this.hasEnded()) continuation.resume(null)
        else setAnimationListener { onAnimationEnd { continuation.resume(null) } }
    else continuation.resume(null)
}
