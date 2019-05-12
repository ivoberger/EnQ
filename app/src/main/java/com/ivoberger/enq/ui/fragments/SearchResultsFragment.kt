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
import androidx.core.os.bundleOf
import com.ivoberger.enq.ui.fragments.base.ResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_PROVIDER_ID
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class SearchResultsFragment : ResultsFragment() {

    companion object {
        fun newInstance(providerID: String) = SearchResultsFragment().apply {
            arguments = bundleOf(KEY_PROVIDER_ID to providerID)
        }
    }

    private lateinit var mProviderID: String
    private var mQueryChanged = false
    private var mCurrentQuery = ""
    private var mLastQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderID = arguments!!.getString(KEY_PROVIDER_ID)!!
        Timber.d("Creating SearchResultFragment with provider $mProviderID")
    }

    fun setQuery(query: String) {
        mCurrentQuery = query
    }

    fun startSearch() = lifecycleScope.launch(Dispatchers.IO) {
        if (mCurrentQuery.isBlank()) return@launch
        mLastQuery = mCurrentQuery
        if (loadingHeader.adapterItemCount == 0) withContext(Dispatchers.Main) {
            songAdapter.clear()
            loadingHeader.add(0, ProgressItem())
        }
        val results = search()
        if (mLastQuery == mCurrentQuery) {
            Timber.d("Setting results for $mCurrentQuery")
            super.displayResults(results)
        } else Timber.d("Cancelled search for $mCurrentQuery")
        mQueryChanged = false
    }

    private suspend fun search(): List<Song> {
        Timber.d("Searching for $mCurrentQuery on provider $mProviderID")
        val results = JMusicBot.search(mProviderID, mCurrentQuery)
        Timber.d("Got ${results.size} results for query $mCurrentQuery on provider $mProviderID")
        return results
    }

    override fun onDetach() {
        Timber.d("Detaching $mProviderID")
        mQueryChanged = true
        super.onDetach()
    }
}
