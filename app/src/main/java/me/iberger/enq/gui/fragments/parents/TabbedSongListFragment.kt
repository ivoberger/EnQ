package me.iberger.enq.gui.fragments.parents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.backend.Configuration
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.data.MusicBotPlugin
import me.iberger.jmusicbot.listener.ConnectionChangeListener
import timber.log.Timber

abstract class TabbedSongListFragment : Fragment(), ViewPager.OnPageChangeListener,
    ConnectionChangeListener {
    val mUIScope = CoroutineScope(Dispatchers.Main)

    val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    lateinit var mProviderPlugins: Deferred<List<MusicBotPlugin>?>
    var mConfig: Configuration = MainActivity.config

    var mSelectedPlugin: MusicBotPlugin? = null
    lateinit var mFragmentPagerAdapter: Deferred<SongListFragmentPager>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view_pager.addOnPageChangeListener(this)
        mUIScope.launch {
            if (mSelectedPlugin == null) mSelectedPlugin = mProviderPlugins.await()?.get(0)
            mSelectedPlugin?.let {
                Timber.d("Setting tab to ${mProviderPlugins.await()!!.indexOf(it)}")
                val idx = mProviderPlugins.await()!!.indexOf(it)
                view_pager.postDelayed(Runnable {
                    view_pager.setCurrentItem(idx, false)
                }, 0)
            }
        }
        initializeTabs()
    }

    abstract fun initializeTabs()

    override fun onConnectionLost(e: Exception) {
        view_pager.adapter = null
    }

    override fun onConnectionRecovered() {
        initializeTabs()
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
    }

    override fun onPageSelected(position: Int) {
        mBackgroundScope.launch { mSelectedPlugin = mProviderPlugins.await()?.get(position) }
    }

    abstract inner class SongListFragmentPager(
        fm: FragmentManager,
        val provider: List<MusicBotPlugin>
    ) :
        FragmentStatePagerAdapter(fm) {

        val resultFragments: MutableList<ResultsFragment> = mutableListOf()

        override fun getCount(): Int = provider.size

        override fun getPageTitle(position: Int): CharSequence? = provider[position].name

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val currentFragment = super.instantiateItem(container, position) as ResultsFragment
            resultFragments.add(currentFragment)
            return currentFragment
        }
    }
}