package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.iberger.enq.gui.fragments.parents.TabbedSongListFragment
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin

class SuggestionsFragment : TabbedSongListFragment() {
    companion object {
        fun newInstance() = SuggestionsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = mBackgroundScope.async { MusicBot.instance.suggesters }
        mBackgroundScope.launch {
            mConfig.lastSuggester?.also {
                if (mProviderPlugins.await().contains(it)) mSelectedPlugin = it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBackgroundScope.launch {
            mFragmentPagerAdapter =
                    async {
                        SuggestionsFragmentPager(
                            childFragmentManager,
                            mProviderPlugins.await()
                        )
                    }
            mUIScope.launch { search_view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mConfig.lastSuggester = mSelectedPlugin
    }

    inner class SuggestionsFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedSongListFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SuggestionResultsFragment.newInstance(provider[position].id)
    }
}