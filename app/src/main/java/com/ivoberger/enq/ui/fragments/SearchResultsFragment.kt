package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.core.os.bundleOf
import com.ivoberger.enq.ui.fragments.parents.ResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_PROVIDER_ID
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchResultsFragment : ResultsFragment() {

    companion object {
        fun newInstance(providerID: String) = SearchResultsFragment().apply {
            arguments = bundleOf(KEY_PROVIDER_ID to providerID)
        }
    }

    private lateinit var mProviderID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderID = arguments!!.getString(KEY_PROVIDER_ID)!!
        Timber.d("Creating SearchResultFragment with provider $mProviderID")
    }

    fun search(query: String) {
        Timber.d("Searching for $query on provider $mProviderID")
        mMainScope.launch {
            loadingHeader.add(ProgressItem())
            songAdapter.clear()
        }
        mBackgroundScope.launch {
            val results = JMusicBot.search(mProviderID, query)
            Timber.d("Got ${results.size} results on provider $mProviderID")
            super.displayResults(results)
        }
    }
}
