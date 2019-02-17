package com.ivoberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.ivoberger.enq.R
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.items.SongItem
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.toastShort
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Song
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.listeners.OnClickListener
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ContentView(R.layout.fragment_queue)
abstract class SongListFragment<T : SongItem> : Fragment() {

    val mMainScope = CoroutineScope(Dispatchers.Main)
    val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    val mViewModel by lazy { ViewModelProviders.of(context as MainActivity).get(MainViewModel::class.java) }

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

        fastAdapter.onClickListener = object : OnClickListener<T> {
            override fun onClick(v: View?, adapter: IAdapter<T>, item: T, position: Int): Boolean {
                if (!mViewModel.connected) return false
                mBackgroundScope.launch {
                    JMusicBot.enqueue(item.model)
                    withContext(Dispatchers.Main) {
                        if (isRemoveAfterEnQ) songAdapter.remove(position)
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
}
