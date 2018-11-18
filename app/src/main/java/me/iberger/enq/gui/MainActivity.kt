package me.iberger.enq.gui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.KEY_CURRENT_SONG
import me.iberger.enq.R
import me.iberger.enq.gui.fragments.CurrentSongFragment
import me.iberger.enq.gui.fragments.QueueFragment
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.KEY_QUEUE
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import me.iberger.jmusicbot.data.PlayerState
import timber.log.Timber

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mMenuIcons = listOf(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon2.cmd_magnify,
        CommunityMaterial.Icon2.cmd_star_outline
    )

    private lateinit var mMusicBot: MusicBot
    private lateinit var mProvider: List<MusicBotPlugin>
    private lateinit var mSuggester: List<MusicBotPlugin>
    private lateinit var mPlayerState: PlayerState

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private val mFragmentBundle = bundleOf()
    private lateinit var mHasUser: Deferred<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_bottom_navigation.setOnNavigationItemSelectedListener(this)
        mUIScope.launch {
            val icons = mMenuIcons.map { mBackgroundScope.async { IconicsDrawable(this@MainActivity).icon(it) } }
            main_bottom_navigation.menu.forEachIndexed { index, item ->
                item.icon = icons[index].await()
            }
        }
        showServerDiscoveryDialog(this@MainActivity, mBackgroundScope, true)
        mHasUser = MusicBot.hasUser(this)
    }

    fun continueWithLogin() =
        mBackgroundScope.launch { showLoginDialog(this@MainActivity, mBackgroundScope, mHasUser.await()) }


    fun continueWithBot(musicBot: MusicBot) = mBackgroundScope.launch {
        mMusicBot = musicBot
        // fetch relevant infos
        val queueJob = async { mMusicBot.queue }
        val playerStateJob = async { mMusicBot.playerState }
        val providerJob = async { mMusicBot.provider }
        val suggesterJob = async { mMusicBot.suggesters }
        val queueFragment = QueueFragment.newInstance(queueJob.await())
        val currentSongFragment = CurrentSongFragment.newInstance(playerStateJob.await())

        supportFragmentManager.commit {
            replace(R.id.main_content, queueFragment)
            replace(R.id.main_current_song, currentSongFragment)
        }
        supportFragmentManager.apply {
            putFragment(mFragmentBundle, KEY_QUEUE, queueFragment)
            putFragment(mFragmentBundle, KEY_CURRENT_SONG, currentSongFragment)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_queue -> {
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
    }

    override fun onDestroy() {
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
