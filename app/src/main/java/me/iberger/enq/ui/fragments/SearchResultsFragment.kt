package me.iberger.enq.ui.fragments

import android.os.Bundle
import androidx.core.os.bundleOf
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.launch
import me.iberger.enq.ui.fragments.parents.ResultsFragment
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.KEY_PROVIDER_ID
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
            resultsAdapter.clear()
        }
        mBackgroundScope.launch {
            val results = JMusicBot.search(mProviderID, query)
            Timber.d("Got ${results.size} results on provider $mProviderID")
            super.displayResults(results)
        }
    }
}
