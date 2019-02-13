package me.iberger.enq.ui.listener

import android.os.Bundle
import androidx.core.view.get
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import kotlinx.android.synthetic.main.activity_main.*
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import timber.log.Timber

class MainNavigationListener(private val mainActivity: MainActivity) : NavController.OnDestinationChangedListener {

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        Timber.d("Navigated to ${destination.label}")
        when (destination.id) {
            R.id.Queue -> checkIfNotChecked(0)
            R.id.Suggestions -> checkIfNotChecked(1)
            R.id.Favorites -> checkIfNotChecked(2)
        }
    }

    private fun checkIfNotChecked(idx: Int) {
        if (!mainActivity.main_bottom_navigation.menu[idx].isChecked)
            mainActivity.main_bottom_navigation.menu[idx].isChecked = true
    }
}
