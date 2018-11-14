package me.iberger.enq.gui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.iberger.enq.R

class QueueFragment : Fragment() {

    companion object {
        fun newInstance() = QueueFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }
}