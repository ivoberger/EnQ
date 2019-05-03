package com.ivoberger.enq.ui.fragments.parents

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.ivoberger.enq.R
import com.ivoberger.jmusicbot.listener.ConnectionChangeListener
import com.ivoberger.jmusicbot.model.MusicBotPlugin
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import timber.log.Timber


@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
abstract class TabbedResultsFragment : Fragment(R.layout.fragment_results), ViewPager.OnPageChangeListener,
    ConnectionChangeListener {
    lateinit var mProviderPlugins: Deferred<List<MusicBotPlugin>?>

    var mSelectedPlugin: MusicBotPlugin? = null
    lateinit var mFragmentPagerAdapter: Deferred<SongListFragmentPager>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view_pager.addOnPageChangeListener(this)
        lifecycleScope.launch {
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

    open fun onTabSelected(position: Int) {}

    override fun onConnectionLost(e: Exception?) {
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
        onTabSelected(position)
        lifecycleScope.launch(Dispatchers.IO) { mSelectedPlugin = mProviderPlugins.await()?.get(position) }
    }

    abstract inner class SongListFragmentPager(
        fm: FragmentManager, val provider: List<MusicBotPlugin>
    ) : FragmentStatePagerAdapter(fm, RESUME_ONLY_CURRENT_FRAGMENT) {

        val resultFragments: MutableList<ResultsFragment> = mutableListOf()

        override fun getCount(): Int = provider.size

        override fun getPageTitle(position: Int): CharSequence? = provider[position].name

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val currentFragment = super.instantiateItem(container, position) as ResultsFragment
            resultFragments.add(position, currentFragment)
            return currentFragment
        }

        open fun onTabSelected(position: Int) {}
    }
}
