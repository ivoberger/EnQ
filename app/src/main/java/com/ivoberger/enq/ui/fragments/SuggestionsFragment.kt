package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.ui.fragments.parents.TabbedResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SuggestionsFragment : TabbedResultsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = mBackgroundScope.async { JMusicBot.getSuggesters() }
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            Configuration.lastSuggester?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }
    }

    override fun initializeTabs() {
        mBackgroundScope.launch {
            val provider = JMusicBot.getSuggesters()
            mFragmentPagerAdapter =
                async {
                    SuggestionsFragmentPager(childFragmentManager, provider)
                }
            mMainScope.launch { view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Configuration.lastSuggester = mSelectedPlugin
    }

    inner class SuggestionsFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedResultsFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SuggestionResultsFragment.newInstance(provider[position].id)
    }
}
