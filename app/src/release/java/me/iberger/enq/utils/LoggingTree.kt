package me.iberger.enq.utils

import android.util.Log
import io.sentry.Sentry
import io.sentry.event.Breadcrumb
import io.sentry.event.BreadcrumbBuilder
import timber.log.Timber

class LoggingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, msg: String, t: Throwable?) {
        val level: Breadcrumb.Level = when (priority) {
            Log.ERROR -> Breadcrumb.Level.ERROR
            Log.WARN -> Breadcrumb.Level.WARNING
            Log.INFO -> Breadcrumb.Level.INFO
            Log.DEBUG -> Breadcrumb.Level.DEBUG
            else -> return
        }
        val msgSplit = msg.split('|')
        val message = msgSplit[0]
        if (msgSplit.size == 3) {
            Sentry.getContext().addExtra(msgSplit[1], msgSplit[2])
        }


        Sentry.getContext().addTag("class", tag)
        if (level == Breadcrumb.Level.ERROR) {
            Sentry.capture(message)
            t?.let { Sentry.capture(it) }
        } else Sentry.getContext().recordBreadcrumb(BreadcrumbBuilder().setMessage(message).setLevel(level).build())

    }
}