package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.fragments.parents.TabbedResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.*
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber


@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class SearchFragment : TabbedResultsFragment(), ConnectionChangeListener {

    private val mSearchView: SearchView by lazy { (activity as MainActivity).searchView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = lifecycleScope.async(Dispatchers.IO) { JMusicBot.getProvider() }
        JMusicBot.connectionListeners.add(this@SearchFragment)
        lifecycleScope.launch(Dispatchers.IO) {
            mProviderPlugins.await() ?: return@launch
            Configuration.lastProvider?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }

        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var oldQuery = ""
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query == oldQuery) return true
                query?.also {
                    oldQuery = it
                    search(it)
                }
                return true
            }

            override fun onQueryTextChange(newQuery: String?): Boolean {
                if (newQuery == oldQuery) return true
                newQuery?.also { oldQuery = it }
                // debounce
                lifecycleScope.launch(Dispatchers.IO) {
                    delay(300)
                    if (oldQuery != newQuery) return@launch
                    search(oldQuery)
                }
                return true
            }
        })
    }

    override fun initializeTabs() {
        lifecycleScope.launch(Dispatchers.IO) {
            mProviderPlugins.await() ?: return@launch
            mFragmentPagerAdapter = async {
                SearchFragmentPager(childFragmentManager, mProviderPlugins.await()!!)
            }
            withContext(Dispatchers.Main) { view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    fun search(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (query.isNotBlank()) (mFragmentPagerAdapter.await() as SearchFragmentPager).search(query)
        }
    }

    override fun onTabSelected(position: Int) {
        lifecycleScope.launch(Dispatchers.IO) { mFragmentPagerAdapter.await().onTabSelected(position) }
    }

    override fun onConnectionLost(e: Exception?) {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onConnectionRecovered() {}

    override fun onDestroy() {
        super.onDestroy()
        Configuration.lastProvider = mSelectedPlugin
        JMusicBot.connectionListeners.remove(this)
    }

    inner class SearchFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedResultsFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment {
            val fragment = SearchResultsFragment.newInstance(provider[position].id)
            resultFragments.add(position, fragment)
            return fragment
        }

        fun search(query: String) {
            Timber.d("Searching for $query")
            resultFragments.forEach { (it as SearchResultsFragment).setQuery(query) }
            (resultFragments[view_pager.currentItem] as SearchResultsFragment).startSearch()
        }

        override fun onTabSelected(position: Int) {
            super.onTabSelected(position)
            (resultFragments[position] as SearchResultsFragment).startSearch()
        }
    }
}
