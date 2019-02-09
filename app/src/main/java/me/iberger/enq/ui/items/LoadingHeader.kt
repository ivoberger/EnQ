package me.iberger.enq.ui.items

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.AbstractItem
import me.iberger.enq.R

class LoadingHeader : AbstractItem<LoadingHeader.ViewHolder>() {
    override val layoutRes: Int = R.layout.dialog_progress_spinner
    override val type: Int = R.id.loading_adapter

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
