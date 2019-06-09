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
package com.ivoberger.enq.ui.items

import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.sizeDp
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial

class ResultItem(song: Song) : SongItem(song) {

    override fun bindView(holder: QueueItem.ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val ctx = holder.itemView.context

        holder.txtDuration.compoundDrawablePadding = 20
        if (model in AppSettings.favorites) holder.txtDuration.setCompoundDrawables(
            ctx.icon(CommunityMaterial.Icon2.cmd_star)
                .color(IconicsColor.colorInt(ctx.secondaryColor())).sizeDp(10),
            null, null, null
        )
    }
}
