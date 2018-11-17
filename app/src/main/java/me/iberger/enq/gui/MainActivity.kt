package me.iberger.enq.gui

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.fragments.CurrentSongFragment
import me.iberger.enq.gui.fragments.QueueFragment
import me.iberger.enq.utils.showServerNotFoundDialog
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.exceptions.AuthException
import timber.log.Timber

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mMenuIcons = listOf(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon2.cmd_magnify,
        CommunityMaterial.Icon2.cmd_star_outline
    )

    private val mScope = CoroutineScope(Dispatchers.Main)
    private lateinit var mHasUser: Deferred<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_bottom_navigation.setOnNavigationItemSelectedListener(this)
        mScope.launch {
            val icons = mMenuIcons.map { async { IconicsDrawable(this@MainActivity).icon(it) } }
            main_bottom_navigation.menu.forEachIndexed { index, item ->
                item.icon = icons[index].await()
            }
        }
        showServerNotFoundDialog(this@MainActivity, mScope, true)
        mHasUser = MusicBot.hasUser(this)
    }

    fun continueWithLogin() = mScope.launch {
        if (mHasUser.await()) {
            login()
        } else {
            val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
            AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
                .setTitle(R.string.tlt_login)
                .setMessage(R.string.msg_login)
                .setPositiveButton(R.string.btn_login) { dialog, _ ->
                    login(dialogView.findViewById<EditText>(R.id.login_username).text.toString())
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
                .show()
        }
    }

    private fun login(userName: String? = null, password: String? = null) = mScope.launch {
        Timber.d("Attempting login for user $userName")
        try {
            val musicBot = MusicBot.init(this@MainActivity, userName).await()
            password?.let { musicBot.changePassword(it).await() }
//                Timber.d("User: ${musicBot.user}")
            withContext(Dispatchers.Main) {
                supportFragmentManager.commit {
                    replace(R.id.main_content, QueueFragment.newInstance())
                    replace(R.id.main_current_song, CurrentSongFragment())
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
            if (e is AuthException) Timber.d("Reason: ${e.reason}")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_queue -> {
                true
            }
            R.id.nav_search -> {
                true
            }
            R.id.nav_starred -> {
                true
            }
            else -> false
        }
    }
}
