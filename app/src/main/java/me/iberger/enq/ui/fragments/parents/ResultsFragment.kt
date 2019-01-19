package me.iberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.items.SongItem
import me.iberger.enq.ui.items.SuggestionsItem
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.MusicBot

open class ResultsFragment : Fragment() {

    val mUIScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    val fastItemAdapter: FastItemAdapter<SongItem> by lazy { FastItemAdapter<SongItem>() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = fastItemAdapter

        fastItemAdapter.withOnClickListener { _, _, item, position ->
            if (!MainActivity.connected) return@withOnClickListener false
            mBackgroundScope.launch {
                MusicBot.instance?.enqueue(item.song)
                withContext(Dispatchers.Main) {
                    fastItemAdapter.remove(position)
                    context!!.toastShort(
                        context!!.getString(
                            R.string.msg_enqueued,
                            item.song.title
                        )
                    )
                }
            }
            true
        }
    }

    fun displayResults(results: List<SuggestionsItem>?) =
        mUIScope.launch { fastItemAdapter.set(results) }
}
