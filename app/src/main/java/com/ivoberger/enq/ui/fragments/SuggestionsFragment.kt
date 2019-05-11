package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.fragments.base.TabbedResultsFragment
import com.ivoberger.enq.utils.retryOnError
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class SuggestionsFragment : TabbedResultsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = lifecycleScope.async { retryOnError { JMusicBot.getSuggesters() } }
        lifecycleScope.launch(Dispatchers.IO) {
            mProviderPlugins.await() ?: return@launch
            AppSettings.lastSuggester?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }
    }

    override fun initializeTabs() {
        lifecycleScope.launch {
            val provider = JMusicBot.getSuggesters()
            mFragmentPagerAdapter = async(Dispatchers.Default) {
                SuggestionsFragmentPager(childFragmentManager, provider)
            }
            view_pager.adapter = mFragmentPagerAdapter.await()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppSettings.lastSuggester = mSelectedPlugin
    }

    inner class SuggestionsFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedResultsFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SuggestionResultsFragment.newInstance(provider[position].id)
    }
}
