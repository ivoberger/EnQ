/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.ui.fragments.base

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoberger.enq.ui.items.ResultItem
import com.ivoberger.jmusicbot.client.model.Song
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.ui.items.ProgressItem
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.launch

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

    fun displayResults(results: List<Song>?) = lifecycleScope.launch {
        loadingHeader.clear()
        results ?: return@launch
        songAdapter.set(results)
    }
}
