package me.iberger.enq.gui

import android.os.Bundle
import android.view.MenuItem
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
import me.iberger.enq.gui.adapterItems.QueueEntryItem
import me.iberger.enq.gui.fragments.CurrentSongFragment
import me.iberger.enq.utils.showLoginDialog
import me.iberger.enq.utils.showServerDiscoveryDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import me.iberger.jmusicbot.data.QueueEntry
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mMenuIcons = listOf<IIcon>(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon.cmd_all_inclusive,
        CommunityMaterial.Icon2.cmd_star_outline
    )

    lateinit var musicBot: MusicBot
    lateinit var queue: List<QueueEntry>
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
        // fetch relevant infos
        val queueJob = async { musicBot.queue }
        val playerStateJob = async { musicBot.playerState }
        val providerJob = async { musicBot.provider }
        val suggesterJob = async { musicBot.suggesters }
        supportFragmentManager.commit { add(R.id.main_current_song, CurrentSongFragment.newInstance(), null) }
        mUIScope.launch {
            val listAdapter = ItemAdapter<QueueEntryItem>()
            main_content.layoutManager = LinearLayoutManager(this@MainActivity)
            main_content.adapter = FastAdapter.with<QueueEntryItem, ItemAdapter<QueueEntryItem>>(listAdapter)
            listAdapter.set(queueJob.await().map { QueueEntryItem(it) })
        }
        queue = queueJob.await()
        provider = providerJob.await()
        suggester = suggesterJob.await()
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
