package me.iberger.enq.gui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_current_song.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity.Companion.favorites
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.PlayerState
import me.iberger.jmusicbot.data.PlayerStates
import me.iberger.jmusicbot.data.Song
import me.iberger.jmusicbot.listener.PlayerUpdateListener
import timber.log.Timber

class CurrentSongFragment : Fragment(), PlayerUpdateListener {
    companion object {

        fun newInstance(): CurrentSongFragment = CurrentSongFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private var mPlayerState: PlayerState = PlayerState(PlayerStates.STOP, null)
    private var mShowSkip = false

    private lateinit var mPlayDrawable: IconicsDrawable
    private lateinit var mPauseDrawable: IconicsDrawable
    private lateinit var mStoppedDrawable: IconicsDrawable
    private lateinit var mSkipDrawable: IconicsDrawable
    private lateinit var mErrorDrawable: IconicsDrawable

    private lateinit var mFavoritesAddDrawable: IconicsDrawable
    private lateinit var mFavoritesDeleteDrawable: IconicsDrawable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicBot.instance.startPlayerUpdates(this@CurrentSongFragment)
        // pre-load drawables for player buttons
        mBackgroundScope.launch {
            val color = Color.WHITE
            mPlayDrawable = IconicsDrawable(context, CommunityMaterial.Icon2.cmd_play).color(color)
            mPauseDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon2.cmd_pause).color(color)
            mStoppedDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon2.cmd_stop).color(color)
            mSkipDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_fast_forward).color(color)
            mErrorDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon.cmd_alert_circle_outline).color(
                        color
                    )

            mFavoritesAddDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon2.cmd_star_outline).color(color)
            mFavoritesDeleteDrawable =
                    IconicsDrawable(context, CommunityMaterial.Icon2.cmd_star).color(
                        ContextCompat.getColor(context!!, R.color.favorites)
                    )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_current_song, container, false)
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
        if (mShowSkip) {
            try {
                MusicBot.instance.skip().await()
            } catch (e: Exception) {
                Timber.e(e)
                mUIScope.launch { context?.toastShort(R.string.msg_no_permission) }
            } finally {
                return@launch
            }
        }
        when (mPlayerState.state) {
            PlayerStates.STOP -> MusicBot.instance.play().await()
            PlayerStates.PLAY -> MusicBot.instance.pause().await()
            PlayerStates.PAUSE -> MusicBot.instance.play().await()
            PlayerStates.ERROR -> MusicBot.instance.play().await()
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


    override fun onPlayerStateChanged(newState: PlayerState) {
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
                    song_play_pause.setImageDrawable(mStoppedDrawable)
                    song_favorite.visibility = View.GONE
                    return@launch
                }
            }
            if (mShowSkip) song_play_pause.setImageDrawable(mSkipDrawable)
            val songEntry = newState.songEntry!!
            val song = songEntry.song
            // fill in song metadata
            song_title.text = song.title
            song_description.text = song.description
            song.albumArtUrl?.also { Picasso.get().load(it).into(song_album_art) }
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

    override fun onUpdateError(e: Exception) {
        Timber.e(e)
        mUIScope.launch {
            Toast.makeText(
                context,
                "Error when updating player state",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
