package me.iberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.listeners.OnLongClickListener
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.mikepenz.fastadapter.utils.DragDropUtil
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.items.QueueItem
import me.iberger.enq.ui.viewmodel.QueueViewModel
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.setupSwipeDragActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.KEY_QUEUE
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.model.QueueEntry
import timber.log.Timber

@ContentView(R.layout.fragment_queue)
class QueueFragment : Fragment(), SimpleSwipeCallback.ItemSwipeCallback, ItemTouchCallback {
    companion object {
        fun newInstance() = QueueFragment()
    }

    private val mViewModel by lazy { ViewModelProviders.of(this).get(QueueViewModel::class.java) }
    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private var mQueue = listOf<QueueEntry>()
    private val mFastItemAdapter: FastItemAdapter<QueueItem> by lazy { FastItemAdapter<QueueItem>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Creating Queue Fragment")
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.queue.observe(this, Observer { updateQueue(it) })

        queue.layoutManager = LinearLayoutManager(context).apply { reverseLayout = true }
        queue.adapter = mFastItemAdapter
        savedInstanceState?.also { mFastItemAdapter.withSavedInstanceState(it, KEY_QUEUE) }

        setupSwipeDragActions(
            context!!, queue, this, this,
            CommunityMaterial.Icon2.cmd_star, R.color.favorites,
            CommunityMaterial.Icon.cmd_delete, R.color.delete
        )

        mFastItemAdapter.onLongClickListener = object : OnLongClickListener<QueueItem> {
            override fun onLongClick(v: View, adapter: IAdapter<QueueItem>, item: QueueItem, position: Int): Boolean {
                mViewModel.queue.removeObservers(this@QueueFragment)
                return true
            }
        }
    }

    private fun updateQueue(newQueue: List<QueueEntry>) = mBackgroundScope.launch {
        if (newQueue == mQueue) return@launch
        Timber.d("Updating Queue")
        mQueue = newQueue
        val diff = FastAdapterDiffUtil.calculateDiff(
            mFastItemAdapter.itemAdapter,
            newQueue.map { QueueItem(it) },
            QueueItem.DiffCallback()
        )
        withContext(mUIScope.coroutineContext) { FastAdapterDiffUtil.set(mFastItemAdapter.itemAdapter, diff) }
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    if (!MainActivity.connected) return@launch
                    try {
                        JMusicBot.dequeue(entry.song)
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}")
                        withContext(Dispatchers.Main) {
                            context!!.toastShort(R.string.msg_no_permission)
                            mFastItemAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    changeFavoriteStatus(context!!, entry.song)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        if (!MainActivity.connected) return false
        DragDropUtil.onMove(mFastItemAdapter.itemAdapter, oldPosition, newPosition)
        return true
    }

    override fun itemTouchDropped(oldPosition: Int, newPosition: Int) {
        if (!MainActivity.connected) return
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(newPosition).model
            Timber.d("Moved ${entry.song.title} from $oldPosition to $newPosition")
            try {
                JMusicBot.moveSong(entry, newPosition)
            } catch (e: Exception) {
                Timber.e(e)
                mUIScope.launch {
                    context?.toastShort(R.string.msg_no_permission)
                }
            } finally {
                withContext(mUIScope.coroutineContext) {
                    mViewModel.queue.observe(
                        this@QueueFragment,
                        Observer { updateQueue(it) })
                }
            }
        }
    }
}
