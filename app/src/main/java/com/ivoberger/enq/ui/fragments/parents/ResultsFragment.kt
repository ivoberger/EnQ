package com.ivoberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.items.ResultItem
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.launch

@ContentView(R.layout.fragment_queue)
open class ResultsFragment : SongListFragment<ResultItem>() {

    val loadingHeader: ItemAdapter<ProgressItem> by lazy { ItemAdapter<ProgressItem>() }
    override val songAdapter: ModelAdapter<Song, ResultItem> by lazy {
        ModelAdapter { element: Song -> ResultItem(element) }
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

    fun displayResults(results: List<Song>?) = mMainScope.launch {
        loadingHeader.clear()
        results ?: return@launch
        songAdapter.set(results)
    }
}
