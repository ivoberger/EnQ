package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.fragments.parents.ResultsFragment
import com.ivoberger.enq.ui.items.ResultItem
import com.ivoberger.enq.utils.changeFavoriteStatus
import com.ivoberger.enq.utils.toastShort
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.KEY_SUGGESTER_ID
import com.ivoberger.jmusicbot.exceptions.AuthException
import com.ivoberger.jmusicbot.model.Permissions
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SuggestionResultsFragment : ResultsFragment(), SimpleSwipeCallback.ItemSwipeCallback {

    companion object {
        fun newInstance(suggesterId: String) = SuggestionResultsFragment().apply {
            arguments = bundleOf(KEY_SUGGESTER_ID to suggesterId)
        }
    }

    private lateinit var mSuggesterId: String
    private val mCanDisklike = JMusicBot.user!!.permissions.contains(Permissions.DISLIKE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSuggesterId = arguments!!.getString(KEY_SUGGESTER_ID)!!
        Timber.d("Creating SuggestionResultFragment with suggester $mSuggesterId")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingHeader.add(ProgressItem())
        getSuggestions()
        if (mCanDisklike) fastAdapter.onLongClickListener = { _, _, item: ResultItem, position: Int ->
            if (JMusicBot.isConnected) {
                mBackgroundScope.launch {
                    JMusicBot.deleteSuggestion(mSuggesterId, item.model)
                    withContext(mMainScope.coroutineContext) { songAdapter.remove(position) }
                }
                true
            } else false
        }
    }

    private fun getSuggestions() = mBackgroundScope.launch {
        Timber.d("Getting suggestions for suggester $mSuggesterId")
        displayResults(JMusicBot.suggestions(mSuggesterId))
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = songAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    try {
                        JMusicBot.deleteSuggestion(mSuggesterId, entry.model)
                        getSuggestions()
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}")
                        withContext(Dispatchers.Main) {
                            context!!.toastShort(R.string.msg_no_permission)
                            fastAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    changeFavoriteStatus(context!!, entry.model)
                    withContext(Dispatchers.Main) {
                        fastAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }
}
