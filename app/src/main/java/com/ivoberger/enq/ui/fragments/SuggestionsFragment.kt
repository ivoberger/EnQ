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
package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.fragments.base.TabbedResultsFragment
import com.ivoberger.enq.utils.retryOnError
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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
