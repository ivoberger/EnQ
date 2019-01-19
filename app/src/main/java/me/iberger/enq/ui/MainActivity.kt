package me.iberger.enq.ui

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.BuildConfig
import me.iberger.enq.R
import me.iberger.enq.backend.Configuration
import me.iberger.enq.ui.fragments.*
import me.iberger.enq.ui.listener.ConnectionListener
import me.iberger.enq.ui.listener.MainNavigationListener
import me.iberger.enq.utils.*
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.Song
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
        showServerDiscoveryDialog(true)
    }

    /**
     * continueToLogin is called by showServerDiscoveryDialog after a server was found
     */
    fun continueToLogin() =
        mBackgroundScope.launch { showLoginDialog(MusicBot.hasAuthorization(applicationContext)) }


    /**
     * continueWithBot is called by showLoginDialog after login is complete
     */
    fun continueWithBot() = mBackgroundScope.launch {
        connected = true
        MusicBot.instance?.connectionChangeListeners?.add((ConnectionListener(this@MainActivity)))
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
        val backStackListener =
            FragmentManager.OnBackStackChangedListener { searchView.isIconified = true }

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

    /**
     * collapsed or shows the FrameView containing the PlayerFragment
     * @param requestCollapse: specifies if the player should be collapsed
     * @param duration: duration of the animation
     */
    fun changePlayerCollapse(requestCollapse: Boolean, duration: Long = 1000) = mUIScope.launch {
        if (mPlayerCollapsed == requestCollapse) return@launch
        if (!mPlayerCollapsed) {
            main_current_song.animate().setDuration(duration)
                .translationYBy(main_current_song.height.toFloat())
                .withEndAction { main_current_song.visibility = View.GONE }.start()
        } else {
            main_current_song.animate().setDuration(duration)
                .translationYBy(-main_current_song.height.toFloat())
                .withStartAction { main_current_song.visibility = View.VISIBLE }.start()
        }
        mPlayerCollapsed = requestCollapse
    }

    /**
     * collapsed or shows the BottomNavigation
     * @param requestCollapse: specifies if the navigation should be collapsed
     * @param duration: duration of the animation
     */
    private fun changeBottomNavCollapse(requestCollapse: Boolean, duration: Long = 1000) =
        mUIScope.launch {
            if (mBottomNavCollapsed == requestCollapse) return@launch
            if (!mBottomNavCollapsed) {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(main_current_song.height.toFloat())
                    .withEndAction { main_bottom_navigation.visibility = View.GONE }.start()

            } else {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(-main_current_song.height.toFloat())
                    .withStartAction { main_bottom_navigation.visibility = View.VISIBLE }.start()
            }
            mBottomNavCollapsed = requestCollapse
        }

    override fun onDestroy() {
        // cancel all running coroutines
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
