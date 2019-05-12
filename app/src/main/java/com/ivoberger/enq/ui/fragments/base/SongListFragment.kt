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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.items.SongItem
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.resources.str
import splitties.toast.toast

@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
abstract class SongListFragment<T : SongItem> : Fragment(R.layout.fragment_queue) {

    val mViewModel: MainViewModel by viewModels({ activity!! })

    abstract val songAdapter: ModelAdapter<Song, T>
    open lateinit var fastAdapter: FastAdapter<T>
    open val isRemoveAfterEnQ = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastAdapter = FastAdapter.with(songAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_queue.layoutManager = LinearLayoutManager(context)
        recycler_queue.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item: T, position: Int ->
            if (!JMusicBot.isConnected) false
            else onEntryClicked(item, position)
        }
    }

    private fun enqueueEntry(item: T, position: Int) = lifecycleScope.launch(Dispatchers.IO) {
        JMusicBot.enqueue(item.model)
        withContext(Dispatchers.Main) {
            if (isRemoveAfterEnQ) songAdapter.remove(position)
            context!!.toast(context!!.str(R.string.msg_enqueued, item.model.title))
        }
    }

    open fun onEntryClicked(item: T, position: Int): Boolean {
        enqueueEntry(item, position)
        return true
    }
}
