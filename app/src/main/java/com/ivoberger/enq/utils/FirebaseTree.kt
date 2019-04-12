package com.ivoberger.enq.utils

import android.util.Log
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import splitties.init.appCtx
import timber.log.Timber

class FirebaseTree(
    private val minCapturePriority: Int = Log.ERROR,
    private val minLogPriority: Int = Log.DEBUG
) : Timber.DebugTree() {

    init {
        Fabric.with(appCtx, Crashlytics())
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        val defaultTag = super.createStackElementTag(element)
        return "EnQ/$defaultTag"
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= minLogPriority

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Crashlytics.log(priority, tag, message)
        t?.let { Crashlytics.logException(it) }
    }
}
