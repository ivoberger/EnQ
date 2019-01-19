package me.iberger.enq.ui.listener

import androidx.recyclerview.widget.ListUpdateCallback
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.iberger.enq.ui.items.QueueItem
import me.iberger.jmusicbot.model.QueueEntry

class QueueUpdateCallback(private val mFastItemAdapter: FastItemAdapter<QueueItem>) : ListUpdateCallback {

    var currentList = listOf<QueueEntry>()

    override fun onChanged(position: Int, count: Int, payload: Any?) =
        mFastItemAdapter.notifyAdapterItemRangeChanged(position, count, payload)

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        mFastItemAdapter.move(fromPosition, toPosition)
//        mFastItemAdapter.notifyAdapterItemMoved(fromPosition, toPosition)
    }

    override fun onInserted(position: Int, count: Int) {
        val addList = currentList.subList(position, position + count).map { QueueItem(it) }
        GlobalScope.launch(Dispatchers.Main) { mFastItemAdapter.add(position, addList) }
    }

    override fun onRemoved(position: Int, count: Int) {
        mFastItemAdapter.removeItemRange(position, count)
    }
}
