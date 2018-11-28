package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.backend.Configuration
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin
import timber.log.Timber

class SearchFragment : TabbedSongListFragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProvider = mBackgroundScope.async { MusicBot.instance.provider }

        val searchView =
            ((activity as MainActivity).optionsMenu.findItem(R.id.app_bar_search).actionView as SearchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBackgroundScope.launch {
            mFragmentPagerAdapter =
                    async { SearchFragmentPager(childFragmentManager, mProvider.await()) }
            mUIScope.launch { search_view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    fun search(query: String) {
        mBackgroundScope.launch {
            (mFragmentPagerAdapter.await() as SearchFragmentPager).search(
                query
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Configuration(context!!).lastProvider = mSelectedProvider
    }

    class SearchFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedSongListFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SearchResultsFragment.newInstance(provider[position].id)

        fun search(query: String) {
            Timber.d("Searching for $query")
            resultFragments.forEach { (it as SearchResultsFragment).search(query) }
        }
    }
}