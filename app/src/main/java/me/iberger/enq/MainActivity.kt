package me.iberger.enq

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.forEachIndexed
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.context.IconicsLayoutInflater2
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val mMenuIcons = listOf(
        CommunityMaterial.Icon2.cmd_playlist_play,
        CommunityMaterial.Icon2.cmd_magnify,
        CommunityMaterial.Icon2.cmd_star_outline
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        LayoutInflaterCompat.setFactory2(layoutInflater, IconicsLayoutInflater2(delegate))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_bottom_navigation.setOnNavigationItemSelectedListener(this)
        main_bottom_navigation.menu.forEachIndexed { index, item ->
            item.icon = IconicsDrawable(this).icon(mMenuIcons[index])
        }

        supportFragmentManager.commit { replace(R.id.main_content, QueueFragment.newInstance()) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_queue -> {
                true
            }
            R.id.nav_search -> {
                true
            }
            R.id.nav_starred -> {
                true
            }
            else -> false
        }
    }
}
