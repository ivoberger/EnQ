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
package com.ivoberger.enq.utils

import coil.api.load
import com.ivoberger.enq.ui.items.QueueItem
import com.ivoberger.jmusicbot.client.model.Song
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial

fun Song.bindView(holder: QueueItem.ViewHolder) {
    val ctx = holder.itemView.context
    holder.txtTitle.isSelected = true
    holder.txtDescription.isSelected = true
    holder.txtTitle.text = this.title
    holder.txtDescription.text = this.description

    holder.imgAlbumArt.load(albumArtUrl) {
        placeholder(ctx.icon(CommunityMaterial.Icon.cmd_album).color(IconicsColor.colorInt(ctx.onPrimaryColor())))
    }
    this.duration?.let { holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60) }
}
