package com.dinaraparanid.prima.viewmodels.mvvm

import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.View
import carbon.widget.Button
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.fragments.guess_the_melody.GtmGameFragment
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

/**
 * MVVM [ViewModel] for
 * [com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment]
 */

class GtmGameViewModel(
    private val fragment: WeakReference<GtmGameFragment>,
    private var _trackNumber: Int,
    private val _tracks: AbstractPlaylist,
    private val playbackLength: Byte,
    private val buttonWithCorrectTrack: Button,
    private var _score: Int = 0,
    private val unsolvedTracks: AbstractPlaylist = DefaultPlaylist()
): ViewModel() {
    private val correctTrack = _tracks[_trackNumber - 1]

    @JvmField
    internal val tracks = ((_tracks - correctTrack).shuffled().take(3) + correctTrack).shuffled()

    internal inline val score
        @JvmName("getScore")
        get() = "${fragment.unchecked.resources.getString(R.string.score)} $_score"

    internal inline val trackNumber
        @JvmName("getTrackNumber")
        get() = "${fragment.unchecked.resources.getString(R.string.gtm_track_number)} $_trackNumber"

    internal inline val playButtonImage
        @JvmName("getPlayButtonImage")
        get() = ViewSetter.getPlayButtonImage(isPlaying)

    internal inline val nextOrFinishButtonTitle
        @JvmName("getNextOrFinishButtonTitle")
        get() = fragment.unchecked.resources.getString(
            if (_trackNumber == _tracks.size) R.string.results else R.string.gtm_next_track
        )

    private var isNextButtonClickable = false
    private var isTracksButtonsClickable = true

    private val startPosition = (correctTrack.duration - playbackLength).let {
        if (it <= 0) 0 else Random.nextLong(it)
    }.toInt()

    private var isPlaying = false
    private val playbackLock = ReentrantLock()
    private val playbackCondition = playbackLock.newCondition()
    private val playbackExecutor = Executors.newSingleThreadExecutor()

    private inline var musicPlayer
        get() = fragment.unchecked.musicPlayer
        set(value) { fragment.unchecked.musicPlayer = value }

    @Synchronized
    @JvmName("onPlayButtonClicked")
    internal fun onPlayButtonClicked() = when {
        isPlaying -> {
            isPlaying = false
            fragment.unchecked.run {
                releaseMusicPlayer()
                setPlayButtonImage(isPlaying)
            }
        }

        else -> {
            isPlaying = true

            musicPlayer = MediaPlayer().apply {
                setDataSource(correctTrack.path)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                prepareAsync()
                isLooping = false
                seekTo(startPosition)
                start()
            }

            playbackExecutor.execute {
                playbackLock.withLock {
                    playbackCondition.await(playbackLength.toLong(), TimeUnit.SECONDS)
                    isPlaying = false
                    fragment.unchecked.releaseMusicPlayer()
                }
            }
        }
    }

    @Synchronized
    @JvmName("onTrackButtonClicked")
    internal fun onTrackButtonClicked(v: View) {
        if (isTracksButtonsClickable) {
            _trackNumber++
            isNextButtonClickable = true
            isTracksButtonsClickable = false
            buttonWithCorrectTrack.setBackgroundColor(Color.GREEN)

            fragment.unchecked.run {
                setNextButtonClickable(isNextButtonClickable)
                setTracksButtonsClickable(isTracksButtonsClickable)
            }

            when {
                v === buttonWithCorrectTrack -> _score++

                else -> {
                    v.setBackgroundColor(Color.RED)
                    unsolvedTracks.add(correctTrack)
                }
            }
        }
    }

    @JvmName("onNextOrFinishButtonClicked")
    internal fun onNextOrFinishButtonClicked() {
        when (_trackNumber) {
            _tracks.size -> {
                // TODO: finish dialog
            }

            else -> fragment.unchecked
                .requireActivity()
                .supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.gtm_fragment_container,
                    GtmGameFragment.newInstance(
                        _score,
                        _trackNumber,
                        _tracks,
                        playbackLength,
                        unsolvedTracks
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }
}