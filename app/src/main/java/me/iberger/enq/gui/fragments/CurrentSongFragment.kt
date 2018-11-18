package me.iberger.enq.gui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_current_song.*
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.enq.listener.PlayerStateChangeListener
import me.iberger.jmusicbot.data.PlayerState
import me.iberger.jmusicbot.data.PlayerStates

class CurrentSongFragment : Fragment(), PlayerStateChangeListener {
    companion object {

        fun newInstance(): CurrentSongFragment = CurrentSongFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_current_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onPlayerStateChanged((activity as MainActivity).playerState)
        song_title.isSelected = true
        song_description.isSelected = true
        song_action_2.setImageDrawable(
            IconicsDrawable(
                context,
                CommunityMaterial.Icon2.cmd_star_outline
            ).color(Color.WHITE)
        )
    }

    override fun onPlayerStateChanged(newState: PlayerState) {
        when (newState.state) {
            PlayerStates.STOP -> {
                song_title.setText(R.string.msg_nothing_playing)
                song_description.setText(R.string.msg_queue_smth)
                song_action_1.setImageDrawable(
                    IconicsDrawable(
                        context,
                        CommunityMaterial.Icon2.cmd_stop
                    ).color(Color.WHITE)
                )
                return
            }
            PlayerStates.PLAY -> song_action_1.setImageDrawable(
                IconicsDrawable(
                    context,
                    CommunityMaterial.Icon2.cmd_pause
                ).color(Color.WHITE)
            )
            PlayerStates.PAUSE -> song_action_1.setImageDrawable(
                IconicsDrawable(
                    context,
                    CommunityMaterial.Icon2.cmd_play
                ).color(Color.WHITE)
            )
            PlayerStates.ERROR -> {
                song_action_1.setImageDrawable(
                    IconicsDrawable(
                        context,
                        CommunityMaterial.Icon.cmd_alert_circle_outline
                    ).color(Color.WHITE)
                )
                return
            }
        }
        val songEntry = newState.songEntry!!
        val song = songEntry.song
        song_title.text = song.title
        song_description.text = song.description
        song.albumArtUrl?.also { Picasso.get().load(it).into(song_album_art) }
        song.duration?.also { song_duration.text = String.format("%02d:%02d", it / 60, it % 60) }
        song_chosen_by.setText(R.string.txt_suggested)
        songEntry.userName?.also { song_chosen_by.text = it }
    }
}
