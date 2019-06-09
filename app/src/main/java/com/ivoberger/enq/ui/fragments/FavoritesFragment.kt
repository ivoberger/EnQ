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
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ItemTouchHelper
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.fragments.base.SongListFragment
import com.ivoberger.enq.ui.items.SongItem
import com.ivoberger.enq.utils.attributeColor
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.sizeDp
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.toast.toast

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class FavoritesFragment : SongListFragment<SongItem>(), SimpleSwipeCallback.ItemSwipeCallback {

    override val songAdapter: ModelAdapter<Song, SongItem> by lazy {
        ModelAdapter { element: Song -> SongItem(element) }

    }
    override val isRemoveAfterEnQ = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppSettings.getFavoritesLiveData().observe(this) { favorites ->
            FastAdapterDiffUtil[songAdapter] = favorites.map { SongItem(it) }
        }
        songAdapter.add(AppSettings.favorites)

        val swipeToDeleteIcon =
            context!!.icon(CommunityMaterial.Icon.cmd_delete).color(IconicsColor.colorInt(context!!.onPrimaryColor()))
                .sizeDp(24)
        val swipeToDeleteColor = context!!.attributeColor(R.attr.colorDelete)

        ItemTouchHelper(
            SimpleSwipeCallback(
                this, swipeToDeleteIcon, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, swipeToDeleteColor
            ).withLeaveBehindSwipeRight(swipeToDeleteIcon).withBackgroundSwipeRight(swipeToDeleteColor)
        ).attachToRecyclerView(recycler_queue)
    }

    override fun onEntryClicked(item: SongItem, position: Int): Boolean {
        lifecycleScope.launch(Dispatchers.IO) {
            val providerIds = JMusicBot.getProvider().map { it.id }
            if (providerIds.contains(item.model.provider.id)) super.onEntryClicked(item, position)
            else withContext(Dispatchers.Main) {
                (activity as MainActivity).search(item.model.title)
                toast(R.string.msg_provider_not_found)
            }
        }
        return true
    }

    override fun itemSwiped(position: Int, direction: Int) {
        val item = songAdapter.getAdapterItem(position)
        if (direction == ItemTouchHelper.RIGHT || direction == ItemTouchHelper.LEFT) {
            AppSettings.changeFavoriteStatus(context!!, item.model)
        }
    }
}
