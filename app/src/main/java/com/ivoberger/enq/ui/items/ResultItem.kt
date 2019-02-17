package com.ivoberger.enq.ui.items

import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.utils.icon
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import splitties.resources.color

class ResultItem(song: Song) : SongItem(song) {

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val context = holder.itemView.context

        holder.txtDuration.compoundDrawablePadding = 20
        if (model in MainActivity.favorites) holder.txtDuration.setCompoundDrawables(
            context.icon(CommunityMaterial.Icon2.cmd_star)
                .color(context.color(R.color.colorAccent)).sizeDp(10),
            null, null, null
        )
    }
}
