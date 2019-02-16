package me.iberger.enq.ui.items

import com.mikepenz.community_material_typeface_library.CommunityMaterial
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.utils.icon
import me.iberger.jmusicbot.model.Song
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
