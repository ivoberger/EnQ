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
import me.iberger.enq.gui.fragments.QueueFragment
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
    lateinit var suggester: List<MusicBotPlugin>

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
        switchToQueue()
        val providerJob = async { musicBot.provider }
        val suggesterJob = async { musicBot.suggesters }

        val currentSongFragment = CurrentSongFragment.newInstance()
        val queueFragment = QueueFragment.newInstance()
        musicBot.startPlayerUpdates(currentSongFragment)
        musicBot.startQueueUpdates(queueFragment)
        supportFragmentManager.commit {
            add(R.id.main_current_song, currentSongFragment, null)
            add(R.id.main_content, queueFragment, null)
        }
        provider = providerJob.await()
        suggester = suggesterJob.await()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.nav_queue -> {
            switchToQueue()
            true
        }
        R.id.nav_suggestions -> {
            true
        }
        R.id.nav_starred -> {
            true
        }
        else -> false
    }

    private fun switchToQueue() = mUIScope.launch {

    }

    override fun onDestroy() {
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
