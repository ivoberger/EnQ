/*
* Copyright 2019 Ivo Berger
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.ivoberger.enq.ui.fragments

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import coil.api.load
import com.ivoberger.enq.R
import com.ivoberger.enq.persistence.AppSettings
import com.ivoberger.enq.ui.viewmodel.MainViewModel
import com.ivoberger.enq.utils.icon
import com.ivoberger.enq.utils.onPrimaryColor
import com.ivoberger.enq.utils.secondaryColor
import com.ivoberger.enq.utils.tryWithErrorToast
import com.ivoberger.jmusicbot.client.JMusicBot
import com.ivoberger.jmusicbot.client.model.Permissions
import com.ivoberger.jmusicbot.client.model.PlayerState
import com.ivoberger.jmusicbot.client.model.PlayerStates
import com.mercari.remotedata.RemoteData
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.resources.dimen
import splitties.resources.str
import splitties.toast.toast
import splitties.views.onClick
import timber.log.Timber

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private val mViewModel: MainViewModel by viewModels({ activity!! })

    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)

    private val mFlingListener by lazy {
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onFling(
                motionEventStart: MotionEvent?,
                motionEventEnd: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (velocityX > Math.abs(velocityY) * 2) if (JMusicBot.user!!.permissions.contains(
                                Permissions.SKIP
                        )
                ) {
                    lifecycleScope.launch { tryWithErrorToast { JMusicBot.skip() } }
                    return true
                } else {
                    context!!.toast(R.string.msg_no_permission)
                }
                return super.onFling(motionEventStart, motionEventEnd, velocityX, velocityY)
            }
        }
    }
    private val mGestureDetector by lazy { GestureDetectorCompat(context, mFlingListener) }
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
        retainInstance = true
        // pre-load drawables for player buttons
        lifecycleScope.launch(Dispatchers.IO) {
            val color = context.onPrimaryColor()
            mPlayDrawable =
                    icon(CommunityMaterial.Icon2.cmd_play).color(IconicsColor.colorInt(color))
            mPauseDrawable =
                    icon(CommunityMaterial.Icon2.cmd_pause).color(IconicsColor.colorInt(color))
            mStoppedDrawable =
                    icon(CommunityMaterial.Icon2.cmd_stop).color(IconicsColor.colorInt(color))
            mErrorDrawable = icon(CommunityMaterial.Icon.cmd_alert_circle_outline).color(
                    IconicsColor.colorInt(color)
            )
            mNotInFavoritesDrawable =
                    icon(CommunityMaterial.Icon2.cmd_star_outline).color(IconicsColor.colorInt(color))
            mInFavoritesDrawable =
                    icon(CommunityMaterial.Icon2.cmd_star).color(IconicsColor.colorInt(context.secondaryColor()))
            mAlbumArtPlaceholderDrawable =
                    icon(CommunityMaterial.Icon.cmd_album).color(IconicsColor.colorInt(color))
                            .size(IconicsSize.dp(dimen(R.dimen.song_albumArt_size).toInt()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.playerState.observe(this) { if (it is RemoteData.Success) onPlayerStateChanged(it.value) }
        Timber.d("Player created")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        song_title.isSelected = true
        song_description.isSelected = true

        song_play_pause.onClick { changePlaybackState() }
        song_favorite.onClick { changeFavoriteStatus() }
        view.setOnTouchListener { _, event ->
            mGestureDetector.onTouchEvent(event)
            true
        }
        song_progress.progress = mPlayerState.progress
    }

    private fun changePlaybackState() = lifecycleScope.launch(Dispatchers.IO) {
        if (!JMusicBot.currentState.isConnected) return@launch
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

    private fun onPlayerStateChanged(newState: PlayerState) =
            lifecycleScope.launch(Dispatchers.Default) {
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
                    song_album_art.load(song.albumArtUrl) { placeholder(mAlbumArtPlaceholderDrawable) }
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
}
