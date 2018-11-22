package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity.Companion.musicBot
import me.iberger.enq.gui.adapter.SuggestionsItem
import me.iberger.jmusicbot.KEY_PROVIDER_ID
import timber.log.Timber

class SearchResultsFragment() : Fragment() {

    companion object {
        fun newInstance(providerID: String) = SearchResultsFragment().apply {
            arguments = bundleOf(KEY_PROVIDER_ID to providerID)
        }
    }

    private lateinit var mProviderID: String

    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private lateinit var mFastItemAdapter: FastItemAdapter<SuggestionsItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderID = arguments!!.getString(KEY_PROVIDER_ID)!!
        Timber.d("Creating SearchResultFragment with provider $mProviderID")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFastItemAdapter = FastItemAdapter()
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = mFastItemAdapter

        mFastItemAdapter.withOnClickListener { _, _, item, position ->
            mBackgroundScope.launch {
                musicBot.enqueue(item.song).await()
                withContext(Dispatchers.Main) {
                    mFastItemAdapter.remove(position)
                }
            }
            true
        }
    }

    fun search(query: String) {
        Timber.d("Searching for $query on provider $mProviderID")
        mBackgroundScope.launch {
            val results = musicBot.search(mProviderID, query).await()
            Timber.d("Got ${results.size} results")
            val resultsAsItems = results.map { SuggestionsItem(it) }
            withContext(Dispatchers.Main) { mFastItemAdapter.set(resultsAsItems) }
        }
    }
}