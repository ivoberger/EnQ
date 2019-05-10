package com.ivoberger.enq

import android.app.Application
import com.ivoberger.enq.logging.EnQDebugTree
import com.ivoberger.enq.logging.FirebaseTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class EnQ : Application() {
    override fun onCreate() {
        MainScope().launch(Dispatchers.Default) {
            // logging (and crash reporting)
            if (Timber.treeCount() < 1) Timber.plant(
                if (BuildConfig.DEBUG) EnQDebugTree() else FirebaseTree(applicationContext)
            )
        }
        super.onCreate()
    }
}