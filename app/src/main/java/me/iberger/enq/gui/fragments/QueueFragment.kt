package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import me.iberger.enq.R
import me.iberger.jmusicbot.KEY_QUEUE
import me.iberger.jmusicbot.data.QueueEntry

class QueueFragment : Fragment() {

    companion object {
        fun newInstance(queue: List<QueueEntry>? = null) = QueueFragment().apply {
            arguments = bundleOf(KEY_QUEUE to queue)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }
}