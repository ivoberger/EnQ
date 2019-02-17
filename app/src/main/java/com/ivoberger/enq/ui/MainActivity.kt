package com.ivoberger.enq.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.ivoberger.enq.BuildConfig
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.ui.fragments.PlayerFragment
import com.ivoberger.enq.ui.listener.ConnectionListener
import com.ivoberger.enq.ui.listener.MainNavigationListener
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.loadFavorites
import com.ivoberger.enq.utils.showLoginDialog
import com.ivoberger.enq.utils.showServerDiscoveryDialog
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.timbersentry.SentryTree
import splitties.resources.colorSL
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        var favorites: MutableList<Song> = mutableListOf()
        lateinit var config: Configuration
    }

    lateinit var optionsMenu: Menu

    val mainScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private val mViewModel: MainViewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }
    private val mNavController: NavController by lazy { main_content.findNavController() }

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

        mNavController.addOnDestinationChangedListener(MainNavigationListener(this))
        main_bottom_navigation.setupWithNavController(mNavController)
        // load bottom navigation icons async
        val icons = listOf<IIcon>(
            CommunityMaterial.Icon2.cmd_playlist_play,
            CommunityMaterial.Icon.cmd_all_inclusive,
            CommunityMaterial.Icon2.cmd_star_outline
        ).map { mBackgroundScope.async { icon(it).color(colorSL(R.color.main_navigation)!!) } }
        mainScope.launch {
            main_bottom_navigation.menu.forEachIndexed { idx, itm -> itm.icon = icons[idx].await() }
        }
        if (!mViewModel.connected) {
            JMusicBot.discoverHost()
            showServerDiscoveryDialog(true)
        }
    }

    /**
     * continueToLogin is called by showServerDiscoveryDialog after a server was found
     */
    fun continueToLogin() = mBackgroundScope.launch { showLoginDialog() }

    /**
     * continueWithBot is called by showLoginDialog after loginUser is complete
     */
    fun continueWithBot() = mBackgroundScope.launch {
        mViewModel.connected = true
        JMusicBot.connectionChangeListeners.add((ConnectionListener(this@MainActivity)))
        mNavController.setGraph(R.navigation.nav_graph)
        supportFragmentManager.commit {
            replace(R.id.main_current_song, PlayerFragment.newInstance(), null)
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

        var playerCollapse = mViewModel.playerCollapsed
        // set listener to iconify the SearchView when back is pressed
        mNavController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.id != R.id.Search && !searchView.isIconified) {
                Timber.d("Closing Search View")
                searchView.isIconified = true
            }
        }

        searchView.setOnSearchClickListener {
            if (!mViewModel.connected) return@setOnSearchClickListener
            mNavController.navigate(R.id.Search)
            // save player collapse state
            playerCollapse = mViewModel.playerCollapsed
            // collapse bottom UI
            changePlayerCollapse(true, 0)
            changeBottomNavCollapse(true, 0)
        }
        searchView.setOnCloseListener {
            // un-collapse bottom UI
            changeBottomNavCollapse(false)
            changePlayerCollapse(playerCollapse)
            // remove search fragment
            mNavController.navigateUp()
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

            R.id.app_bar_search -> false
            R.id.app_bar_settings -> {
                mNavController.navigate(R.id.Settings)
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
    fun changePlayerCollapse(collapse: Boolean, duration: Long = 1000) = mainScope.launch {
        if (mViewModel.playerCollapsed == collapse) return@launch
        if (!mViewModel.playerCollapsed) {
            main_current_song.animate().setDuration(duration)
                .translationYBy(main_current_song.height.toFloat())
                .withEndAction { main_current_song.visibility = View.GONE }.start()
        } else {
            main_current_song.animate().setDuration(duration)
                .translationYBy(-main_current_song.height.toFloat())
                .withStartAction { main_current_song.visibility = View.VISIBLE }.start()
        }
        mViewModel.playerCollapsed = collapse
    }

    /**
     * collapsed or shows the BottomNavigation
     * @param collapse: specifies if the navigation should be collapsed
     * @param duration: duration of the animation
     */
    private fun changeBottomNavCollapse(collapse: Boolean, duration: Long = 1000) =
        mainScope.launch {
            if (mViewModel.bottomNavCollapsed == collapse) return@launch
            if (!mViewModel.bottomNavCollapsed) {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(main_current_song.height.toFloat())
                    .withEndAction { main_bottom_navigation.visibility = View.GONE }.start()

            } else {
                main_bottom_navigation.animate().setDuration(duration)
                    .translationYBy(-main_current_song.height.toFloat())
                    .withStartAction { main_bottom_navigation.visibility = View.VISIBLE }.start()
            }
            mViewModel.bottomNavCollapsed = collapse
        }

    override fun onDestroy() {
        // cancel all running coroutines
        mainScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
