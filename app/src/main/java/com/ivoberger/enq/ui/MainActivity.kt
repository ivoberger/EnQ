package com.ivoberger.enq.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
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
import com.ivoberger.enq.utils.*
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import splitties.resources.colorSL
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        private var mTreePlanted = false
        var favorites: MutableList<Song> = mutableListOf()
        lateinit var config: Configuration
    }

    lateinit var searchView: SearchView

    val mainScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private val mViewModel: MainViewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }
    private val mNavController: NavController by lazy {
        main_content.findNavController()
    }

    private val mPlayerFragment by lazy { PlayerFragment.newInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // general setup
        mBackgroundScope.launch {
            // logging (and crash reporting)
            if (!mTreePlanted) {
                Timber.plant(if (BuildConfig.DEBUG) EnQDebugTree() else FirebaseTree(this@MainActivity))
                mTreePlanted = true
            }
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
        ).map { mBackgroundScope.async { icon(it).color(colorSL(R.color.bottom_navigation)!!) } }
        mainScope.launch {
            main_bottom_navigation.menu.forEachIndexed { idx, itm -> itm.icon = icons[idx].await() }
        }

        if (!JMusicBot.state.hasServer) {
            JMusicBot.discoverHost()
            showServerDiscoveryDialog(true)
        } else continueToLogin()
    }

    /**
     * continueToLogin is called by showServerDiscoveryDialog after a server was found
     */
    fun continueToLogin() = mBackgroundScope.launch {
        Timber.d("Continuing with login")
        showLoginDialog()
    }

    /**
     * continueWithBot is called by showLoginDialog after loginUser is complete
     */
    fun continueWithBot() = mBackgroundScope.launch {
        JMusicBot.connectionChangeListeners.add((ConnectionListener(this@MainActivity)))
        mNavController.setGraph(R.navigation.nav_graph)
        supportFragmentManager.commit {
            replace(R.id.main_current_song, mPlayerFragment, null)
        }
        Timber.d("${this@MainActivity}")
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 0) super.onBackPressed()
        else supportFragmentManager.popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_main, menu)
        // save menu for use in SearchFragment
        searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView

        var playerCollapse = mViewModel.playerCollapsed
        // set listener to iconify the SearchView when back is pressed
        mNavController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.id != R.id.Search && !searchView.isIconified) {
                Timber.d("Closing Search View")
                searchView.isIconified = true
            }
        }

        searchView.setOnSearchClickListener {
            if (!JMusicBot.isConnected) return@setOnSearchClickListener
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
                mNavController.navigate(R.id.About)
                true
            }

            R.id.app_bar_search -> false
            R.id.app_bar_user_options -> {
                mNavController.navigate(R.id.UserInfo)
                true
            }
            R.id.app_bar_settings -> {
                mNavController.navigate(R.id.Settings)
                true
            }
            else -> false
        }
    }

    fun search(query: String) = mainScope.launch {
        searchView.isIconified = false
        // give search fragment time to setup
        delay(200)
        searchView.setQuery(query, false)
    }

    fun reset() = mainScope.launch {
        supportFragmentManager.commitNow {
            supportFragmentManager.fragments.forEach {
                remove(it)
            }
        }
        recreate()
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
