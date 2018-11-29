package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.gui.fragments.parents.ResultsFragment
import me.iberger.enq.gui.items.SuggestionsItem
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.setupSwipeActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.KEY_SUGGESTER_ID
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.exceptions.AuthException
import timber.log.Timber

class SuggestionResultsFragment : ResultsFragment(), SimpleSwipeCallback.ItemSwipeCallback {

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
        getSuggestions()
        setupSwipeActions(
            context!!, queue, this,
            CommunityMaterial.Icon2.cmd_star, R.color.favorites,
            CommunityMaterial.Icon.cmd_delete, R.color.delete
        )
    }

    private fun getSuggestions() = mBackgroundScope.launch {
        val results = MusicBot.instance?.getSuggestions(mSuggesterId)?.await()
        super.displayResults(results?.map { SuggestionsItem(it) })
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    try {
                        MusicBot.instance?.deleteSuggestion(mSuggesterId, entry.song)?.await()
                        getSuggestions()
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}")
                        withContext(Dispatchers.Main) {
                            context!!.toastShort(R.string.msg_no_permission)
                            mFastItemAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    changeFavoriteStatus(context!!, entry.song)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }
}