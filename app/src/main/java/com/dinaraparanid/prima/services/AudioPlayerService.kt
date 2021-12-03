package com.dinaraparanid.prima.services

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.*
import android.media.AudioManager.*
import android.media.MediaPlayer.*
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import arrow.core.None
import arrow.core.Some
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.playbackParam
import com.dinaraparanid.prima.utils.extensions.unwrap
import com.dinaraparanid.prima.utils.polymorphism.AbstractService
import com.dinaraparanid.prima.utils.polymorphism.getFromWorkerThreadAsync
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.utils.polymorphism.runOnWorkerThread
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import java.io.FileInputStream

/**
 * [Service] to play music
 */

class AudioPlayerService : AbstractService(),
    OnCompletionListener,
    OnPreparedListener,
    OnErrorListener,
    OnSeekCompleteListener,
    OnInfoListener,
    OnBufferingUpdateListener,
    OnAudioFocusChangeListener {
    internal companion object {
        private const val ACTION_PLAY = "com.dinaraparanid.prima.media.ACTION_PLAY"
        private const val ACTION_PAUSE = "com.dinaraparanid.prima.media.ACTION_PAUSE"
        private const val ACTION_PREVIOUS = "com.dinaraparanid.prima.media.ACTION_PREVIOUS"
        private const val ACTION_NEXT = "com.dinaraparanid.prima.media.ACTION_NEXT"
        private const val ACTION_STOP = "com.dinaraparanid.prima.media.ACTION_STOP"
        private const val ACTION_LOOP_TRACK = "com.dinaraparanid.prima.media.ACTION_LOOP_TRACK"
        private const val ACTION_NO_LOOP = "com.dinaraparanid.prima.media.ACTION_NO_LOOP"
        private const val ACTION_LOOP_PLAYLIST = "com.dinaraparanid.prima.media.ACTION_LOOP_PLAYLIST"
        private const val ACTION_LIKE = "com.dinaraparanid.prima.media.ACTION_LIKE"
        private const val ACTION_NO_LIKE = "com.dinaraparanid.prima.media.ACTION_NO_LIKE"
        private const val ACTION_REMOVE = "com.dinaraparanid.prima.media.ACTION_REMOVE"
        private const val MEDIA_CHANNEL_ID = "media_playback_channel"
        private const val NOTIFICATION_ID = 101

        internal const val Broadcast_PLAY_NEW_TRACK = "com.dinaraparanid.prima.PlayNewTrack"
        internal const val Broadcast_PLAY_NEXT_AND_UPDATE_UI = "com.dinaraparanid.prima.PlayNextAndUpdateUI"
        internal const val Broadcast_PLAY_NEXT_OR_STOP = "com.dinaraparanid.prima.PlayNextOrStop"
        internal const val Broadcast_HIGHLIGHT_TRACK = "com.dinaraparanid.prima.HighlightTrack"
        internal const val Broadcast_CUSTOMIZE = "com.dinaraparanid.prima.Customize"
        internal const val Broadcast_RELEASE_AUDIO_VISUALIZER = "com.dinaraparanid.prima.ReleaseAudioVisualizer"
        internal const val Broadcast_INIT_AUDIO_VISUALIZER = "com.dinaraparanid.prima.InitAudioVisualizer"
        internal const val Broadcast_PREPARE_FOR_PLAYING = "com.dinaraparanid.prima.PrepareForPlaying"
        internal const val Broadcast_UPDATE_LOOPING = "com.dinaraparanid.prima.UpdateLooping"
        internal const val Broadcast_UPDATE_FAVOURITE_TRACKS_FRAGMENT = "com.dinaraparanid.prima.UpdateFavouriteTracksFragment"

        internal const val UPD_IMAGE_ARG = "upd_image"

        @Deprecated("Like button is not used anymore. Replaced by audio recording")
        internal const val Broadcast_SET_LIKE_BUTTON_IMAGE = "com.dinaraparanid.prima.SetLikeButtonImage"

        @Deprecated("Like button is not used anymore. Replaced by audio recording")
        internal const val LIKE_IMAGE_ARG = "like_image"
    }

    internal enum class PlaybackStatus {
        PLAYING, PAUSED
    }

    private var mediaPlayer: MediaPlayer? = null

    // MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSession? = null
    private var transportControls: MediaController.TransportControls? = null

    // TrackFocus
    private var audioManager: AudioManager? = null

    private var resumePosition = 0
    private var isStarted = false
    private var startFromLooping = false
    private var startFromPause = false
    private var isLiked = false

    // Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private lateinit var notificationAlbumImage: Bitmap

    override val coroutineScope: CoroutineScope
        get() = this

    internal inline val curTrack
        get() = (application as MainApplication).run {
            curPath.takeIf { it != Params.NO_PATH }
                ?.let {
                    Some(
                        curPlaylist.run { get(indexOfFirst { track -> track.path == it }) }
                    )
                }
                ?: None
        }

    private inline val curPath
        get() = (application as MainApplication).curPath

    private inline val curInd
        get() = (application as MainApplication)
            .curPlaylist.indexOfFirst { it.path == curPath }

    internal inline val isLooping1
        get() = Params.instance.loopingStatus == Params.Companion.Looping.TRACK

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            // Pause track on ACTION_AUDIO_BECOMING_NOISY

            runOnWorkerThread {
                pauseMedia(isLocking = true)
                buildNotification(PlaybackStatus.PAUSED, isLocking = true)
            }
        }
    }

    private val playNewTrackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            runOnWorkerThread {
                audioFocusHelp(isLocking = true)
                (application as MainApplication).sendBroadcast(Intent(Broadcast_HIGHLIGHT_TRACK))

                // A PLAY_NEW_TRACK action received
                // Reset mediaPlayer to play the new Track

                stopMedia(isLocking = true)
                mediaPlayer?.reset()
                initMediaPlayer(isLocking = true)
                updateMetaData(true, isLocking = true)
                mediaPlayer!!.isLooping = isLooping1
                buildNotification(PlaybackStatus.PLAYING, isLocking = true)
            }
        }
    }

    private val resumePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            runOnWorkerThread {
                resumeMedia(
                    intent
                        ?.getIntExtra(MainActivity.RESUME_POSITION_ARG, resumePosition)
                        ?.takeIf { it != -1 } ?: resumePosition,
                    isLocking = true
                )

                updateMetaData(false, isLocking = true)
                buildNotification(PlaybackStatus.PLAYING, isLocking = true)
            }
        }
    }

    private val pausePlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            runOnWorkerThread {
                pauseMedia(isLocking = true)
                updateMetaData(false, isLocking = true)
                buildNotification(PlaybackStatus.PAUSED, isLocking = true)
            }
        }
    }

    private val setLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            runOnWorkerThread {
                if (mediaPlayer == null)
                    initMediaPlayer(isLocking = true)

                mediaPlayer!!.isLooping = isLooping1
                StorageUtil.instance.storeLooping(Params.instance.loopingStatus)

                buildNotification(
                    when {
                        mediaPlayer!!.isPlaying -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    isLocking = true
                )
            }
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            runOnWorkerThread {
                resumePosition = mediaPlayer?.currentPosition ?: run {
                    initMediaPlayer(isLocking = true)
                    StorageUtil.instance.loadTrackPauseTime()
                }

                savePauseTime(isLocking = true)
                removeNotification(isLocking = true)
                sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, false) })
            }
        }
    }

    private val updateNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread {
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    isLocking = true
                )
            }
        }
    }

    private val removeNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            runOnWorkerThread { removeNotification(isLocking = true) }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.

        runOnWorkerThread {
            isLiked = FavouriteRepository.instance.getTrackAsync(curPath).await() != null
        }

        callStateListener()

        registerBecomingNoisyReceiver()
        registerPlayNewTrack()
        registerResume()
        registerPause()
        registerSetLooping()
        registerStop()
        registerUpdateNotification()
        registerRemoveNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel()

            // Load data from SharedPreferences
            val storage = StorageUtil.instance
            val loadResume = intent!!.getIntExtra(MainActivity.RESUME_POSITION_ARG, -1)

            startFromLooping = intent.action?.let { it == MainActivity.LOOPING_PRESSED_ARG }
                ?: false
            startFromPause = intent.action?.let { it == MainActivity.PAUSED_PRESSED_ARG }
                ?: false

            runOnWorkerThread {
                resumePosition = when {
                    loadResume != -1 -> loadResume
                    else -> when {
                        resumePosition != 0 -> resumePosition
                        else -> storage.loadTrackPauseTime().takeIf { it != -1 } ?: 0
                    }
                }
            }
        } catch (e: Exception) {
            runOnWorkerThread {
                resumePosition = mediaPlayer?.currentPosition ?: run {
                    initMediaPlayer(isLocking = true)
                    StorageUtil.instance.loadTrackPauseTime()
                }

                savePauseTime(isLocking = true)
                removeNotification(isLocking = true)
            }
        }

        runOnWorkerThread {
            audioFocusHelp(isLocking = true)
            handleIncomingActions(intent, isLocking = true)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            runOnWorkerThread { stopMedia(isLocking = true) }

            (application as MainApplication).run {
                equalizer.release()
                bassBoost?.release()
                presetReverb?.release()
            }

            mediaPlayer!!.release()
        }

        removeTrackFocus()

        // Disable the PhoneStateListener

        if (phoneStateListener != null)
            telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)

        runOnWorkerThread {
            removeNotification(isLocking = true)
            StorageUtil.instance.clearCachedPlaylist()
        }

        unregisterReceiver(playNewTrackReceiver)
        unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(resumePlayingReceiver)
        unregisterReceiver(pausePlayingReceiver)
        unregisterReceiver(setLoopingReceiver)
        unregisterReceiver(stopReceiver)
        unregisterReceiver(updateNotificationReceiver)
        unregisterReceiver(removeNotificationReceiver)
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int): Unit = Unit

    override fun onCompletion(mp: MediaPlayer) {
        // Invoked when playback of a media source has completed.

        runOnWorkerThread {
            stopMedia(isLocking = true)
            sendBroadcast(
                when (Params.instance.loopingStatus) {
                    Params.Companion.Looping.TRACK -> Intent(Broadcast_PLAY_NEW_TRACK)
                    Params.Companion.Looping.PLAYLIST -> Intent(Broadcast_PLAY_NEXT_AND_UPDATE_UI)
                    Params.Companion.Looping.NONE -> Intent(Broadcast_PLAY_NEXT_OR_STOP)
                }
            )
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked when there has been an error during an asynchronous operation
        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )

            MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )

            MEDIA_ERROR_UNKNOWN -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }

        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean = false

    override fun onPrepared(mp: MediaPlayer) {
        when {
            startFromLooping -> startFromLooping = false

            startFromPause -> {
                startFromPause = false
                runOnWorkerThread {
                    pauseMedia(isLocking = true)
                    buildNotification(PlaybackStatus.PAUSED, isLocking = true)
                }
            }

            isStarted -> runOnWorkerThread {
                playMedia(isLocking = true).apply {
                    buildNotification(PlaybackStatus.PLAYING, isLocking = true)
                }
            }

            else -> {
                isStarted = true

                when ((application as MainApplication).startPath) {
                    None -> runOnWorkerThread { playMedia(isLocking = true) }

                    else -> runOnWorkerThread {
                        when ((application as MainApplication).startPath.unwrap()) {
                            curPath -> resumeMedia(isLocking = true)
                            else -> playMedia(isLocking = true)
                        }
                    }
                }
            }
        }
    }

    override fun onSeekComplete(mp: MediaPlayer): Unit = Unit

    override fun onAudioFocusChange(focusState: Int) {
        when (focusState) {
            AUDIOFOCUS_GAIN -> {
                // Resume playback

                /*if (mediaPlayer == null) initMediaPlayer(true)
                else if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
                mediaSession!!.isActive = true
                buildNotification(PlaybackStatus.PLAYING, false)*/
            }

            AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time

                if (mediaPlayer != null) {
                    if (mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.stop()
                        resumePosition = mediaPlayer!!.currentPosition
                        runOnWorkerThread { savePauseTime(isLocking = true) }
                    }

                    mediaPlayer!!.reset()
                    mediaPlayer!!.release()
                    mediaPlayer = null
                    sendBroadcast(Intent(Broadcast_RELEASE_AUDIO_VISUALIZER))
                }

                runOnWorkerThread { buildNotification(PlaybackStatus.PAUSED, isLocking = true) }
            }

            AUDIOFOCUS_LOSS_TRANSIENT -> runOnWorkerThread {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume

                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer!!.pause()
                        resumePosition = mediaPlayer!!.currentPosition
                        savePauseTime(isLocking = true)
                    }
                    buildNotification(PlaybackStatus.PAUSED, isLocking = true)
                } catch (e: Exception) {
                    initMediaPlayer(true, isLocking = true)
                }
            }

            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level

                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
        }
    }

    private fun requestTrackFocus() =
        (getSystemService(AUDIO_SERVICE) as AudioManager).let { audioManager ->
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> audioManager.requestAudioFocus(
                    AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(this)
                        .build()
                )

                else -> audioManager.requestAudioFocus(
                    this,
                    STREAM_MUSIC,
                    AUDIOFOCUS_GAIN
                )
            }
        }

    private inline val isTrackFocusGranted: Boolean
        get() = requestTrackFocus() == AUDIOFOCUS_REQUEST_GRANTED

    private fun removeTrackFocus() =
        AUDIOFOCUS_REQUEST_GRANTED == audioManager?.abandonAudioFocus(this)

    private suspend fun initMediaPlayerNoLock(resume: Boolean) {
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer()

        mediaPlayer!!.apply {
            setOnCompletionListener(this@AudioPlayerService)
            setOnErrorListener(this@AudioPlayerService)
            setOnPreparedListener(this@AudioPlayerService)
            setOnBufferingUpdateListener(this@AudioPlayerService)
            setOnSeekCompleteListener(this@AudioPlayerService)
            setOnInfoListener(this@AudioPlayerService)

            reset()

            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            try {
                setDataSource(FileInputStream(curPath).fd)
            } catch (e: Exception) {
                e.printStackTrace()
                resumePosition = mediaPlayer!!.currentPosition
                savePauseTime(isLocking = false)
            }

            isLooping = isLooping1

            try {
                prepare()
                if (resume) seekTo(resumePosition)

                (application as MainApplication).run {
                    musicPlayer = this@apply

                    if (Params.instance.isStartingWithEqualizer)
                        startEqualizer()
                }

                buildNotification(PlaybackStatus.PLAYING, isLocking = false)
            } catch (e: Exception) {
                e.printStackTrace()
                Exception(curPath).printStackTrace()

                runOnUIThread {
                    Toast.makeText(
                        applicationContext,
                        resources.getString(R.string.file_is_corrupted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * initializes and prepares [MediaPlayer] and sets actions
     */

    internal suspend fun initMediaPlayer(resume: Boolean = false, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { initMediaPlayerNoLock(resume) }
        else -> initMediaPlayerNoLock(resume)
    }

    private suspend fun initEqualizerNoLock() {
        EqualizerSettings.instance.isEditing = true

        if (EqualizerSettings.instance.equalizerModel == null) {
            EqualizerSettings.instance.equalizerModel = EqualizerModel.newInstance().apply {
                reverbPreset = PresetReverb.PRESET_NONE
                bassStrength = (1000 / 19).toShort()
            }
        }

        val audioSessionId = mediaPlayer!!.audioSessionId
        val app = application as MainApplication

        app.equalizer = Equalizer(0, audioSessionId)

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            app.bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = EqualizerSettings.instance.isEqualizerEnabled
                properties = BassBoost.Settings(properties.toString()).apply {
                    strength = StorageUtil.instance.loadBassStrength()
                }
            }

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q)
            app.presetReverb = PresetReverb(0, audioSessionId).apply {
                try {
                    preset = StorageUtil.instance.loadReverbPreset()
                } catch (ignored: Exception) {
                    // not supported
                }
                enabled = EqualizerSettings.instance.isEqualizerEnabled
            }

        app.equalizer.enabled = EqualizerSettings.instance.isEqualizerEnabled

        val seekBarPoses = StorageUtil.instance.loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        when (EqualizerSettings.instance.presetPos) {
            0 -> (0 until app.equalizer.numberOfBands).forEach {
                app.equalizer.setBandLevel(
                    it.toShort(),
                    seekBarPoses[it].toShort()
                )
            }

            else -> app.equalizer.usePreset(EqualizerSettings.instance.presetPos.toShort())
        }
    }

    private suspend fun initEqualizer(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { initEqualizerNoLock() }
        else -> initEqualizerNoLock()
    }

    private suspend fun playMediaNoLock() {
        if (mediaPlayer == null)
            initMediaPlayer(isLocking = false)

        requestTrackFocus()
        sendBroadcast(Intent(Broadcast_INIT_AUDIO_VISUALIZER))

        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.run {
                start()

                if (EqualizerSettings.instance.isEqualizerEnabled) {
                    initEqualizer(isLocking = false)

                    val loader = StorageUtil.instance
                    val pitch = loader.loadPitch()
                    val speed = loader.loadSpeed()

                    playbackParams = PlaybackParams()
                        .setPitch(pitch)
                        .setSpeed(speed)
                }
            }

            buildNotification(PlaybackStatus.PLAYING, isLocking = false)
            sendBroadcast(Intent(Broadcast_PREPARE_FOR_PLAYING).apply { putExtra(UPD_IMAGE_ARG, true) })
        }
    }

    private suspend fun playMedia(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { playMediaNoLock() }
        else -> playMediaNoLock()
    }

    private suspend fun stopMediaNoLock() {
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
            resumePosition = mediaPlayer!!.currentPosition
            savePauseTime(isLocking = false)
        }
    }

    internal suspend fun stopMedia(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { stopMediaNoLock() }
        else -> stopMediaNoLock()
    }

    private suspend fun pauseMediaNoLock() {
        if (mediaPlayer == null)
            return

        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition
            savePauseTime(isLocking = false)
            sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, false) })
        }
    }

    internal suspend fun pauseMedia(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pauseMediaNoLock() }
        else -> pauseMediaNoLock()
    }

    private suspend fun resumeMediaNoLock(resumePos: Int) {
        if (mediaPlayer == null)
            initMediaPlayer(isLocking = false)

        requestTrackFocus()

        mediaPlayer!!.run {
            val sv = isLooping
            seekTo(resumePos)

            if (EqualizerSettings.instance.isEqualizerEnabled) {
                initEqualizer(isLocking = false)

                val loader = StorageUtil.instance

                try {
                    playbackParams = PlaybackParams()
                        .setPitch(loader.loadPitch().playbackParam)
                        .setSpeed(loader.loadSpeed().playbackParam)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            start()
            isLooping = sv
        }

        sendBroadcast(Intent(Broadcast_INIT_AUDIO_VISUALIZER))
        sendBroadcast(Intent(Broadcast_PREPARE_FOR_PLAYING).apply { putExtra(UPD_IMAGE_ARG, false) })
    }

    internal suspend fun resumeMedia(resumePos: Int = resumePosition, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { resumeMediaNoLock(resumePos) }
        else -> resumeMediaNoLock(resumePos)
    }

    private suspend fun skipToNextNoLock() {
        (application as MainApplication).run {
            val curIndex = (curInd + 1).let { if (it == curPlaylist.size) 0 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil.instance.storeTrackPath(curPath)
        stopMedia(false)
        mediaPlayer!!.reset()
        initMediaPlayer(isLocking = false)
        mediaPlayer!!.isLooping = isLooping1
    }

    internal suspend fun skipToNext(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { skipToNextNoLock() }
        else -> skipToNextNoLock()
    }

    private suspend fun skipToPreviousNoLock() {
        (application as MainApplication).run {
            val curIndex = (curInd - 1).let { if (it < 0) curPlaylist.size - 1 else it }
            curPath = curPlaylist[curIndex].path
        }

        // Update stored index
        StorageUtil.instance.storeTrackPath(curPath)
        stopMedia(false)

        mediaPlayer!!.reset()
        initMediaPlayer(isLocking = false)
        mediaPlayer!!.isLooping = isLooping1
    }

    internal suspend fun skipToPrevious(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { skipToPreviousNoLock() }
        else -> skipToPreviousNoLock()
    }

    private fun registerBecomingNoisyReceiver() = registerReceiver(
        becomingNoisyReceiver,
        IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    )

    private fun registerPlayNewTrack() =
        registerReceiver(playNewTrackReceiver, IntentFilter(MainActivity.Broadcast_PLAY_NEW_TRACK))

    private fun registerResume() =
        registerReceiver(resumePlayingReceiver, IntentFilter(MainActivity.Broadcast_RESUME))

    private fun registerPause() =
        registerReceiver(pausePlayingReceiver, IntentFilter(MainActivity.Broadcast_PAUSE))

    private fun registerSetLooping() =
        registerReceiver(setLoopingReceiver, IntentFilter(MainActivity.Broadcast_LOOPING))

    private fun registerStop() =
        registerReceiver(stopReceiver, IntentFilter(MainActivity.Broadcast_STOP))

    private fun registerUpdateNotification() = registerReceiver(
        updateNotificationReceiver,
        IntentFilter(MainActivity.Broadcast_UPDATE_NOTIFICATION)
    )

    private fun registerRemoveNotification() = registerReceiver(
        removeNotificationReceiver,
        IntentFilter(MainActivity.Broadcast_REMOVE_NOTIFICATION)
    )

    /**
     * Handle PhoneState changes
     */
    private fun callStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING ->
                        if (mediaPlayer != null) {
                            val wasPlaying = mediaPlayer!!.isPlaying
                            runOnWorkerThread {
                                pauseMedia(isLocking = true)
                                ongoingCall = wasPlaying
                                buildNotification(PlaybackStatus.PAUSED, isLocking = true)
                                savePauseTime(isLocking = true)
                                sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, false) })
                            }
                        }

                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing

                        if (mediaPlayer != null)
                            if (ongoingCall) {
                                ongoingCall = false

                                runOnWorkerThread {
                                    resumeMedia(isLocking = true)
                                    buildNotification(PlaybackStatus.PLAYING, isLocking = true)
                                    sendBroadcast(Intent(Broadcast_CUSTOMIZE)
                                        .apply { putExtra(UPD_IMAGE_ARG, false) })
                                }
                            }
                }
            }
        }

        // Register the listener with the telephony manager
        // Listen for changes to the device call state

        telephonyManager!!.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    private suspend fun initMediaSessionNoLock() {
        if (mediaSessionManager != null)
            return

        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE)!! as MediaSessionManager
        mediaSession = MediaSession(applicationContext, "AudioPlayer")
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            mediaSession!!.setFlags(
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                    .or(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            )

        updateMetaData(true, isLocking = false)


        mediaSession!!.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                runOnWorkerThread {
                    resumeMedia(isLocking = false)
                    buildNotification(PlaybackStatus.PLAYING, isLocking = false)
                    sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, false) })
                }
            }

            override fun onPause() {
                super.onPause()
                runOnWorkerThread {
                    pauseMedia(isLocking = false)
                    updateMetaData(false, isLocking = false)
                    buildNotification(PlaybackStatus.PAUSED, isLocking = false)
                    savePauseTime(isLocking = false)
                    sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, false) })
                }
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                runOnWorkerThread {
                    skipToNext(isLocking = false)
                    updateMetaData(true, isLocking = false)
                    buildNotification(PlaybackStatus.PLAYING, isLocking = false)
                    sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, true) })
                }
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                runOnWorkerThread {
                    skipToPrevious(isLocking = false)
                    updateMetaData(true, isLocking = false)
                    buildNotification(PlaybackStatus.PLAYING, isLocking = false)
                    sendBroadcast(Intent(Broadcast_CUSTOMIZE).apply { putExtra(UPD_IMAGE_ARG, true) })
                }
            }

            override fun onStop() {
                super.onStop()
                runOnWorkerThread {
                    removeNotification(isLocking = false)
                    resumePosition = mediaPlayer?.currentPosition ?: run {
                        initMediaPlayer(isLocking = false)
                        StorageUtil.instance.loadTrackPauseTime()
                    }
                    savePauseTime(isLocking = false)
                }
            }
        })

        mediaSession!!.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY
                            or PlaybackState.ACTION_PLAY_PAUSE
                            or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                            or PlaybackState.ACTION_PAUSE
                            or PlaybackState.ACTION_SKIP_TO_NEXT
                            or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                    PlaybackState.STATE_PAUSED,
                    resumePosition.toLong(),
                    1.0F,
                    SystemClock.elapsedRealtime()
                )
                .build()
        )
    }

    /**
     * initializes [MediaSession] and [Notification] actions
     */

    private suspend fun initMediaSession(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { initMediaSessionNoLock() }
        else -> initMediaSessionNoLock()
    }

    private suspend fun updateMetaDataNoLock(updImage: Boolean) {
        val activeTrack = curTrack.unwrap()

        mediaSession!!.setMetadata(
            MediaMetadata.Builder()
                .putBitmap(
                    MediaMetadata.METADATA_KEY_ALBUM_ART,
                    when {
                        updImage -> (application as MainApplication)
                            .getAlbumPictureAsync(
                                curPath,
                                Params.instance.isPlaylistsImagesShown
                            )
                            .await()
                            .also { notificationAlbumImage = it }
                        else -> notificationAlbumImage
                    }
                )
                .putString(MediaMetadata.METADATA_KEY_ARTIST, activeTrack.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, activeTrack.playlist)
                .putString(MediaMetadata.METADATA_KEY_TITLE, activeTrack.title)
                .build()
        )
    }

    /**
     * Updates metadata for notification asynchronously
     */

    internal suspend fun updateMetaData(updImage: Boolean, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { updateMetaDataNoLock(updImage) }
        else -> updateMetaDataNoLock(updImage)
    }

    /**
     * For Android Api 28+
     *
     * Build a new notification according to
     * the current state of the MediaPlayer
     * @param playbackStatus playing or paused
     */

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun buildNotificationPie(playbackStatus: PlaybackStatus) {
        val activeTrack = curTrack.unwrap()

        val notificationView = getFromWorkerThreadAsync {
            RemoteViews(
                applicationContext.packageName,
                R.layout.notification_layout
            ).apply {
                setTextViewText(R.id.notification_title, activeTrack.title)
                setTextColor(R.id.notification_title, Color.WHITE)
                setTextViewTextSize(R.id.notification_title, TypedValue.COMPLEX_UNIT_SP, 16F)

                setTextViewText(R.id.notification_artist_album, activeTrack.artistAndAlbumFormatted)
                setTextColor(R.id.notification_artist_album, Color.WHITE)
                setTextViewTextSize(R.id.notification_artist_album, TypedValue.COMPLEX_UNIT_SP, 12F)

                setImageViewBitmap(
                    R.id.notification_album_image,
                    (application as MainApplication).getAlbumPictureAsync(
                        activeTrack.path,
                        true
                    ).await()
                )

                setImageViewResource(
                    R.id.notification_repeat_button, when (Params.instance.loopingStatus) {
                        Params.Companion.Looping.PLAYLIST -> R.drawable.repeat_white
                        Params.Companion.Looping.TRACK -> R.drawable.repeat_1_white
                        Params.Companion.Looping.NONE -> R.drawable.no_repeat_white
                    }
                )

                setImageViewResource(
                    R.id.notification_play_button, when (playbackStatus) {
                        PlaybackStatus.PLAYING -> R.drawable.pause_white
                        PlaybackStatus.PAUSED -> R.drawable.play_white
                    }
                )

                setImageViewResource(
                    R.id.notification_like_button, when {
                        FavouriteRepository.instance.getTrackAsync(activeTrack.path).await() != null ->
                            R.drawable.heart_like_white

                        else -> R.drawable.heart_white
                    }
                )

                setOnClickPendingIntent(
                    R.id.notification_repeat_button, when (Params.instance.loopingStatus) {
                        Params.Companion.Looping.PLAYLIST -> playbackAction(4, isLocking = false)
                        Params.Companion.Looping.TRACK -> playbackAction(5, isLocking = false)
                        Params.Companion.Looping.NONE -> playbackAction(6, isLocking = false)
                    }
                )

                setOnClickPendingIntent(R.id.notification_prev_track, playbackAction(3, isLocking = false))

                setOnClickPendingIntent(
                    R.id.notification_play_button, when (playbackStatus) {
                        PlaybackStatus.PLAYING -> playbackAction(1, isLocking = false)
                        PlaybackStatus.PAUSED -> playbackAction(0, isLocking = false)
                    }
                )

                setOnClickPendingIntent(R.id.notification_next_track, playbackAction(2, isLocking = false))

                setOnClickPendingIntent(
                    R.id.notification_like_button, when {
                        isLiked -> playbackAction(8, isLocking = false)
                        else -> playbackAction(7, isLocking = false)
                    }
                )

                setOnClickPendingIntent(R.id.notification_remove, playbackAction(9, isLocking = false))
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> startForeground(
                NOTIFICATION_ID,
                Notification.Builder(applicationContext, MEDIA_CHANNEL_ID)
                    .setStyle(Notification.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationView.await())
                    .setSmallIcon(R.drawable.octopus)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )

            else -> startForeground(
                NOTIFICATION_ID,
                Notification.Builder(applicationContext, MEDIA_CHANNEL_ID)
                    .setStyle(Notification.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationView.await())
                    .setSmallIcon(R.drawable.octopus)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .build()
            )
        }
    }

    /**
     * For Android Api 27-
     *
     * Builds Notification when playing or paused
     * @param playbackStatus playing or paused
     * @param updImage does track image need update
     */

    private suspend fun buildNotificationCompat(playbackStatus: PlaybackStatus, updImage: Boolean) {
        val play = R.drawable.play_white
        val pause = R.drawable.pause_white
        val prev = R.drawable.prev_track_white
        val next = R.drawable.next_track_white
        val repeatPlaylist = R.drawable.repeat_white
        val repeatTrack = R.drawable.repeat_1_white
        val noRepeat = R.drawable.no_repeat_white
        val noLike = R.drawable.heart_white
        val like = R.drawable.heart_like_white

        val playPauseAction: PendingIntent?
        val loopAction: PendingIntent?
        val likeAction: PendingIntent?

        // Build a new notification according to the current state of the MediaPlayer

        val playAction = when (playbackStatus) {
            PlaybackStatus.PLAYING -> {
                playPauseAction = playbackAction(1, isLocking = false)
                pause
            }

            PlaybackStatus.PAUSED -> {
                playPauseAction = playbackAction(0, isLocking = false)
                play
            }
        }

        val loopingAction = when (Params.instance.loopingStatus) {
            Params.Companion.Looping.PLAYLIST -> {
                loopAction = playbackAction(4, isLocking = false)
                repeatPlaylist
            }

            Params.Companion.Looping.TRACK -> {
                loopAction = playbackAction(5, isLocking = false)
                repeatTrack
            }

            Params.Companion.Looping.NONE -> {
                loopAction = playbackAction(6, isLocking = false)
                noRepeat
            }
        }

        val likingAction = when {
            isLiked -> {
                likeAction = playbackAction(8, isLocking = false)
                like
            }

            else -> {
                likeAction = playbackAction(7, isLocking = false)
                noLike
            }
        }

        val activeTrack = curTrack.unwrap()

        val customize = { builder: Notification.Builder ->
            async(Dispatchers.Default) {
                builder
                    .setStyle(
                        Notification.MediaStyle()                           // Attach our MediaSession token
                            .setMediaSession(mediaSession!!.sessionToken)   // Show our playback controls in the compat view
                            .setShowActionsInCompactView(1, 2, 3)
                    )                                                       // Set the Notification color
                    .setColor(Params.instance.primaryColor)                 // Set the large and small icons
                    .setLargeIcon(when {
                        updImage -> (application as MainApplication)
                            .getAlbumPictureAsync(
                                curPath,
                                Params.instance.isPlaylistsImagesShown
                            )
                            .await()
                            .also { notificationAlbumImage = it }
                        else -> notificationAlbumImage
                    })
                    .setSmallIcon(R.drawable.octopus)                       // Set Notification content information
                    .setSubText(activeTrack.playlist.let {
                        if (it == "<unknown>" ||
                            it == curPath.split('/').takeLast(2).first()
                        ) resources.getString(R.string.unknown_album) else it
                    })
                    .setContentText(activeTrack.artist
                        .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it })
                    .setContentTitle(activeTrack.title
                        .let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it })
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource("", loopingAction),
                            "looping",
                            loopAction
                        ).build()
                    )
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource("", prev),
                            "previous",
                            playbackAction(3, isLocking = false)
                        ).build()
                    )
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource("", playAction),
                            "pause",
                            playPauseAction
                        ).build()
                    )
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource("", next),
                            "next",
                            playbackAction(2, isLocking = false)
                        ).build()
                    )
                    .addAction(
                        Notification.Action.Builder(
                            Icon.createWithResource("", likingAction),
                            "like",
                            likeAction
                        ).build()
                    )
            }
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> customize(
                Notification.Builder(this, MEDIA_CHANNEL_ID)
            ).let {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> startForeground(
                        NOTIFICATION_ID,
                        it.await().build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )

                    else -> startForeground(NOTIFICATION_ID, it.await().build())
                }
            }

            else -> customize(Notification.Builder(this)).let {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> startForeground(
                        NOTIFICATION_ID,
                        it.await().build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )

                    else -> startForeground(NOTIFICATION_ID, it.await().build())
                }
            }
        }
    }

    private suspend fun buildNotificationNoLock(
        playbackStatus: PlaybackStatus,
        updImage: Boolean
    ) = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> when {
            Params.instance.isUsingAndroidNotification ->
                buildNotificationCompat(playbackStatus, updImage)

            else -> buildNotificationPie(playbackStatus)
        }

        else -> buildNotificationCompat(playbackStatus, updImage)
    }

    /**
     * Builds Notification when playing or paused
     * @param playbackStatus playing or paused
     * @param updImage does track image need update
     */

    internal suspend fun buildNotification(
        playbackStatus: PlaybackStatus,
        updImage: Boolean = true,
        isLocking: Boolean
    ) = when {
        isLocking -> mutex.withLock { buildNotificationNoLock(playbackStatus, updImage) }
        else -> buildNotificationNoLock(playbackStatus, updImage)
    }

    private fun playbackActionNoLock(actionNumber: Int): PendingIntent? {
        val playbackAction = Intent(this, AudioPlayerService::class.java)

        return when (actionNumber) {
            in 0..9 -> {
                playbackAction.action = when (actionNumber) {
                    0 -> ACTION_PLAY            // Play
                    1 -> ACTION_PAUSE           // Pause
                    2 -> ACTION_NEXT            // Next track
                    3 -> ACTION_PREVIOUS        // Previous track
                    4 -> ACTION_LOOP_PLAYLIST   // Playlist looping
                    5 -> ACTION_LOOP_TRACK      // Track looping
                    6 -> ACTION_NO_LOOP         // No looping
                    7 -> ACTION_LIKE            // Like track
                    8 -> ACTION_NO_LIKE         // Not like track
                    else -> ACTION_REMOVE       // Remove notification
                }

                PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }

            else -> null
        }
    }

    /**
     * 0 -> Play
     *
     * 1 -> Pause
     *
     * 2 -> Next track
     *
     * 3 -> Previous track
     *
     * 4 -> Set playlist looping
     *
     * 5 -> Set track looping
     *
     * 6 -> Set no looping
     *
     * 7 -> Remove like from track
     *
     * 8 -> Set like to track
     *
     * 9 -> Remove notification
     */

    @SuppressLint("UnspecifiedImmutableFlag")
    private suspend fun playbackAction(actionNumber: Int, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { playbackActionNoLock(actionNumber) }
        else -> playbackActionNoLock(actionNumber)
    }

    override suspend fun handleIncomingActionsNoLock(action: Intent?) {
        if (action == null || action.action == null) return
        val actionString = action.action

        when {
            actionString.equals(ACTION_PLAY, ignoreCase = true) ->
                transportControls!!.play()

            actionString.equals(ACTION_PAUSE, ignoreCase = true) ->
                transportControls!!.pause().apply {
                    try {
                        runOnWorkerThread {
                            resumePosition = mediaPlayer?.currentPosition ?: run {
                                initMediaPlayer(isLocking = false)
                                StorageUtil.instance.loadTrackPauseTime()
                            }
                            savePauseTime(isLocking = false)
                        }
                    } catch (e: Exception) {
                        // on close app error
                        removeNotification(isLocking = false)
                        stopSelf()
                    }
                }

            actionString.equals(ACTION_NEXT, ignoreCase = true) -> transportControls!!.skipToNext()

            actionString.equals(ACTION_PREVIOUS, ignoreCase = true) ->
                transportControls!!.skipToPrevious()

            actionString.equals(ACTION_LOOP_PLAYLIST, ignoreCase = true) ||
                    actionString.equals(ACTION_LOOP_TRACK, ignoreCase = true) ||
                    actionString.equals(ACTION_NO_LOOP, ignoreCase = true) -> runOnWorkerThread {
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    isLocking = false
                )

                sendBroadcast(Intent(Broadcast_UPDATE_LOOPING))
            }

            actionString.equals(ACTION_LIKE, ignoreCase = true) -> {
                isLiked = !isLiked
                FavouriteRepository.instance.addTrackAsync(curTrack.unwrap().asFavourite())
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    isLocking = false
                )

                sendBroadcast(Intent(Broadcast_UPDATE_FAVOURITE_TRACKS_FRAGMENT))
            }

            actionString.equals(ACTION_NO_LIKE, ignoreCase = false) -> {
                isLiked = !isLiked
                FavouriteRepository.instance.removeTrackAsync(curTrack.unwrap().asFavourite())
                buildNotification(
                    when (mediaPlayer?.isPlaying) {
                        true -> PlaybackStatus.PLAYING
                        else -> PlaybackStatus.PAUSED
                    },
                    isLocking = false
                )

                sendBroadcast(Intent(Broadcast_UPDATE_FAVOURITE_TRACKS_FRAGMENT))
            }

            actionString.equals(ACTION_REMOVE, ignoreCase = true) -> removeNotification(isLocking = false)

            actionString.equals(ACTION_STOP, ignoreCase = true) ->
                transportControls!!.stop().apply {
                    runOnWorkerThread {
                        resumePosition = mediaPlayer?.currentPosition ?: run {
                            initMediaPlayer(isLocking = false)
                            StorageUtil.instance.loadTrackPauseTime()
                        }
                    }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createChannel() = (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager)
        .createNotificationChannel(
            NotificationChannel(
                MEDIA_CHANNEL_ID,
                "Media playback",
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> NotificationManager.IMPORTANCE_LOW
                    else -> 0
                }
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
            }
        )

    private suspend fun savePauseTimeNoLock() {
        (application as MainApplication).savePauseTime()
    }

    /** Saves paused time of track */
    internal suspend fun savePauseTime(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { savePauseTimeNoLock() }
        else -> savePauseTimeNoLock()
    }

    private suspend fun audioFocusHelpNoLock() {
        if (!isTrackFocusGranted) {
            resumePosition = mediaPlayer?.currentPosition ?: run {
                initMediaPlayer(isLocking = false)
                StorageUtil.instance.loadTrackPauseTime()
            }
            savePauseTime(isLocking = false)
            removeNotification(isLocking = false)
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession(isLocking = false)
                initMediaPlayer(isLocking = false)
            } catch (e: Exception) {
                try {
                    resumePosition = mediaPlayer?.currentPosition ?: run {
                        initMediaPlayer(isLocking = false)
                        StorageUtil.instance.loadTrackPauseTime()
                    }
                    savePauseTime(isLocking = false)
                } catch (ignored: Exception) {
                    // on close app error
                }

                removeNotification(isLocking = false)
            }
        }

        when (mediaPlayer) {
            null -> (getSystemService(NOTIFICATION_SERVICE)!! as NotificationManager).cancelAll()

            else -> {
                (application as MainApplication).musicPlayer = mediaPlayer!!
                mediaPlayer!!.isLooping = isLooping1
            }
        }
    }

    internal suspend fun audioFocusHelp(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { audioFocusHelpNoLock() }
        else -> audioFocusHelpNoLock()
    }
}