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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.items.QueueItem
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.attributeColor
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.exceptions.AuthException
import com.ivoberger.jmusicbot.client.model.Permissions
import com.ivoberger.jmusicbot.client.model.QueueEntry
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.swipe_drag.SimpleSwipeDragCallback
import com.mikepenz.fastadapter.utils.DragDropUtil
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.resources.color
import splitties.toast.toast
import timber.log.Timber

class QueueFragment : Fragment(R.layout.fragment_queue), SimpleSwipeCallback.ItemSwipeCallback,
        ItemTouchCallback {

    private val KEY_QUEUE = "queue"

    private val mViewModel: MainViewModel by viewModels({ activity!! })
    private val mFastItemAdapter: FastItemAdapter<QueueItem> by lazy { FastItemAdapter<QueueItem>() }
    private val mQueueUpdateChannel = Channel<List<QueueEntry>>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Creating Queue Fragment")
        super.onCreate(savedInstanceState)
        updateQueue()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.queue.observe(this) { if (it.isSuccess) mQueueUpdateChannel.sendBlocking(it.get()!!) }

        recycler_queue.layoutManager = LinearLayoutManager(context).apply { reverseLayout = true }
        recycler_queue.adapter = mFastItemAdapter
        savedInstanceState?.also { mFastItemAdapter.withSavedInstanceState(it, KEY_QUEUE) }

        val deleteDrawable =
                context!!.icon(CommunityMaterial.Icon2.cmd_star)
                        .color(IconicsColor.colorInt(onPrimaryColor())).size(
                                IconicsSize.dp(24)
                        )
        val favoritesDrawable =
                context!!.icon(CommunityMaterial.Icon.cmd_delete)
                        .color(IconicsColor.colorInt(onPrimaryColor())).size(IconicsSize.dp(24))

        // enable swipe and drag actions depending on the users permissions
        JMusicBot.user?.let {
            val userPermissions = it.permissions
            val touchCallback = if (userPermissions.contains(Permissions.MOVE))
                SimpleSwipeDragCallback(
                        this,
                        this,
                        deleteDrawable,
                        ItemTouchHelper.LEFT,
                        attributeColor(R.attr.colorFavorite)
                ) else SimpleSwipeCallback(
                    this,
                    deleteDrawable,
                    ItemTouchHelper.LEFT,
                    attributeColor(R.attr.colorDelete)
            )

            when (touchCallback) {
                is SimpleSwipeCallback -> touchCallback.withBackgroundSwipeRight(color(R.color.deleteColor))
                        .withLeaveBehindSwipeRight(favoritesDrawable)
                is SimpleSwipeDragCallback -> touchCallback.withBackgroundSwipeRight(color(R.color.deleteColor))
                        .withLeaveBehindSwipeRight(favoritesDrawable)
            }
            ItemTouchHelper(touchCallback).attachToRecyclerView(recycler_queue)
        }
    }

    private fun updateQueue() = lifecycleScope.launch(Dispatchers.Default) {
        for (newQueue in mQueueUpdateChannel) {
            if (isHidden) continue
            val diff = FastAdapterDiffUtil.calculateDiff(
                    mFastItemAdapter.itemAdapter,
                    newQueue.map { QueueItem(it) },
                    QueueItem.DiffCallback()
            )
            withContext(Dispatchers.Main) {
                FastAdapterDiffUtil.set(
                        mFastItemAdapter.itemAdapter,
                        diff
                )
            }
        }
    }

    override fun itemSwiped(position: Int, direction: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    if (!JMusicBot.currentState.isConnected) return@launch
                    try {
                        JMusicBot.dequeue(entry.song)
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}, message ${e.message}")
                        withContext(Dispatchers.Main) {
                            context!!.toast(R.string.msg_no_permission)
                            mFastItemAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    AppSettings.changeFavoriteStatus(context!!, entry.song)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        if (!JMusicBot.currentState.isConnected) return false
        DragDropUtil.onMove(mFastItemAdapter.itemAdapter, oldPosition, newPosition)
        return true
    }

    override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
        if (!JMusicBot.currentState.isConnected) return
        lifecycleScope.launch(Dispatchers.IO) {
            val entry = mFastItemAdapter.getAdapterItem(newPosition).model
            Timber.d("Moved ${entry.song.title} from $oldPosition to $newPosition")
            try {
                JMusicBot.moveEntry(entry, entry.song.provider.id, entry.song.id, newPosition)
            } catch (e: Exception) {
                Timber.e(e)
                lifecycleScope.launch { context?.toast(R.string.msg_no_permission) }
            }
        }
    }
}
