package me.iberger.enq.gui.adapterItems

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import me.iberger.enq.R
import me.iberger.jmusicbot.data.Song

class SongEntryItem(
    val song: Song
) :
    AbstractItem<SongEntryItem, SongEntryItem.ViewHolder>() {
    override fun getType(): Int = R.id.queue_entry
    override fun getLayoutRes(): Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        holder.txtTitle.isSelected = true
        holder.txtDescription.isSelected = true
        holder.txtTitle.text = song.title
        holder.txtDescription.text = song.description
        song.albumArtUrl?.also { Picasso.get().load(it).into(holder.imgAlbumArt) }
        song.duration?.also { holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var imgAlbumArt: ImageView = view.findViewById(R.id.song_album_art)
        var txtTitle: TextView = view.findViewById(R.id.song_title)
        var txtDescription: TextView = view.findViewById(R.id.song_description)
        var txtDuration: TextView = view.findViewById(R.id.song_duration)
    }
}