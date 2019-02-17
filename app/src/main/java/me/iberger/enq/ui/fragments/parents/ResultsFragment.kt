package me.iberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IInterceptor
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.ui.items.ResultItem
import me.iberger.jmusicbot.model.Song

@ContentView(R.layout.fragment_queue)
open class ResultsFragment : SongListFragment<ResultItem>() {

    val loadingHeader: ItemAdapter<ProgressItem> by lazy { ItemAdapter<ProgressItem>() }
    override val songAdapter: ModelAdapter<Song, ResultItem> by lazy {
        ModelAdapter(object : IInterceptor<Song, ResultItem> {
            override fun intercept(element: Song): ResultItem? = ResultItem(element)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastAdapter = FastAdapter.with(listOf(loadingHeader, songAdapter))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_queue.layoutManager = LinearLayoutManager(context)
        recycler_queue.adapter = fastAdapter
    }

    fun displayResults(results: List<Song>?) =
        mMainScope.launch {
            loadingHeader.clear()
            results ?: return@launch
            songAdapter.set(results)
        }
}
