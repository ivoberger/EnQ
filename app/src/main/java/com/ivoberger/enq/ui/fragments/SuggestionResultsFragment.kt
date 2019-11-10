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
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.fragments.base.ResultsFragment
import com.ivoberger.enq.ui.items.ResultItem
import com.ivoberger.enq.utils.tryWithErrorToast
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.model.Permissions
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.toast.toast
import timber.log.Timber

class SuggestionResultsFragment : ResultsFragment(), SimpleSwipeCallback.ItemSwipeCallback {

    companion object {
        const val KEY_SUGGESTER_ID = "suggesterId"
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
        if (mCanDislike) fastAdapter.onLongClickListener =
                { _, _, item: ResultItem, position: Int ->
                    if (JMusicBot.currentState.isConnected) {
                        lifecycleScope.launch {
                            tryWithErrorToast { JMusicBot.deleteSuggestion(mSuggesterId, item.model) }
                            songAdapter.remove(position)
                        }
                        true
                    } else false
                }
    }

    private fun getSuggestions() = lifecycleScope.launch(Dispatchers.IO) {
        Timber.d("Getting suggestions for suggester $mSuggesterId")
        val suggestions = tryWithErrorToast(listOf()) { JMusicBot.suggestions(mSuggesterId) }
        displayResults(suggestions)
    }

    override fun itemSwiped(position: Int, direction: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = songAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    tryWithErrorToast {
                        try {
                            JMusicBot.deleteSuggestion(mSuggesterId, entry.model)
                            getSuggestions()
                        } catch (e: AuthException) {
                            Timber.e("AuthException with reason ${e.reason}")
                            withContext(Dispatchers.Main) {
                                context!!.toast(R.string.msg_no_permission)
                                fastAdapter.notifyAdapterItemChanged(position)
                            }
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    AppSettings.changeFavoriteStatus(context!!, entry.model)
                    withContext(Dispatchers.Main) {
                        fastAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }
}
