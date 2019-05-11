package com.ivoberger.enq.ui.fragments

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.persistence.GlideApp
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.enq.utils.tryWithErrorToast
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.PlayerStates
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.resources.dimen
import splitties.resources.str
import splitties.views.onClick
import timber.log.Timber

@ExperimentalSplittiesApi
@PotentialFutureAndroidXLifecycleKtxApi
class PlayerFragment : Fragment(R.layout.fragment_player), MotionLayout.TransitionListener {

    private val mViewModel: MainViewModel by viewModels({ activity!! })

    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)
    private val mProgressMultiplier = 1000

    private lateinit var mPlayDrawable: IconicsDrawable
    private lateinit var mPauseDrawable: IconicsDrawable
    private lateinit var mStoppedDrawable: IconicsDrawable
    private lateinit var mErrorDrawable: IconicsDrawable
    private lateinit var mAlbumArtPlaceholderDrawable: IconicsDrawable
    private lateinit var mNotInFavoritesDrawable: IconicsDrawable
    private lateinit var mInFavoritesDrawable: IconicsDrawable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as MainActivity).main_container.setTransitionListener(this)
        // pre-load drawables for player buttons
        lifecycleScope.launch(Dispatchers.IO) {
            val color = context.onPrimaryColor()
            mPlayDrawable = icon(CommunityMaterial.Icon2.cmd_play).color(color)
            mPauseDrawable = icon(CommunityMaterial.Icon2.cmd_pause).color(color)
            mStoppedDrawable = icon(CommunityMaterial.Icon2.cmd_stop).color(color)
            mErrorDrawable = icon(CommunityMaterial.Icon.cmd_alert_circle_outline).color(color)
            mNotInFavoritesDrawable = icon(CommunityMaterial.Icon2.cmd_star_outline).color(color)
            mInFavoritesDrawable = icon(CommunityMaterial.Icon2.cmd_star).color(context.secondaryColor())
            mAlbumArtPlaceholderDrawable =
                icon(CommunityMaterial.Icon.cmd_album).color(color).sizeDp(dimen(R.dimen.song_albumArt_size).toInt())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.playerState.observe(this) { onPlayerStateChanged(it) }
        Timber.d("Player created")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        song_title.isSelected = true
        song_description.isSelected = true

        song_play_pause.onClick { changePlaybackState() }
        song_favorite.onClick { changeFavoriteStatus() }
        song_progress.progress = mPlayerState.progress
    }

    private fun changePlaybackState() = lifecycleScope.launch(Dispatchers.IO) {
        if (!JMusicBot.isConnected) return@launch
        tryWithErrorToast {
            when (mPlayerState.state) {
                PlayerStates.STOP -> JMusicBot.play()
                PlayerStates.PLAY -> JMusicBot.pause()
                PlayerStates.PAUSE -> JMusicBot.play()
                PlayerStates.ERROR -> JMusicBot.play()
            }
        }
    }

    private fun changeFavoriteStatus() = lifecycleScope.launch(Dispatchers.IO) {
        AppSettings.changeFavoriteStatus(context!!, mPlayerState.songEntry!!.song).join()
        withContext(Dispatchers.Main) {
            song_favorite.setImageDrawable(
                if (mPlayerState.songEntry!!.song in AppSettings.favorites) mInFavoritesDrawable
                else mNotInFavoritesDrawable
            )
        }
    }


    private fun onPlayerStateChanged(newState: PlayerState) = lifecycleScope.launch(Dispatchers.Default) {
        if (newState == mPlayerState || view == null) return@launch
        mPlayerState = newState
        when (newState.state) {
            PlayerStates.STOP -> {
                withContext(Dispatchers.Main) {
                    song_title.setText(R.string.msg_nothing_playing)
                    song_description.setText(R.string.msg_queue_smth)
                    song_play_pause.setImageDrawable(mStoppedDrawable)
                    song_favorite.visibility = View.GONE
                }
                return@launch
            }
            PlayerStates.PLAY -> {
                withContext(Dispatchers.Main) {
                    song_favorite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPauseDrawable)
                }
            }
            PlayerStates.PAUSE -> {
                withContext(Dispatchers.Main) {
                    song_favorite.visibility = View.VISIBLE
                    song_play_pause.setImageDrawable(mPlayDrawable)
                }
            }
            PlayerStates.ERROR -> {
                withContext(Dispatchers.Main) {
                    song_play_pause.setImageDrawable(mErrorDrawable)
                }
                return@launch
            }
        }
        val songEntry = newState.songEntry!!
        val song = songEntry.song
        withContext(Dispatchers.Main) {
            // fill in song metadata
            song_title.text = song.title
            song_description.text = song.description
            GlideApp.with(this@PlayerFragment)
                .load(song.albumArtUrl ?: mAlbumArtPlaceholderDrawable)
                .into(song_album_art)
            song.duration?.also {
                song_duration.text = String.format("%02d:%02d", it / 60, it % 60)
                song_progress.max = it * mProgressMultiplier
            }
            song_chosen_by.text = songEntry.userName ?: str(R.string.txt_suggested)
            // set fav status
            song_favorite.setImageDrawable(
                if (song in AppSettings.favorites) mInFavoritesDrawable
                else mNotInFavoritesDrawable
            )
            ObjectAnimator.ofInt(
                song_progress, "progress", mPlayerState.progress * mProgressMultiplier
            ).apply {
                setAutoCancel(true)
                duration = 1000
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}

    override fun allowsTransition(motionLayout: MotionScene.Transition?): Boolean = true

    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}

    override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
        player_container.progress = progress
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        GlideApp.with(this@PlayerFragment)
            .load(mPlayerState.songEntry?.song?.albumArtUrl ?: mAlbumArtPlaceholderDrawable)
            .into(song_album_art)
    }
}
