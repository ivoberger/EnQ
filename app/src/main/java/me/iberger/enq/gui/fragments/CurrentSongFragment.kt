package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_current_song.*
import me.iberger.enq.R
import me.iberger.jmusicbot.data.PlayerState
import me.iberger.jmusicbot.data.PlayerStates

class CurrentSongFragment : Fragment() {

    companion object {
        private lateinit var mPlayerState: PlayerState
        fun newInstance(playerState: PlayerState): CurrentSongFragment {
            mPlayerState = playerState
            return CurrentSongFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_current_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        song_title.isSelected = true
        song_description.isSelected = true
        if (mPlayerState.state == PlayerStates.STOP) {
            song_title.setText(R.string.msg_nothing_playing)
            song_description.setText(R.string.msg_queue_smth)
        }
    }
}