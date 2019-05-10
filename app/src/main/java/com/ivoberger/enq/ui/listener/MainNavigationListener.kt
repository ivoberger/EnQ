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
            else -> mainActivity.bottom_navigation.menu.setGroupCheckable(0, false, true)
        }
    }

    private fun checkIfNotChecked(idx: Int) = mainActivity.lifecycleScope.launch {
        mainActivity.bottom_navigation.menu.setGroupCheckable(0, true, true)
        if (!mainActivity.bottom_navigation.menu[idx].isChecked)
            mainActivity.bottom_navigation.menu[idx].isChecked = true
    }
}
