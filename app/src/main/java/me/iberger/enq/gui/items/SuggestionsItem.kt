package me.iberger.enq.gui.items

import androidx.core.content.ContextCompat
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.data.Song

class SuggestionsItem(song: Song) : SongItem(song) {


    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val context = holder.itemView.context

        holder.txtDuration.compoundDrawablePadding = 20
        if (song in MainActivity.favorites) holder.txtDuration.setCompoundDrawables(
            IconicsDrawable(context, CommunityMaterial.Icon2.cmd_star).color(
                ContextCompat.getColor(context, R.color.colorAccent)
            ).sizeDp(10), null, null, null
        )
    }
}