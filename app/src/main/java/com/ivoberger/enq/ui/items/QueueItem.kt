package com.ivoberger.enq.ui.items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.persistence.GlideApp
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

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val context = holder.itemView.context

        holder.txtTitle.isSelected = true
        holder.txtDescription.isSelected = true
        holder.txtTitle.text = song.title
        holder.txtDescription.text = song.description

        song.albumArtUrl?.also { GlideApp.with(holder.itemView).load(it).into(holder.imgAlbumArt) }
        song.duration?.also {
            holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60)
        }
        holder.txtChosenBy.setText(R.string.txt_suggested)
        model.userName.also { holder.txtChosenBy.text = it }

        holder.txtDuration.compoundDrawablePadding = 20
        if (song in Configuration.favorites) holder.txtDuration.setCompoundDrawables(
            context.icon(CommunityMaterial.Icon2.cmd_star)
                .color(context.secondaryColor()).sizeDp(10),
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
            oldItem: QueueItem, oldItemPosition: Int, newItem: QueueItem, newItemPosition: Int
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
