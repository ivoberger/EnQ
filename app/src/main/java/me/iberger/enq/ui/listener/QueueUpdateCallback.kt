package me.iberger.enq.ui.listener

import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class QueueUpdateCallback<A : ModelAdapter<Model, Item>, Model, Item : IItem<out RecyclerView.ViewHolder>>(private val adapter: A) :
    ListUpdateCallback {

    override fun onInserted(position: Int, count: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            adapter.fastAdapter!!.notifyAdapterItemRangeInserted(
                adapter.fastAdapter!!.getPreItemCountByOrder(adapter.order) + position,
                count
            )
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            adapter.fastAdapter!!.notifyAdapterItemRangeRemoved(
                adapter.fastAdapter!!.getPreItemCountByOrder(adapter.order) + position,
                count
            )
        }
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            adapter.fastAdapter!!.notifyAdapterItemMoved(
                adapter.fastAdapter!!.getPreItemCountByOrder(adapter.order) + fromPosition,
                toPosition
            )
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        GlobalScope.launch(Dispatchers.Main) {
            adapter.fastAdapter!!.notifyAdapterItemRangeChanged(
                adapter.fastAdapter!!.getPreItemCountByOrder(adapter.order) + position,
                count,
                payload
            )
        }
    }
}
