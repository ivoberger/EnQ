package me.iberger.enq.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.BuildConfig
import me.iberger.enq.R
import me.iberger.enq.backend.Configuration
import me.iberger.enq.ui.fragments.CurrentSongFragment
import me.iberger.enq.ui.fragments.QueueFragment
import me.iberger.enq.ui.fragments.SearchFragment
import me.iberger.enq.ui.listener.ConnectionListener
import me.iberger.enq.ui.listener.MainNavigationListener
import me.iberger.enq.utils.loadFavorites
import me.iberger.enq.utils.make
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.model.Song
import me.iberger.timbersentry.SentryTree
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        var connected = false
        var favorites: MutableList<Song> = mutableListOf()
        lateinit var config: Configuration
    }

    lateinit var optionsMenu: Menu

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private var mPlayerCollapsed = false
    private var mBottomNavCollapsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // general setup
        mBackgroundScope.launch {
            // logging
            Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else SentryTree(context = this@MainActivity))
            // saved data
            favorites = loadFavorites(this@MainActivity)
            config = Configuration(this@MainActivity)
        }
        // setup main bottom navigation
        main_bottom_navigation.setOnNavigationItemSelectedListener(MainNavigationListener(this))

        // load bottom navigation icons async
        val icons = listOf<IIcon>(
            CommunityMaterial.Icon2.cmd_playlist_play,
            CommunityMaterial.Icon.cmd_all_inclusive,
            CommunityMaterial.Icon2.cmd_star_outline
        ).map { mBackgroundScope.async { it.make(this@MainActivity) } }
        mUIScope.launch {
            main_bottom_navigation.menu.forEachIndexed { idx, itm -> itm.icon = icons[idx].await() }
        }
        mBackgroundScope.launch { JMusicBot.init(this@MainActivity) }
        showServerDiscoveryDialog(true)
    }

    /**
     * continueToLogin is called by showServerDiscoveryDialog after a server was found
     */
    fun continueToLogin() = mBackgroundScope.launch { showLoginDialog() }

    /**
     * continueWithBot is called by showLoginDialog after login is complete
     */
    fun continueWithBot() = mBackgroundScope.launch {
        connected = true
        JMusicBot.connectionChangeListeners.add((ConnectionListener(this@MainActivity)))
        val currentSongFragment = CurrentSongFragment.newInstance()
        supportFragmentManager.commit {
            replace(R.id.main_current_song, currentSongFragment, null)
            replace(R.id.main_content, QueueFragment.newInstance(), null)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) super.onBackPressed()
        else supportFragmentManager.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_main, menu)
        // save menu for use in SearchFragment
        optionsMenu = menu
        val searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView

        var playerCollapse = mPlayerCollapsed
        // set listener to iconify the SearchView when back is pressed
        val backStackListener = FragmentManager.OnBackStackChangedListener { searchView.isIconified = true }

        searchView.setOnSearchClickListener {
            if (!connected) return@setOnSearchClickListener
            // save player collapse state
            playerCollapse = mPlayerCollapsed
            // collapse bottom UI
            changePlayerCollapse(true, 0)
            changeBottomNavCollapse(true, 0)
            supportFragmentManager.commit {
                // hide current fragment (always at 1 as 0 ist the player)
                hide(supportFragmentManager.fragments[1])
                // add search fragment with transition
                add(R.id.main_content, SearchFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                addToBackStack(null)
            }
            // execute transactions and set listener afterwards
            supportFragmentManager.executePendingTransactions()
            supportFragmentManager.addOnBackStackChangedListener(backStackListener)
        }
        searchView.setOnCloseListener {
            // un-collapse bottom UI
            changeBottomNavCollapse(false)
            changePlayerCollapse(playerCollapse)
            // remove search fragment
            supportFragmentManager.popBackStack()
            supportFragmentManager.commit {
                // show previous fragment again
                show(supportFragmentManager.fragments[1])
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            }
            supportFragmentManager.removeOnBackStackChangedListener(backStackListener)
            return@setOnCloseListener false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return false
        return when (item.itemId) {
            R.id.app_bar_about -> {
                LibsBuilder().apply {
                    activityStyle = Libs.ActivityStyle.DARK
                    aboutAppName = getString(R.string.app_name)
                    aboutShowIcon = true
                    showVersion = true
                    aboutShowVersionName = true
                    aboutShowVersionCode = false
                    aboutDescription = "Collaborative DJ-ing"
                    activityTitle = getString(R.string.nav_about)
                    showLicense = true
                }.start(this)
                true
            }
            else -> false
        }
    }

    /**
     * collapsed or shows the FrameView containing the PlayerFragment
     * @param collapse: specifies if the player should be collapsed
     * @param duration: duration of the animation
     */
    fun changePlayerCollapse(collapse: Boolean, duration: Long = 1000) = mUIScope.launch {
        if (mPlayerCollapsed == collapse) return@launch
        if (!mPlayerCollapsed) {
            main_current_song.animate().setDuration(duration)
                .translationYBy(main_current_song.height.toFloat())
                .withEndAction { main_current_song.visibility = View.GONE }.start()
        } else {
            main_current_song.animate().setDuration(duration)
                .translationYBy(-main_current_song.height.toFloat())
                .withStartAction { main_current_song.visibility = View.VISIBLE }.start()
        }
        mPlayerCollapsed = collapse
    }

    /**
     * collapsed or shows the BottomNavigation
     * @param collapse: specifies if the navigation should be collapsed
     * @param duration: duration of the animation
     */
    private fun changeBottomNavCollapse(collapse: Boolean, duration: Long = 1000) =
        mUIScope.launch {
            if (mBottomNavCollapsed == collapse) return@launch
            if (!mBottomNavCollapsed) {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(main_current_song.height.toFloat())
                    .withEndAction { main_bottom_navigation.visibility = View.GONE }.start()

            } else {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(-main_current_song.height.toFloat())
                    .withStartAction { main_bottom_navigation.visibility = View.VISIBLE }.start()
            }
            mBottomNavCollapsed = collapse
        }

    override fun onDestroy() {
        // cancel all running coroutines
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
