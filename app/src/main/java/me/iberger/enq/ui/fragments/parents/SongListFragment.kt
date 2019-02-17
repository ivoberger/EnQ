package me.iberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.listeners.OnClickListener
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.items.SongItem
import me.iberger.enq.ui.viewmodel.MainViewModel
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.model.Song

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
