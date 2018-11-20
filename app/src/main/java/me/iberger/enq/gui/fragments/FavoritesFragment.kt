package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.iberger.enq.R
import me.iberger.enq.gui.adapterItems.SongEntryItem
import me.iberger.enq.utils.loadFavorites

class FavoritesFragment : Fragment() {

    companion object {
        fun newInstance() = FavoritesFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val itemAdapter = ItemAdapter<SongEntryItem>()
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = FastAdapter.with<SongEntryItem, ItemAdapter<SongEntryItem>>(itemAdapter)
        val favorites = loadFavorites(context!!)
        itemAdapter.add(favorites.map { SongEntryItem(it) })
    }
}