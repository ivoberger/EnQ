package com.ivoberger.enq.utils

import com.ivoberger.enq.persistence.GlideApp
import com.ivoberger.enq.ui.items.QueueItem
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial

fun Song.bindView(holder: QueueItem.ViewHolder) {
    val ctx = holder.itemView.context
    holder.txtTitle.isSelected = true
    holder.txtDescription.isSelected = true
    holder.txtTitle.text = this.title
    holder.txtDescription.text = this.description

    GlideApp.with(holder.itemView)
        .load(this.albumArtUrl)
        .placeholder(ctx.icon(CommunityMaterial.Icon.cmd_album).color(ctx.onPrimaryColor()))
        .into(holder.imgAlbumArt)
    this.duration?.let { holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60) }
}