package me.iberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.OnClickListener
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.items.LoadingHeader
import me.iberger.enq.ui.items.SongItem
import me.iberger.enq.ui.items.SuggestionsItem
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot

@ContentView(R.layout.fragment_queue)
open class ResultsFragment : Fragment() {

    val mUIScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    val loadingHeader: ItemAdapter<LoadingHeader> by lazy { ItemAdapter<LoadingHeader>() }
    val resultsAdapter: ItemAdapter<SongItem> by lazy { ItemAdapter<SongItem>() }
    lateinit var fastAdapter: FastAdapter<SongItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fastAdapter = FastAdapter.with(listOf(loadingHeader, resultsAdapter))

        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = fastAdapter

        fastAdapter.onClickListener = object : OnClickListener<SongItem> {
            override fun onClick(v: View?, adapter: IAdapter<SongItem>, item: SongItem, position: Int): Boolean {
                if (!MainActivity.connected) return false
                mBackgroundScope.launch {
                    JMusicBot.enqueue(item.model)
                    withContext(Dispatchers.Main) {
                        resultsAdapter.remove(position)
                        context!!.toastShort(
                            context!!.getString(
                                R.string.msg_enqueued,
                                item.model.title
                            )
                        )
                    }
                }
                return true
            }
        }
    }

    fun displayResults(results: List<SuggestionsItem>?) =
        mUIScope.launch {
            loadingHeader.clear()
            results ?: return@launch
            resultsAdapter.set(results)
        }
}
