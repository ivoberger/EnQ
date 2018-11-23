package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import kotlinx.coroutines.launch
import me.iberger.enq.gui.adapter.SuggestionsItem
import me.iberger.jmusicbot.KEY_SUGGESTER_ID
import me.iberger.jmusicbot.MusicBot
import timber.log.Timber

class SuggestionResultsFragment : ResultsFragment() {

    companion object {
        fun newInstance(suggesterId: String) = SuggestionResultsFragment().apply {
            arguments = bundleOf(KEY_SUGGESTER_ID to suggesterId)
        }
    }

    private lateinit var mSuggesterId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSuggesterId = arguments!!.getString(KEY_SUGGESTER_ID)!!
        Timber.d("Creating SuggestionResultFragment with suggester $mSuggesterId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Getting suggestions for suggester $mSuggesterId")
        mBackgroundScope.launch {
            val results = MusicBot.instance.getSuggestions(mSuggesterId).await()
            super.displayResults(results.map { SuggestionsItem(it) })
        }
    }
}