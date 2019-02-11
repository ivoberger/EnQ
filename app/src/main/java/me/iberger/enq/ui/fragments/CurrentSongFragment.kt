package me.iberger.enq.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.ContentView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.fragment_current_song.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.ui.MainActivity
import me.iberger.enq.ui.MainActivity.Companion.favorites
import me.iberger.enq.ui.viewmodel.PlayerViewModel
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.icon
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.model.PlayerState
import me.iberger.jmusicbot.model.PlayerStates
import me.iberger.jmusicbot.model.Song
import splitties.resources.color
import timber.log.Timber

@ContentView(R.layout.fragment_current_song)
class CurrentSongFragment : Fragment() {

    companion object {
        fun newInstance(): CurrentSongFragment = CurrentSongFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)
    private val mViewModel by lazy { ViewModelProviders.of(this).get(PlayerViewModel::class.java) }

    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)
    private var mShowSkip = false

    private lateinit var mPlayDrawable: IconicsDrawable
    private lateinit var mPauseDrawable: IconicsDrawable
    private lateinit var mStoppedDrawable: IconicsDrawable
    private lateinit var mSkipDrawable: IconicsDrawable
    private lateinit var mErrorDrawable: IconicsDrawable

    private lateinit var mFavoritesAddDrawable: IconicsDrawable
    private lateinit var mFavoritesDeleteDrawable: IconicsDrawable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // pre-load drawables for player buttons
        mBackgroundScope.launch {
            val color = color(R.color.white)
            mPlayDrawable = context.icon(CommunityMaterial.Icon2.cmd_play).color(color)
            mPauseDrawable = context.icon(CommunityMaterial.Icon2.cmd_pause).color(color)
            mStoppedDrawable = context.icon(CommunityMaterial.Icon2.cmd_stop).color(color)
            mSkipDrawable = context.icon(CommunityMaterial.Icon.cmd_fast_forward).color(color)
            mErrorDrawable = context.icon(CommunityMaterial.Icon.cmd_alert_circle_outline).color(color)
            mFavoritesAddDrawable = context.icon(CommunityMaterial.Icon2.cmd_star_outline).color(color)
            mFavoritesDeleteDrawable = context.icon(CommunityMaterial.Icon2.cmd_star).color(color(R.color.favorites))
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.playerState.observe(this, Observer { onPlayerStateChanged(it) })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        song_title.isSelected = true
        song_description.isSelected = true
        song_play_pause.setOnClickListener { changePlaybackState() }
        song_favorite.setOnClickListener { addToFavorites() }
        view.setOnClickListener {
            song_play_pause.setImageDrawable(
                if (mShowSkip) {
                    mShowSkip = false
                    if (mPlayerState.state == PlayerStates.PLAY) mPauseDrawable
                    else mPlayDrawable
                } else {
                    mShowSkip = true
                    mSkipDrawable
                }
            )
        }
    }

    private fun changePlaybackState() = mBackgroundScope.launch {
        if (!MainActivity.connected) return@launch
        if (mShowSkip) {
            try {
                JMusicBot.skip()
            } catch (e: Exception) {
                Timber.e(e)
                mUIScope.launch { context?.toastShort(R.string.msg_no_permission) }
            } finally {
                return@launch
            }
        }
        when (mPlayerState.state) {
            PlayerStates.STOP -> JMusicBot.play()
            PlayerStates.PLAY -> JMusicBot.pause()
            PlayerStates.PAUSE -> JMusicBot.play()
            PlayerStates.ERROR -> JMusicBot.play()
        }
    }

    private fun addToFavorites() {
        mBackgroundScope.launch {
            changeFavoriteStatus(mPlayerState.songEntry!!.song).join()
            mUIScope.launch {
                song_favorite.setImageDrawable(
                    if (mPlayerState.songEntry!!.song in favorites) mFavoritesDeleteDrawable
                    else mFavoritesAddDrawable
                )
            }
        }
    }

    private fun changeFavoriteStatus(song: Song) = mBackgroundScope.launch {
        changeFavoriteStatus(context!!, song)
        mUIScope.launch {
            song_favorite.setImageDrawable(
                if (song in favorites) mFavoritesDeleteDrawable
                else mFavoritesAddDrawable
            )
        }
    }


    fun onPlayerStateChanged(newState: PlayerState) {
        if (newState == mPlayerState || view == null) return
        mUIScope.launch {
            mPlayerState = newState
            when (newState.state) {
                PlayerStates.STOP -> {
                    song_title.setText(R.string.msg_nothing_playing)
                    song_description.setText(R.string.msg_queue_smth)
                    song_play_pause.setImageDrawable(mStoppedDrawable)
                    song_favorite.visibility = View.GONE
                    return@launch
                }
                PlayerStates.PLAY -> {
                    song_favorite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPauseDrawable)
                }
                PlayerStates.PAUSE -> {
                    song_favorite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPlayDrawable)
                }
                PlayerStates.ERROR -> {
                    song_play_pause.setImageDrawable(mErrorDrawable)
                    return@launch
                }
            }
            if (mShowSkip) song_play_pause.setImageDrawable(mSkipDrawable)
            val songEntry = newState.songEntry!!
            val song = songEntry.song
            // fill in song metadata
            song_title.text = song.title
            song_description.text = song.description
            if (song.albumArtUrl != null)
                Glide.with(this@CurrentSongFragment).load(song.albumArtUrl).into(song_album_art)
            else Glide.with(this@CurrentSongFragment).clear(song_album_art)
            song.duration?.also {
                song_duration.text = String.format("%02d:%02d", it / 60, it % 60)
            }
            song_chosen_by.setText(R.string.txt_suggested)
            songEntry.userName?.also { song_chosen_by.text = it }
            // set fav status
            song_favorite.setImageDrawable(
                if (song in favorites) mFavoritesDeleteDrawable
                else mFavoritesAddDrawable
            )
        }
    }
}
