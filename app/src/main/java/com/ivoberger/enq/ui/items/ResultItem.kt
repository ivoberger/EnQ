package com.ivoberger.enq.ui.items

import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial

class ResultItem(song: Song) : SongItem(song) {

    override fun bindView(holder: QueueItem.ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val context = holder.itemView.context

        holder.txtDuration.compoundDrawablePadding = 20
        if (model in AppSettings.favorites) holder.txtDuration.setCompoundDrawables(
            context.icon(CommunityMaterial.Icon2.cmd_star)
                .color(context.secondaryColor()).sizeDp(10),
            null, null, null
        )
    }
}
