package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_suggestions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.enq.gui.adapter.FavoritesItem
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.MusicBotPlugin

class SuggestionsFragment : Fragment(), BottomNavigationView.OnNavigationItemSelectedListener {
    companion object {

        fun newInstance() = SuggestionsFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)

    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private lateinit var mMusicBot: MusicBot

    private lateinit var mSuggesters: List<MusicBotPlugin>
    private lateinit var mItemAdapter: ItemAdapter<FavoritesItem>
    private val mMenuItems: MutableList<MenuItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMusicBot = (activity as MainActivity).musicBot
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_suggestions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mItemAdapter = ItemAdapter()
        suggest_list.layoutManager = LinearLayoutManager(context)
        val fastAdapter = FastAdapter.with<FavoritesItem, ItemAdapter<FavoritesItem>>(mItemAdapter)
        fastAdapter.withOnClickListener { _, _, item, position ->
            mBackgroundScope.launch {
                mMusicBot.enqueue(item.song).await()
                mUIScope.launch {
                    mItemAdapter.remove(position)
                    Toast.makeText(context, "Enqueued ${item.song.title}", Toast.LENGTH_SHORT).show()
                }
            }
            return@withOnClickListener true
        }
        suggest_list.adapter = fastAdapter

        suggestions_bottom_navigation.setOnNavigationItemSelectedListener(this)
        mBackgroundScope.launch {
            mSuggesters = mMusicBot.suggesters
            mUIScope.launch {
                val menu = suggestions_bottom_navigation.menu
                mSuggesters.forEach { mMenuItems.add(menu.add(it.name)) }
            }
            val suggestions = mMusicBot.getSuggestions(mSuggesters[0])
            mUIScope.launch { mItemAdapter.set(suggestions.await().map { FavoritesItem(it) }) }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mBackgroundScope.launch {
            val suggestions = mMusicBot.getSuggestions(mSuggesters[mMenuItems.indexOf(item)])
            mUIScope.launch { mItemAdapter.set(suggestions.await().map { FavoritesItem(it) }) }
        }
        return true
    }
}
