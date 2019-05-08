package com.ivoberger.enq.logging

import timber.log.Timber

class EnQDebugTree : Timber.DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String? {
        val defaultTag = super.createStackElementTag(element)
        return "EnQDebug/$defaultTag"
    }
}
