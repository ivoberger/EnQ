package me.iberger.enq.gui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_current_song.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.PlayerState
import me.iberger.jmusicbot.data.PlayerStates
import me.iberger.jmusicbot.listener.PlayerUpdateListener
import timber.log.Timber

class CurrentSongFragment : Fragment(), PlayerUpdateListener {
    companion object {

        fun newInstance(): CurrentSongFragment = CurrentSongFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)

    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private lateinit var mMainActivity: MainActivity

    private lateinit var mMusicBot: MusicBot
    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)
    private lateinit var mPlayDrawable: IconicsDrawable

    private lateinit var mPauseDrawable: IconicsDrawable
    private lateinit var mStoppedDrawable: IconicsDrawable
    private lateinit var mErrorDrawable: IconicsDrawable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainActivity = activity as MainActivity
        mMusicBot = mMainActivity.musicBot
        mMusicBot.startPlayerUpdates(this@CurrentSongFragment)
        mBackgroundScope.launch {
            mPlayDrawable = IconicsDrawable(context, CommunityMaterial.Icon2.cmd_play).color(Color.WHITE)
            mPauseDrawable = IconicsDrawable(context, CommunityMaterial.Icon2.cmd_pause).color(Color.WHITE)
            mStoppedDrawable = IconicsDrawable(context, CommunityMaterial.Icon2.cmd_stop).color(Color.WHITE)
            mErrorDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_alert_circle_outline).color(Color.WHITE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_current_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        ButterKnife.bind(this, view)
        song_title.isSelected = true
        song_description.isSelected = true
        song_favourite.setImageDrawable(
            IconicsDrawable(
                context,
                CommunityMaterial.Icon2.cmd_star_outline
            ).color(Color.WHITE)
        )
        song_play_pause.setOnClickListener { changePlaybackState() }
        song_favourite.setOnClickListener { addToFavourites() }
    }

    //    @OnClick(R.id.song_play_pause)
    private fun changePlaybackState() {
        when (mPlayerState.state) {
            PlayerStates.STOP -> mBackgroundScope.launch { onPlayerStateChanged(mMusicBot.play().await()) }
            PlayerStates.PLAY -> mBackgroundScope.launch { onPlayerStateChanged(mMusicBot.pause().await()) }
            PlayerStates.PAUSE -> mBackgroundScope.launch { onPlayerStateChanged(mMusicBot.play().await()) }
            PlayerStates.ERROR -> mBackgroundScope.launch { onPlayerStateChanged(mMusicBot.play().await()) }
        }
    }

    //    @OnClick(R.id.song_favourite)
    private fun addToFavourites() {
        mMainActivity.favourites.add(mPlayerState.songEntry!!.song)
    }

    override fun onPlayerStateChanged(newState: PlayerState) {
        if (newState == mPlayerState) return
        mUIScope.launch {
            mPlayerState = newState
            when (newState.state) {
                PlayerStates.STOP -> {
                    song_title.setText(R.string.msg_nothing_playing)
                    song_description.setText(R.string.msg_queue_smth)
                    song_play_pause.setImageDrawable(mStoppedDrawable)
                    song_favourite.visibility = View.GONE
                    return@launch
                }
                PlayerStates.PLAY -> {
                    song_favourite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPauseDrawable)
                }
                PlayerStates.PAUSE -> {
                    song_favourite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPlayDrawable)
                }
                PlayerStates.ERROR -> {
                    song_play_pause.setImageDrawable(mStoppedDrawable)
                    song_favourite.visibility = View.GONE
                    return@launch
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

    override fun onUpdateError(e: Exception) {
        Timber.e(e)
        mUIScope.launch { Toast.makeText(context, "Error when updating player state", Toast.LENGTH_SHORT).show() }
    }
}
