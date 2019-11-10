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
package com.ivoberger.enq

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.iid.FirebaseInstanceId
import com.ivoberger.enq.logging.EnQDebugTree
import com.ivoberger.enq.logging.FirebaseTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class EnQ : Application() {

    companion object {
        lateinit var instanceId: String
            private set
    }

    override fun onCreate() {
        MainScope().launch(Dispatchers.Default) {
            FirebaseApp.initializeApp(this@EnQ)
            FirebaseAnalytics.getInstance((this@EnQ)).setAnalyticsCollectionEnabled(false)
            instanceId = FirebaseInstanceId.getInstance().id
            // logging (and crash reporting)
            if (Timber.treeCount() < 1) Timber.plant(
                    if (BuildConfig.DEBUG) EnQDebugTree() else FirebaseTree(applicationContext)
            )
        }
        super.onCreate()
    }
}
