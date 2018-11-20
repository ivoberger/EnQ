package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.enq.gui.adapterItems.QueueEntryItem
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.listener.QueueUpdateListener
import timber.log.Timber

class QueueFragment : Fragment(), QueueUpdateListener {

    companion object {
        fun newInstance() = QueueFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private lateinit var mMusicBot: MusicBot
    private var mQueue: List<QueueEntry> = listOf()
    private lateinit var mItemAdapter: ItemAdapter<QueueEntryItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMusicBot = (activity as MainActivity).musicBot
        mMusicBot.startQueueUpdates(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mItemAdapter = ItemAdapter()
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = FastAdapter.with<QueueEntryItem, ItemAdapter<QueueEntryItem>>(mItemAdapter)
    }

    override fun onQueueChanged(newQueue: List<QueueEntry>) {
        if (mQueue == newQueue) return
        mQueue = newQueue
        val itemQueue = newQueue.map { QueueEntryItem((it)) }
        mUIScope.launch { mItemAdapter.set(itemQueue) }
    }

    override fun onUpdateError(e: Exception) {
        Timber.e(e)
        Toast.makeText(context, "Something horrific just happened", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMusicBot.stopQueueUpdates(this)
    }
}