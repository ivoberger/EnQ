package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import androidx.core.os.bundleOf
import com.ivoberger.enq.ui.fragments.parents.ResultsFragment
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_PROVIDER_ID
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

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

    fun startSearch() = mBackgroundScope.launch {
        if (mCurrentQuery.isBlank()) return@launch
        mLastQuery = mCurrentQuery
        if (loadingHeader.adapterItemCount == 0) withContext(mMainScope.coroutineContext) {
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
        Timber.d("DETACHING")
        mQueryChanged = true
        mMainScope.coroutineContext.cancel()
        mBackgroundScope.coroutineContext.cancel()
        super.onDetach()
    }
}
