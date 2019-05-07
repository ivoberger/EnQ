package com.ivoberger.enq.ui.items

import android.view.View
import com.ivoberger.enq.R
import com.ivoberger.enq.utils.bindView
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.items.ModelAbstractItem

open class SongItem(song: Song) : ModelAbstractItem<Song, QueueItem.ViewHolder>(song) {

    override var identifier: Long = song.id.hashCode().toLong()
    override val type: Int = R.id.queue_entry
    override val layoutRes: Int = R.layout.adapter_queue_entry
    override fun getViewHolder(v: View) = QueueItem.ViewHolder(v)

    override fun bindView(holder: QueueItem.ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)
        model.bindView(holder)
    }
}
