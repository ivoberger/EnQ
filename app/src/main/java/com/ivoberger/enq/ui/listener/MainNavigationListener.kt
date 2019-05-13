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
package com.ivoberger.enq.ui.listener

import android.os.Bundle
import androidx.core.view.get
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class MainNavigationListener(private val mainActivity: MainActivity) : NavController.OnDestinationChangedListener {

    init {
        Timber.d("Initializing MainNavigationListener")
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        Timber.d("Navigated to ${destination.label} with id ${destination.id}")
        when (destination.id) {
            R.id.dest_queue -> checkIfNotChecked(0)
            R.id.dest_suggestions -> checkIfNotChecked(1)
            R.id.dest_favorites -> checkIfNotChecked(2)
            // remove highlight from bottom navigation if none of its menu points is selected (e.g. user info)
            else -> mainActivity.lifecycleScope.launch {
                mainActivity.bottom_navigation.menu.setGroupCheckable(0, false, true)
            }
        }
    }

    private fun checkIfNotChecked(idx: Int) = mainActivity.lifecycleScope.launch {
        mainActivity.bottom_navigation.menu.setGroupCheckable(0, true, true)
        if (!mainActivity.bottom_navigation.menu[idx].isChecked)
            mainActivity.bottom_navigation.menu[idx].isChecked = true
    }
}
