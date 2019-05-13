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
package com.ivoberger.enq.logging

import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.ivoberger.enq.R
import io.fabric.sdk.android.Fabric
import splitties.resources.appStr
import timber.log.Timber

/**
 * {@link Timber} tree to log to firebase crashlytics
 */
class FirebaseTree(
    private val context: Context,
    private val minCapturePriority: Int = Log.ERROR,
    private val minLogPriority: Int = Log.DEBUG
) : Timber.DebugTree() {

    init {
        Timber.d("Initializing Crashlytics")
        // make sure crashlytics is initialized
        Fabric.with(context.applicationContext, Crashlytics())
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        val defaultTag = super.createStackElementTag(element)
        // add apps name to tag for better filtering
        return "${appStr(R.string.app_name)}/$defaultTag"
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= minLogPriority

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Crashlytics.log(priority, tag, message)
        t?.let { Crashlytics.logException(it) }
    }
}
