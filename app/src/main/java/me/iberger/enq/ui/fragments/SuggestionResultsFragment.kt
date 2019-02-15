package me.iberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.ItemTouchHelper
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.fragments.parents.ResultsFragment
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.setupSwipeActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.KEY_SUGGESTER_ID
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.model.Permissions
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
        getSuggestions()
        val canDisklike = JMusicBot.user!!.permissions.contains(Permissions.DISLIKE)
        setupSwipeActions(
            context!!, Queue, this,
            CommunityMaterial.Icon2.cmd_star, R.color.favorites,
            if (canDisklike) CommunityMaterial.Icon.cmd_delete else null, R.color.delete
        )
        loadingHeader.add(ProgressItem())
    }

    private fun getSuggestions() = mBackgroundScope.launch {
        Timber.d("Getting suggestions for suggester $mSuggesterId")
        super.displayResults(JMusicBot.suggestions(mSuggesterId))
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = resultsAdapter.getAdapterItem(position)
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
