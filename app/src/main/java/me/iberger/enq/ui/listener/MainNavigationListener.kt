package me.iberger.enq.ui.listener

import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.fragments.FavoritesFragment
import me.iberger.enq.ui.fragments.QueueFragment
import me.iberger.enq.ui.fragments.SuggestionsFragment

class MainNavigationListener(
    private val mainActivity: MainActivity,
    private val supportFragmentManager: FragmentManager = mainActivity.supportFragmentManager
) :
    BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.nav_queue -> {
            mainActivity.changePlayerCollapse(false)
            supportFragmentManager.commit {
                replace(R.id.main_content, QueueFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        R.id.nav_suggestions -> {
            mainActivity.changePlayerCollapse(true)
            supportFragmentManager.commit {
                replace(R.id.main_content, SuggestionsFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        R.id.nav_starred -> {
            mainActivity.changePlayerCollapse(true)
            supportFragmentManager.commit {
                replace(R.id.main_content, FavoritesFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        else -> false
    }
}
