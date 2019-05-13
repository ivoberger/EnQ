/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.ivoberger.enq.MainDirections
import com.ivoberger.enq.R
import com.ivoberger.enq.model.ServerInfo
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.fragments.PlayerFragment
import com.ivoberger.enq.ui.listener.ConnectionListener
import com.ivoberger.enq.ui.listener.MainNavigationListener
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.awaitEnd
import com.ivoberger.enq.utils.hideKeyboard
import com.ivoberger.enq.utils.icon
import com.ivoberger.jmusicbot.JMusicBot
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.typeface.IIcon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.resources.colorSL
import timber.log.Timber

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class MainActivity : AppCompatActivity() {

    lateinit var searchView: SearchView

    private val mViewModel: MainViewModel by viewModels()
    private val mConnectionListener by lazy { ConnectionListener(this) }
    val navController: NavController by lazy { container_main_content.findNavController() }
    private val KEY_CURRENT_SERVER = "currentServer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // general setup
        navController.addOnDestinationChangedListener(MainNavigationListener(this))
        bottom_navigation.setupWithNavController(navController)

        // load bottom navigation icons async
        val icons = listOf<IIcon>(
            CommunityMaterial.Icon2.cmd_playlist_play,
            CommunityMaterial.Icon.cmd_all_inclusive,
            CommunityMaterial.Icon2.cmd_star_outline
        ).map { lifecycleScope.async(Dispatchers.Default) { icon(it).color(colorSL(R.color.bottom_navigation)) } }
        lifecycleScope.launch { bottom_navigation.menu.forEachIndexed { idx, itm -> itm.icon = icons[idx].await() } }

        when {
            JMusicBot.isConnected -> continueWithBot()
            savedInstanceState != null && AppSettings.getLatestUser() != null -> {
                Timber.d("Resuming from saved state")
                savedInstanceState.getParcelable<ServerInfo>(KEY_CURRENT_SERVER)?.let {
                    lifecycleScope.launch {
                        try {
                            JMusicBot.connect(
                                AppSettings.getLatestUser()!!,
                                it.baseUrl,
                                AppSettings.savedToken
                            )
                            continueWithBot()
                        } catch (e: Exception) {
                            Timber.w(e)
                            navController.navigate(MainDirections.actionGlobalServerDiscoveryDialog())
                        }
                    }
                }
            }
            JMusicBot.state.hasServer -> navController.navigate(MainDirections.actionGlobalLoginDialog(true))
            else -> navController.navigate(MainDirections.actionGlobalServerDiscoveryDialog())
        }
    }

    /**
     * continueWithBot is called by showLoginDialog after loginUser is complete
     */
    fun continueWithBot() = lifecycleScope.launch(Dispatchers.Default) {
        Timber.d("Continuing")
        supportFragmentManager.commit { replace(R.id.container_current_song, PlayerFragment(), null) }
        if (navController.currentDestination?.id != R.id.dest_queue) navController.popBackStack(R.id.dest_queue, false)
        // hide keyboard in case is wasn't hidden after login
        hideKeyboard()
        AppSettings.addUser(JMusicBot.user!!)
        JMusicBot.connectionListeners.add(mConnectionListener)
        JMusicBot.connectionListeners.add(mViewModel)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_options, menu)
        // save menu for use in SearchFragment
        searchView = menu.findItem(R.id.app_bar_search).actionView as SearchView

        var playerCollapse = mViewModel.playerCollapsed
        // set listener to iconify the SearchView when back is pressed
        navController.addOnDestinationChangedListener { _, dest, _ ->
            if (dest.id != R.id.dest_search && !searchView.isIconified) {
                Timber.d("Closing Search View")
                searchView.isIconified = true
            }
        }

        searchView.setOnSearchClickListener {
            if (!JMusicBot.isConnected) return@setOnSearchClickListener
            navController.navigate(MainDirections.actionGlobalSearch())
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
            navController.navigateUp()
            return@setOnCloseListener false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return false
        return when (item.itemId) {
            R.id.app_bar_about -> {
                navController.navigate(MainDirections.actionGlobalAbout())
                true
            }

            R.id.app_bar_search -> false
            R.id.app_bar_user_info -> {
                navController.navigate(MainDirections.actionGlobalUserInfo())
                true
            }
            R.id.app_bar_settings -> {
                navController.navigate(MainDirections.actionGlobalSettings())
                true
            }
            else -> false
        }
    }

    fun search(query: String) = lifecycleScope.launch {
        searchView.isIconified = false
        // give search fragment time to setup
        delay(200)
        searchView.setQuery(query, false)
    }

    fun reset() = lifecycleScope.launch {
        JMusicBot.logout()
        navController.navigate(MainDirections.actionGlobalLoginDialog(false))
        supportFragmentManager.commitNow {
            supportFragmentManager.fragments.forEach { if (it is PlayerFragment) remove(it) }
        }
    }

    /**
     * collapsed or shows the FrameView containing the PlayerFragment
     * @param collapse: specifies if the player should be collapsed
     * @param duration: duration of the animation
     */
    private fun changePlayerCollapse(collapse: Boolean, duration: Long = 1000) = lifecycleScope.launch {
        current_song_container.animation.awaitEnd()
        if (mViewModel.playerCollapsed == collapse) return@launch
        Timber.d("Changing player collapse to $collapse")
        val animation = current_song_container.animate().setDuration(duration)
        val translateBy = current_song_container.height.toFloat()
        if (!mViewModel.playerCollapsed) animation.translationYBy(translateBy)
            .withEndAction { current_song_container.visibility = View.GONE }
            .start()
        else animation.translationYBy(-translateBy)
            .withStartAction { current_song_container.visibility = View.VISIBLE }
            .start()
        mViewModel.playerCollapsed = collapse
    }

    /**
     * collapsed or shows the BottomNavigation
     * @param collapse: specifies if the navigation should be collapsed
     * @param duration: duration of the animation
     */
    private fun changeBottomNavCollapse(collapse: Boolean, duration: Long = 1000) = lifecycleScope.launch {
        bottom_navigation.animation.awaitEnd()
        if (mViewModel.bottomNavCollapsed == collapse) return@launch
        Timber.d("Changing bottom nav collapse to $collapse")
        val animation = bottom_navigation.animate().setDuration(duration)
        val translateBy = bottom_navigation.height.toFloat()
        if (!mViewModel.bottomNavCollapsed) animation.translationYBy(translateBy)
            .withEndAction { bottom_navigation.visibility = View.GONE }
            .start()
        else animation.translationYBy(-translateBy)
            .withStartAction { bottom_navigation.visibility = View.VISIBLE }
            .start()
        mViewModel.bottomNavCollapsed = collapse
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_CURRENT_SERVER, AppSettings.getLatestServer())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        JMusicBot.connectionListeners.remove(mConnectionListener)
    }
}
