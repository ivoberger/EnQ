package me.iberger.enq.gui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.fragments.*
import me.iberger.enq.utils.loadFavorites
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import me.iberger.jmusicbot.data.Song
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    companion object {
        var mFavorites: MutableList<Song> = mutableListOf()
        lateinit var musicBot: MusicBot
    }

    private val mMenuIcons = listOf<IIcon>(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon.cmd_all_inclusive,
        CommunityMaterial.Icon2.cmd_star_outline
    )
    lateinit var optionsMenu: Menu
    lateinit var provider: List<MusicBotPlugin>

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private lateinit var mHasUser: Deferred<Boolean>

    private var mPlayerCollapsed = false
    private var mBottomNavCollapsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_bottom_navigation.setOnNavigationItemSelectedListener(this)
        mUIScope.launch {
            val icons = mMenuIcons.map { mBackgroundScope.async { IconicsDrawable(this@MainActivity, it) } }
            main_bottom_navigation.menu.forEachIndexed { index, item ->
                item.icon = icons[index].await()
            }
        }
        showServerDiscoveryDialog(this@MainActivity, mBackgroundScope, true)
        mHasUser = MusicBot.hasUser(this)
        mBackgroundScope.launch { mFavorites = loadFavorites(this@MainActivity) }
    }

    fun continueWithLogin() =
        mBackgroundScope.launch { showLoginDialog(this@MainActivity, mBackgroundScope, mHasUser.await()) }


    fun continueWithBot(mBot: MusicBot) = mBackgroundScope.launch {
        musicBot = mBot
        val providerJob = async { musicBot.provider }

        val currentSongFragment = CurrentSongFragment.newInstance()
        musicBot.startPlayerUpdates(currentSongFragment)
        supportFragmentManager.commit {
            replace(R.id.main_current_song, currentSongFragment, null)
            replace(R.id.main_content, QueueFragment.newInstance(), null)
        }
        provider = providerJob.await()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_main, menu)
        optionsMenu = menu
        val searchView = (menu.findItem(R.id.app_bar_search).actionView as SearchView)

        searchView.setOnSearchClickListener {
            changePlayerCollapse(true, 0)
            changeBottomNavCollapse(true, 0)
            supportFragmentManager.commit { replace(R.id.main_content, SearchFragment.newInstance()) }
        }
        searchView.setOnCloseListener {
            changeBottomNavCollapse(false)
            changePlayerCollapse(false)
            supportFragmentManager.commit { replace(R.id.main_content, QueueFragment.newInstance()) }
            return@setOnCloseListener false
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.nav_queue -> {
            changePlayerCollapse(false)
            supportFragmentManager.commit { replace(R.id.main_content, QueueFragment.newInstance()) }
            true
        }
        R.id.nav_suggestions -> {
            changePlayerCollapse(true)
            supportFragmentManager.commit { replace(R.id.main_content, SuggestionsFragment.newInstance()) }
            true
        }
        R.id.nav_starred -> {
            changePlayerCollapse(true)
            supportFragmentManager.commit { replace(R.id.main_content, FavoritesFragment.newInstance()) }
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
