package me.iberger.enq.gui.items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter_extensions.drag.IDraggable
import com.mikepenz.iconics.IconicsDrawable
import com.squareup.picasso.Picasso
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.data.Song

class QueueItem(
    val queueEntry: QueueEntry,
    val song: Song = queueEntry.song
) :
    AbstractItem<QueueItem, QueueItem.ViewHolder>(), IDraggable<QueueItem, QueueItem> {
    override fun getType(): Int = R.id.queue_entry

    override fun getLayoutRes(): Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = ViewHolder(v)
    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        val context = holder.itemView.context

        holder.txtTitle.isSelected = true
        holder.txtDescription.isSelected = true
        holder.txtTitle.text = song.title
        holder.txtDescription.text = song.description
        song.albumArtUrl?.also { Picasso.get().load(it).into(holder.imgAlbumArt) }
        song.duration?.also {
            holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60)
        }
        holder.txtChosenBy.setText(R.string.txt_suggested)
        queueEntry.userName.also { holder.txtChosenBy.text = it }

        holder.txtDuration.compoundDrawablePadding = 20
        if (song in MainActivity.mFavorites) holder.txtDuration.setCompoundDrawables(
            IconicsDrawable(context, CommunityMaterial.Icon2.cmd_star).color(
                ContextCompat.getColor(context, R.color.favorites)
            ).sizeDp(10), null, null, null
        )
    }

    override fun withIsDraggable(draggable: Boolean): QueueItem = this

    override fun isDraggable(): Boolean = true

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imgAlbumArt: ImageView = view.findViewById(R.id.song_album_art)
        var txtTitle: TextView = view.findViewById(R.id.song_title)
        var txtDescription: TextView = view.findViewById(R.id.song_description)
        var txtChosenBy: TextView = view.findViewById(R.id.song_chosen_by)
        var txtDuration: TextView = view.findViewById(R.id.song_duration)
    }
}