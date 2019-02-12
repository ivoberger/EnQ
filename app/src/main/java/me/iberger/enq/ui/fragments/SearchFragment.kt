package me.iberger.enq.ui.fragments

import android.os.Bundle
import android.widget.SearchView
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.fragments.parents.TabbedSongListFragment
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import me.iberger.jmusicbot.model.MusicBotPlugin
import timber.log.Timber

@ContentView(R.layout.fragment_search)
class SearchFragment : TabbedSongListFragment(), ConnectionChangeListener {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private lateinit var mSearchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = mBackgroundScope.async { JMusicBot.getProvider() }
        JMusicBot.connectionChangeListeners.add(this@SearchFragment)
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            mConfig.lastProvider?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }

        mSearchView =
                (activity as MainActivity).optionsMenu.findItem(R.id.app_bar_search).actionView as SearchView

        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var oldText = ""
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.also {
                    oldText = it
                    search(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText == oldText) return true
                newText?.also { oldText = it }
                mBackgroundScope.launch {
                    delay(300)
                    if (oldText != newText) return@launch
                    search(oldText)
                }
                return true
            }
        })
    }

    override fun initializeTabs() {
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            mFragmentPagerAdapter = async {
                SearchFragmentPager(childFragmentManager, mProviderPlugins.await()!!)
            }
            mMainScope.launch { view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    fun search(query: String) {
        mBackgroundScope.launch {
            (mFragmentPagerAdapter.await() as SearchFragmentPager).search(query)
        }
    }

    override fun onConnectionLost(e: Exception) {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onConnectionRecovered() {}

    override fun onDestroy() {
        super.onDestroy()
        mConfig.lastProvider = mSelectedPlugin
        JMusicBot.connectionChangeListeners.remove(this)
    }

    inner class SearchFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedSongListFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SearchResultsFragment.newInstance(provider[position].id)

        fun search(query: String) {
            Timber.d("Searching for $query")
            resultFragments.forEach { (it as SearchResultsFragment).search(query) }
        }
    }
}
