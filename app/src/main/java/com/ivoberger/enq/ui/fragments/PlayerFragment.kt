package com.ivoberger.enq.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.Configuration
import com.ivoberger.enq.persistence.GlideApp
import com.ivoberger.enq.ui.MainActivity
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.enq.utils.tryWithErrorToast
import com.ivoberger.jmusicbot.JMusicBot
import com.ivoberger.jmusicbot.model.Permissions
import com.ivoberger.jmusicbot.model.PlayerState
import com.ivoberger.jmusicbot.model.PlayerStates
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.experimental.ExperimentalSplittiesApi
import splitties.lifecycle.coroutines.PotentialFutureAndroidXLifecycleKtxApi
import splitties.lifecycle.coroutines.lifecycleScope
import splitties.toast.toast
import timber.log.Timber


@PotentialFutureAndroidXLifecycleKtxApi
@ExperimentalSplittiesApi
class PlayerFragment : Fragment(R.layout.fragment_player) {

    private val mViewModel by lazy { ViewModelProviders.of(context as MainActivity).get(MainViewModel::class.java) }

    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)
    private var mShowSkip = false

    private val mFlingListener by lazy {
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                motionEventStart: MotionEvent?,
                motionEventEnd: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (velocityX > Math.abs(velocityY) * 2) if (JMusicBot.user!!.permissions.contains(Permissions.SKIP)) {
                    lifecycleScope.launch(Dispatchers.IO) { JMusicBot.skip() }
                    return true
                } else {
                    context!!.toast(R.string.msg_no_permission)
                }
                return super.onFling(motionEventStart, motionEventEnd, velocityX, velocityY)
            }
        }
    }
    private val mGestureDetector by lazy { GestureDetectorCompat(context, mFlingListener) }

    private lateinit var mPlayDrawable: IconicsDrawable
    private lateinit var mPauseDrawable: IconicsDrawable
    private lateinit var mStoppedDrawable: IconicsDrawable
    private lateinit var mSkipDrawable: IconicsDrawable
    private lateinit var mErrorDrawable: IconicsDrawable

    private lateinit var mNotInFavoritesDrawable: IconicsDrawable
    private lateinit var mInFavoritesDrawable: IconicsDrawable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // pre-load drawables for player buttons
        lifecycleScope.launch(Dispatchers.IO) {
            val color = context.onPrimaryColor()
            mPlayDrawable = icon(CommunityMaterial.Icon2.cmd_play).color(color)
            mPauseDrawable = icon(CommunityMaterial.Icon2.cmd_pause).color(color)
            mStoppedDrawable = icon(CommunityMaterial.Icon2.cmd_stop).color(color)
            mSkipDrawable = icon(CommunityMaterial.Icon.cmd_fast_forward).color(color)
            mErrorDrawable = icon(CommunityMaterial.Icon.cmd_alert_circle_outline).color(color)
            mNotInFavoritesDrawable = icon(CommunityMaterial.Icon2.cmd_star_outline).color(color)
            mInFavoritesDrawable = icon(CommunityMaterial.Icon2.cmd_star).color(context.secondaryColor())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.playerState.observe(this, Observer { onPlayerStateChanged(it) })
        Timber.d("Player created")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        song_title.isSelected = true
        song_description.isSelected = true
        song_play_pause.setOnClickListener { changePlaybackState() }
        song_favorite.setOnClickListener { addToFavorites() }
        view.setOnTouchListener { _, event ->
            mGestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun changePlaybackState() = lifecycleScope.launch(Dispatchers.IO) {
        if (!JMusicBot.isConnected) return@launch
        if (mShowSkip) {
            try {
                JMusicBot.skip()
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) { context?.toast(R.string.msg_no_permission) }
            } finally {
                return@launch
            }
        }
        tryWithErrorToast {
            runBlocking {
                when (mPlayerState.state) {
                    PlayerStates.STOP -> JMusicBot.play()
                    PlayerStates.PLAY -> JMusicBot.pause()
                    PlayerStates.PAUSE -> JMusicBot.play()
                    PlayerStates.ERROR -> JMusicBot.play()
                }
            }
        }
    }

    private fun addToFavorites() = lifecycleScope.launch(Dispatchers.IO) {
        Configuration.changeFavoriteStatus(context!!, mPlayerState.songEntry!!.song).join()
        withContext(Dispatchers.Main) {
            song_favorite.setImageDrawable(
                if (mPlayerState.songEntry!!.song in Configuration.favorites) mInFavoritesDrawable
                else mNotInFavoritesDrawable
            )
        }
    }


    private fun onPlayerStateChanged(newState: PlayerState) {
        if (newState == mPlayerState || view == null) return
        lifecycleScope.launch {
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
                GlideApp.with(this@PlayerFragment).load(song.albumArtUrl).into(song_album_art)
            else GlideApp.with(this@PlayerFragment).clear(song_album_art)
            song.duration?.also {
                song_duration.text = String.format("%02d:%02d", it / 60, it % 60)
                song_progress.max = it
            }
            song_chosen_by.setText(R.string.txt_suggested)
            songEntry.userName?.also { song_chosen_by.text = it }
            // set fav status
            song_favorite.setImageDrawable(
                if (song in Configuration.favorites) mInFavoritesDrawable
                else mNotInFavoritesDrawable
            )
            song_progress.progress = mPlayerState.progress

        }
    }
}
