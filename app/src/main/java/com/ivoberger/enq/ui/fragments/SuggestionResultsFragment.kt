package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.ui.fragments.parents.ResultsFragment
import com.ivoberger.enq.ui.items.ResultItem
import com.ivoberger.enq.utils.tryWithErrorToast
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_SUGGESTER_ID
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.model.Permissions
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.toast.toast
import timber.log.Timber

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class SuggestionResultsFragment : ResultsFragment(), SimpleSwipeCallback.ItemSwipeCallback {

    companion object {
        fun newInstance(suggesterId: String) = SuggestionResultsFragment().apply {
            arguments = bundleOf(KEY_SUGGESTER_ID to suggesterId)
        }
    }

    private lateinit var mSuggesterId: String
    private val mCanDislike = JMusicBot.user!!.permissions.contains(Permissions.DISLIKE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSuggesterId = arguments!!.getString(KEY_SUGGESTER_ID)!!
        Timber.d("Creating SuggestionResultFragment with suggester $mSuggesterId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingHeader.add(ProgressItem())
        getSuggestions()
        if (mCanDislike) fastAdapter.onLongClickListener = { _, _, item: ResultItem, position: Int ->
            if (JMusicBot.isConnected) {
                lifecycleScope.launch(Dispatchers.IO) {
                    tryWithErrorToast { runBlocking { JMusicBot.deleteSuggestion(mSuggesterId, item.model) } }
                    withContext(Dispatchers.Main) { songAdapter.remove(position) }
                }
                true
            } else false
        }
    }

    private fun getSuggestions() = lifecycleScope.launch(Dispatchers.IO) {
        Timber.d("Getting suggestions for suggester $mSuggesterId")
        val suggestions = tryWithErrorToast(listOf()) {
            runBlocking { JMusicBot.suggestions(mSuggesterId) }
        }
        displayResults(suggestions)
    }

    override fun itemSwiped(position: Int, direction: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = songAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    tryWithErrorToast {
                        try {
                            runBlocking { JMusicBot.deleteSuggestion(mSuggesterId, entry.model) }
                            getSuggestions()
                        } catch (e: AuthException) {
                            Timber.e("AuthException with reason ${e.reason}")
                            lifecycleScope.launch {
                                context!!.toast(R.string.msg_no_permission)
                                fastAdapter.notifyAdapterItemChanged(position)
                            }
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    Configuration.changeFavoriteStatus(context!!, entry.model)
                    withContext(Dispatchers.Main) {
                        fastAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }
}
