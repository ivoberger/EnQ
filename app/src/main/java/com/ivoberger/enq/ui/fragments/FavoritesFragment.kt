package com.ivoberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.fragments.parents.SongListFragment
import com.ivoberger.enq.ui.items.SongItem
import com.ivoberger.enq.utils.attributeColor
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.launch
import splitties.toast.toast

class FavoritesFragment : SongListFragment<SongItem>(), SimpleSwipeCallback.ItemSwipeCallback {

    override val songAdapter: ModelAdapter<Song, SongItem> by lazy {
        ModelAdapter { element: Song -> SongItem(element) }
    }
    override val isRemoveAfterEnQ = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Configuration.getFavoritesLiveData().observe(this, Observer { favorites ->
            FastAdapterDiffUtil[songAdapter] = favorites.map { SongItem(it) }
        })
        songAdapter.add(Configuration.favorites)

        val swipeToDeleteIcon =
            context!!.icon(CommunityMaterial.Icon.cmd_delete).color(context!!.onPrimaryColor()).sizeDp(24)
        val swipeToDeleteColor = context!!.attributeColor(R.attr.colorDelete)

        ItemTouchHelper(
            SimpleSwipeCallback(
                this, swipeToDeleteIcon, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, swipeToDeleteColor
            ).withLeaveBehindSwipeRight(swipeToDeleteIcon).withBackgroundSwipeRight(swipeToDeleteColor)
        ).attachToRecyclerView(recycler_queue)
    }

    override fun onEntryClicked(item: SongItem, position: Int): Boolean {
        mBackgroundScope.launch {
            val providerIds = JMusicBot.getProvider().map { it.id }
            if (providerIds.contains(item.model.provider.id)) super.onEntryClicked(item, position)
            else mMainScope.launch {
                (activity as MainActivity).search(item.model.title)
                toast(R.string.msg_provider_not_found)
            }
        }
        return true
    }

    override fun itemSwiped(position: Int, direction: Int) {
        val item = songAdapter.getAdapterItem(position)
        if (direction == ItemTouchHelper.RIGHT || direction == ItemTouchHelper.LEFT) {
            Configuration.changeFavoriteStatus(context!!, item.model)
        }
    }
}
