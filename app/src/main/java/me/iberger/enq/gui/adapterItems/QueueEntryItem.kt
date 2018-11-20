package me.iberger.enq.gui.adapterItems

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import me.iberger.enq.R
import me.iberger.jmusicbot.data.QueueEntry

class QueueEntryItem(
    private val queueEntry: QueueEntry
) :
    AbstractItem<QueueEntryItem, QueueEntryItem.ViewHolder>() {
    override fun getType(): Int = R.id.queue_entry
    override fun getLayoutRes(): Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        holder.txtTitle.isSelected = true
        holder.txtDescription.isSelected = true
        val song = queueEntry.song
        holder.txtTitle.text = song.title
        holder.txtDescription.text = song.description
        song.albumArtUrl?.also { Picasso.get().load(it).into(holder.imgAlbumArt) }
        song.duration?.also { holder.txtDuration.text = String.format("%02d:%02d", it / 60, it % 60) }
        holder.txtChosenBy.setText(R.string.txt_suggested)
        queueEntry.userName.also { holder.txtChosenBy.text = it }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //        @BindView(R.id.song_album_art)
        lateinit var imgAlbumArt: ImageView
        //        @BindView(R.id.song_title)
        lateinit var txtTitle: TextView
        //        @BindView(R.id.song_description)
        lateinit var txtDescription: TextView
        //        @BindView(R.id.song_chosen_by)
        lateinit var txtChosenBy: TextView
        //        @BindView(R.id.song_duration)
        lateinit var txtDuration: TextView

        init {
//            ButterKnife.bind(this, view)
            imgAlbumArt = view.findViewById(R.id.song_album_art)
            txtTitle = view.findViewById(R.id.song_title)
            txtDescription = view.findViewById(R.id.song_description)
            txtChosenBy = view.findViewById(R.id.song_chosen_by)
            txtDuration = view.findViewById(R.id.song_duration)
        }
    }
}