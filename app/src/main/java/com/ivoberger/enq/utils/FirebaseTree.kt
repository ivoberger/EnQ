package com.ivoberger.enq.utils

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
