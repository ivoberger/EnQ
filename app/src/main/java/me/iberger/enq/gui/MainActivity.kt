package me.iberger.enq.gui

import android.os.Bundle
import android.view.MenuItem
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
import me.iberger.enq.gui.fragments.CurrentSongFragment
import me.iberger.enq.gui.fragments.FavoritesFragment
import me.iberger.enq.gui.fragments.QueueFragment
import me.iberger.enq.gui.fragments.SuggestionsFragment
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mMenuIcons = listOf<IIcon>(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon.cmd_all_inclusive,
        CommunityMaterial.Icon2.cmd_star_outline
    )

    lateinit var musicBot: MusicBot
    lateinit var provider: List<MusicBotPlugin>

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private lateinit var mHasUser: Deferred<Boolean>

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

    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.nav_queue -> {
            supportFragmentManager.commit { replace(R.id.main_content, QueueFragment.newInstance()) }
            true
        }
        R.id.nav_suggestions -> {
            supportFragmentManager.commit { replace(R.id.main_content, SuggestionsFragment.newInstance()) }
            true
        }
        R.id.nav_starred -> {
            supportFragmentManager.commit { replace(R.id.main_content, FavoritesFragment.newInstance()) }
            true
        }
        else -> false
    }

    override fun onDestroy() {
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
