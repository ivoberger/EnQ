package me.iberger.enq.gui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.TABS
import me.iberger.enq.gui.adapterItems.QueueEntryItem
import me.iberger.enq.gui.fragments.CurrentSongFragment
import me.iberger.enq.utils.loadFavorites
import me.iberger.enq.utils.saveFavorites
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.data.Song
import me.iberger.jmusicbot.listener.QueueUpdateListener
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener, QueueUpdateListener {

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

    private var mFavorites: MutableList<Song> = mutableListOf()
    private var mQueue: List<QueueEntry> = listOf()

    private var mCurrentTab = TABS.QUEUE
    private lateinit var mItemAdapters: MutableMap<TABS, ItemAdapter<QueueEntryItem>>
    private lateinit var mFastAdapters: MutableMap<TABS, FastAdapter<QueueEntryItem>>

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
        mBackgroundScope.launch { mFavorites = this@MainActivity.loadFavorites() }
    }

    fun continueWithLogin() =
        mBackgroundScope.launch { showLoginDialog(this@MainActivity, mBackgroundScope, mHasUser.await()) }


    fun continueWithBot(mBot: MusicBot) = mBackgroundScope.launch {
        musicBot = mBot
        switchToQueue()
        val providerJob = async { musicBot.provider }
        val suggesterJob = async { musicBot.suggesters }
        supportFragmentManager.commit { add(R.id.main_current_song, CurrentSongFragment.newInstance(), null) }
        provider = providerJob.await()
        suggester = suggesterJob.await()
        joinAll()
        musicBot.startQueueUpdates(this@MainActivity)
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
        main_content.layoutManager = LinearLayoutManager(this@MainActivity)
        mItemAdapters[TABS.QUEUE] = ItemAdapter()
        mFastAdapters[TABS.QUEUE] =
                FastAdapter.with<QueueEntryItem, ItemAdapter<QueueEntryItem>>(mItemAdapters[TABS.QUEUE]!!)
        main_content.adapter = mFastAdapters[TABS.QUEUE]
    }

    override fun onQueueChanged(newQueue: List<QueueEntry>) {
        if (mQueue == newQueue) return
        mQueue = newQueue
        val itemQueue = newQueue.map { QueueEntryItem((it)) }
        mUIScope.launch { mItemAdapters[TABS.QUEUE]?.set(itemQueue) }
    }

    override fun onUpdateError(e: Exception) {
        Timber.e(e)
        Toast.makeText(this, "Something horrific just happened", Toast.LENGTH_SHORT).show()
    }

    // Favorite management

    fun isInFavorites(song: Song) = song in mFavorites

    fun changeFavoriteStatus(song: Song) = mBackgroundScope.launch {
        if (song in mFavorites) {
            Timber.d("Removing $song from favorites")
            mFavorites.remove(song)
        } else {
            Timber.d("Adding $song to favorites")
            mFavorites.add(song)
        }
        saveFavorites(mFavorites)
    }


    override fun onDestroy() {
        mUIScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDestroy()
    }
}
