package me.iberger.enq.gui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.fragments.*
import me.iberger.enq.utils.LoggingTree
import me.iberger.enq.utils.loadFavorites
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.Song
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    companion object {
        var mFavorites: MutableList<Song> = mutableListOf()
    }

    private val mMenuIcons = listOf<IIcon>(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon.cmd_all_inclusive,
        CommunityMaterial.Icon2.cmd_star_outline
    )
    lateinit var optionsMenu: Menu

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private var mPlayerCollapsed = false
    private var mBottomNavCollapsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBackgroundScope.launch {
            Timber.plant(LoggingTree())
            Sentry.init(AndroidSentryClientFactory(applicationContext))
            mFavorites = loadFavorites(this@MainActivity)
        }

        main_bottom_navigation.setOnNavigationItemSelectedListener(this)
        mUIScope.launch {
            val icons = mMenuIcons.map { mBackgroundScope.async { IconicsDrawable(this@MainActivity, it) } }
            main_bottom_navigation.menu.forEachIndexed { index, item ->
                item.icon = icons[index].await()
            }
        }
        showServerDiscoveryDialog(this@MainActivity, mBackgroundScope, true)
    }

    fun continueWithLogin() =
        mBackgroundScope.launch {
            showLoginDialog(
                this@MainActivity,
                mBackgroundScope,
                MusicBot.hasUser(this@MainActivity).await()
            )
        }


    fun continueWithBot() = mBackgroundScope.launch {
        val currentSongFragment = CurrentSongFragment.newInstance()
        supportFragmentManager.commit {
            replace(R.id.main_current_song, currentSongFragment, null)
            replace(R.id.main_content, QueueFragment.newInstance(), null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_main, menu)
        optionsMenu = menu
        val searchView = (menu.findItem(R.id.app_bar_search).actionView as SearchView)

        var playerCollapse = mPlayerCollapsed

        searchView.setOnSearchClickListener {
            playerCollapse = mPlayerCollapsed
            changePlayerCollapse(true, 0)
            changeBottomNavCollapse(true, 0)
            supportFragmentManager.commit {
                hide(supportFragmentManager.fragments[1])
                add(R.id.main_content, SearchFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                addToBackStack(null)
            }
        }
        searchView.setOnCloseListener {
            changeBottomNavCollapse(false)
            changePlayerCollapse(playerCollapse)
            supportFragmentManager.popBackStack()
            supportFragmentManager.commit {
                show(supportFragmentManager.fragments[1])
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            }
            return@setOnCloseListener false
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.nav_queue -> {
            changePlayerCollapse(false)
            supportFragmentManager.commit {
                replace(R.id.main_content, QueueFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        R.id.nav_suggestions -> {
            changePlayerCollapse(true)
            supportFragmentManager.commit {
                replace(R.id.main_content, SuggestionsFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        R.id.nav_starred -> {
            changePlayerCollapse(true)
            supportFragmentManager.commit {
                replace(R.id.main_content, FavoritesFragment.newInstance())
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            true
        }
        else -> false
    }

    private fun changePlayerCollapse(requestCollapse: Boolean, duration: Long = 1000) {
        if (mPlayerCollapsed == requestCollapse) return
        if (!mPlayerCollapsed) {
            main_current_song.animate().setDuration(duration).translationYBy(main_current_song.height.toFloat())
                .withEndAction { main_current_song.visibility = View.GONE }.start()
        } else {
            main_current_song.animate().setDuration(duration).translationYBy(-main_current_song.height.toFloat())
                .withStartAction { main_current_song.visibility = View.VISIBLE }.start()
        }
        mPlayerCollapsed = requestCollapse
    }

    private fun changeBottomNavCollapse(requestCollapse: Boolean, duration: Long = 1000) {
        if (mBottomNavCollapsed == requestCollapse) return
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
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
