package com.ivoberger.enq.ui.items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.items.ModelAbstractItem

open class SongItem(song: Song) : ModelAbstractItem<Song, SongItem.ViewHolder>(song) {

    override val type: Int = R.id.queue_entry
    override val layoutRes: Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        holder.txtTitle.isSelected = true
        holder.txtDescription.isSelected = true
        holder.txtTitle.text = model.title
        holder.txtDescription.text = model.description

        model.albumArtUrl?.also { Glide.with(holder.itemView).load(it).into(holder.imgAlbumArt) }
        model.duration?.also {
            holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imgAlbumArt: ImageView = view.findViewById(R.id.song_album_art)
        var txtTitle: TextView = view.findViewById(R.id.song_title)
        var txtDescription: TextView = view.findViewById(R.id.song_description)
        var txtDuration: TextView = view.findViewById(R.id.song_duration)
    }
}
