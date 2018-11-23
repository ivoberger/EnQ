package me.iberger.enq.gui.fragments

import android.os.Bundle
import androidx.core.os.bundleOf
import kotlinx.coroutines.launch
import me.iberger.enq.gui.adapter.SuggestionsItem
import me.iberger.jmusicbot.KEY_PROVIDER_ID
import me.iberger.jmusicbot.MusicBot
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
        mBackgroundScope.launch {
            val results = MusicBot.instance.search(mProviderID, query).await()
            Timber.d("Got ${results.size} results on provider $mProviderID")
            super.displayResults(results.map { SuggestionsItem(it) })
        }
    }
}