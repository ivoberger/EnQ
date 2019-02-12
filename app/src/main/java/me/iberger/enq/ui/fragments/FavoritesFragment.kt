package me.iberger.enq.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.ui.items.FavoritesItem
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.loadFavorites
import me.iberger.enq.utils.setupSwipeActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot

@ContentView(R.layout.fragment_queue)
class FavoritesFragment : Fragment(), SimpleSwipeCallback.ItemSwipeCallback {

    companion object {
        fun newInstance() = FavoritesFragment()
    }

    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private lateinit var mFastItemAdapter: FastItemAdapter<FavoritesItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFastItemAdapter = FastItemAdapter()
        Queue.layoutManager = LinearLayoutManager(context)
        Queue.adapter = mFastItemAdapter
        val favorites = loadFavorites(context!!)
        mFastItemAdapter.add(favorites.map { FavoritesItem(it) })

        setupSwipeActions(
            context!!, Queue, this,
            CommunityMaterial.Icon2.cmd_plus, R.color.enqueue,
            CommunityMaterial.Icon.cmd_delete, R.color.delete
        )
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val item = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    JMusicBot.enqueue(item.model)
                    withContext(Dispatchers.Main) {
                        context!!.toastShort(
                            context!!.getString(
                                R.string.msg_enqueued,
                                item.model.title
                            )
                        )
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
                ItemTouchHelper.RIGHT -> {
                    changeFavoriteStatus(context!!, item.model)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.remove(position)
                    }
                }
            }
        }
    }
}
