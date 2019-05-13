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

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.utils.bindView
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.jmusicbot.model.QueueEntry
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.drag.IDraggable
import com.mikepenz.fastadapter.items.ModelAbstractItem

class QueueItem(
    queueEntry: QueueEntry,
    val song: Song = queueEntry.song
) :
    ModelAbstractItem<QueueEntry, QueueItem.ViewHolder>(queueEntry), IDraggable {

    override var identifier: Long = song.id.hashCode().toLong()
    override val type: Int = R.id.queue_entry
    override val layoutRes: Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = ViewHolder(v)
    override var isDraggable = true

    override fun toString(): String = model.toString()

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        song.bindView(holder)
        val ctx = holder.itemView.context
        holder.txtChosenBy.setText(R.string.txt_suggested)
        model.userName.also { holder.txtChosenBy.text = it }

        holder.txtDuration.compoundDrawablePadding = 20
        if (song in AppSettings.favorites) holder.txtDuration.setCompoundDrawables(
            ctx.icon(CommunityMaterial.Icon2.cmd_star).color(ctx.secondaryColor()).sizeDp(10),
            null, null, null
        )
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imgAlbumArt: ImageView = view.findViewById(R.id.song_album_art)
        var txtTitle: TextView = view.findViewById(R.id.song_title)
        var txtDescription: TextView = view.findViewById(R.id.song_description)
        var txtChosenBy: TextView = view.findViewById(R.id.song_chosen_by)
        var txtDuration: TextView = view.findViewById(R.id.song_duration)
    }

    class DiffCallback : com.mikepenz.fastadapter.diff.DiffCallback<QueueItem> {
        override fun getChangePayload(
            oldItem: QueueItem,
            oldItemPosition: Int,
            newItem: QueueItem,
            newItemPosition: Int
        ): Any? = null

        override fun areItemsTheSame(oldItem: QueueItem, newItem: QueueItem): Boolean {
            val oldEntry = oldItem.model
            val newEntry = newItem.model

            return oldEntry.song.id == newEntry.song.id && oldEntry.userName == newEntry.userName
        }

        override fun areContentsTheSame(oldItem: QueueItem, newItem: QueueItem): Boolean =
            oldItem.model == newItem.model
    }
}
