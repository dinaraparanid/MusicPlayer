package com.dinaraparanid.prima

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import arrow.core.Either
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Contact
import com.dinaraparanid.prima.databases.entities.hidden.HiddenArtist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenPlaylist
import com.dinaraparanid.prima.databases.entities.hidden.HiddenTrack
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.*
import com.dinaraparanid.prima.dialogs.*
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMMainFragment
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.fragments.hidden.*
import com.dinaraparanid.prima.fragments.main_menu.DefaultArtistListFragment
import com.dinaraparanid.prima.fragments.main_menu.DefaultTrackListFragment
import com.dinaraparanid.prima.fragments.main_menu.MP3ConverterFragment
import com.dinaraparanid.prima.fragments.main_menu.UltimateCollectionFragment
import com.dinaraparanid.prima.fragments.main_menu.about_app.AboutAppFragment
import com.dinaraparanid.prima.fragments.main_menu.favourites.FavouriteArtistListFragment
import com.dinaraparanid.prima.fragments.main_menu.favourites.FavouriteTrackListFragment
import com.dinaraparanid.prima.fragments.main_menu.favourites.FavouritesFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.FontsFragment
import com.dinaraparanid.prima.fragments.main_menu.settings.SettingsFragment
import com.dinaraparanid.prima.fragments.main_menu.statistics.StatisticsFragment
import com.dinaraparanid.prima.fragments.main_menu.statistics.StatisticsHolderFragment
import com.dinaraparanid.prima.fragments.playing_panel_fragments.*
import com.dinaraparanid.prima.fragments.playing_panel_fragments.trimmer.ChooseContactFragment
import com.dinaraparanid.prima.fragments.playing_panel_fragments.trimmer.TrimFragment
import com.dinaraparanid.prima.fragments.track_collections.AlbumListFragment
import com.dinaraparanid.prima.fragments.track_collections.DefaultPlaylistListFragment
import com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment
import com.dinaraparanid.prima.fragments.track_collections.TrackCollectionsFragment
import com.dinaraparanid.prima.fragments.track_lists.AlbumTrackListFragment
import com.dinaraparanid.prima.fragments.track_lists.ArtistTrackListFragment
import com.dinaraparanid.prima.fragments.track_lists.CustomPlaylistTrackListFragment
import com.dinaraparanid.prima.fragments.track_lists.TrackListFoundFragment
import com.dinaraparanid.prima.services.AudioPlayerService
import com.dinaraparanid.prima.services.MicRecordService
import com.dinaraparanid.prima.services.PlaybackRecordService
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.extensions.setShadowColor
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.rustlibs.NativeLibrary
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.GeniusTrack
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.utils.web.github.GitHubFetcher
import com.dinaraparanid.prima.mvvmp.androidx.MainActivityViewModel
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import com.dinaraparanid.prima.mvvmp.view.dialogs.*
import com.dinaraparanid.prima.mvvmp.view.dialogs.SleepDialog
import com.gauravk.audiovisualizer.model.AnimSpeed
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.kaopiz.kprogresshud.KProgressHUD
import com.vmadalin.easypermissions.EasyPermissions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.system.exitProcess

/** Prima's main activity on which the entire application rests */

class MainActivity :
    AbstractActivity(),
    AbstractTrackListFragment.Callbacks,
    AbstractArtistListFragment.Callbacks,
    AbstractPlaylistListFragment.Callbacks,
    FontsFragment.Callbacks,
    TrackListFoundFragment.Callbacks,
    TrackChangeFragment.Callbacks,
    TrimFragment.Callbacks,
    ChooseContactFragment.Callbacks,
    GTMPlaylistSelectFragment.Callbacks,
    CurPlaylistTrackListFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener,
    UIUpdatable<Pair<AbstractTrack?, AbstractTrack>>,
    StatisticsUpdatable,
    EasyPermissions.PermissionCallbacks {
    private var _binding: Either<ActivityMainBarBinding, ActivityMainWaveBinding>? = null

    override val viewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    internal lateinit var sheetBehavior: BottomSheetBehavior<View>
    internal var curPlaylistFragment = WeakReference<CurPlaylistTrackListFragment>(null)

    private var playingCoroutine: Job? = null
    private var awaitDialog: Deferred<KProgressHUD>? = null
    private var actionBarSize = 0
    private var backClicksCount = 2

    override val mutex = Mutex()
    override val updateStyle = Statistics::withIncrementedAppWasOpened

    private val audioCommand = AtomicReference<Runnable>()
    private val sendIsRunning = AtomicBoolean()
    private val audioCondition = AsyncCondVar()

    private var isSeekBarDragging = false
    internal var isUpped = false

    internal val isBindingInitialized
        get() = _binding != null

    internal val awaitBindingInitCondition = AsyncCondVar()

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private var albumImageWidth = 0
    private var albumImageHeight = 0

    private var recordFilename = ""
    internal fun setRecordFilename(filename: String) {
        recordFilename = filename
    }

    private val smallAlbumImageAnimator by lazy {
        ObjectAnimator
            .ofFloat(binding.playingLayout.playingAlbumImage, "rotation", 0F, 360F)
            .apply {
                duration = 15000
                repeatCount = Animation.INFINITE
            }
    }

    private inline val binding
        get() = when (_binding) {
            is Either.Right -> Either.Right((_binding as Either.Right<ActivityMainWaveBinding>).value)
            is Either.Left -> Either.Left((_binding as Either.Left<ActivityMainBarBinding>).value)
            else -> throw NullPointerException("Main Activity binding is null")
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.drawerLayout
        get() = when (this) {
            is Either.Right -> value.drawerLayout
            is Either.Left -> value.drawerLayout
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.mainCoordinatorLayout
        get() = when (this) {
            is Either.Right -> value.mainCoordinatorLayout
            is Either.Left -> value.mainCoordinatorLayout
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.appbar
        get() = when (this) {
            is Either.Right -> value.appbar
            is Either.Left -> value.appbar
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.switchToolbar
        get() = when (this) {
            is Either.Right -> value.switchToolbar
            is Either.Left -> value.switchToolbar
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.mainLabel
        get() = when (this) {
            is Either.Right -> value.mainLabel
            is Either.Left -> value.mainLabel
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.fragmentContainer
        get() = when (this) {
            is Either.Right -> value.fragmentContainer
            is Either.Left -> value.fragmentContainer
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.playingLayout
        get() = when (this) {
            is Either.Right -> Either.Right(value.playingLayoutWave)
            is Either.Left -> Either.Left(value.playingLayoutBar)
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.navView
        get() = when (this) {
            is Either.Right -> value.navView
            is Either.Left -> value.navView
        }

    private inline val Either<ActivityMainBarBinding, ActivityMainWaveBinding>.activityViewModel
        get() = when (this) {
            is Either.Right -> value.viewModel
            is Either.Left -> value.viewModel
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playing
        get() = when (this) {
            is Either.Right -> value.playingWave
            is Either.Left -> value.playingBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingToolbar
        get() = when (this) {
            is Either.Right -> value.playingToolbarWave
            is Either.Left -> value.playingToolbarBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingLayout
        get() = when (this) {
            is Either.Right -> value.playingLayoutWave
            is Either.Left -> value.playingLayoutBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingAlbumImage
        get() = when (this) {
            is Either.Right -> value.playingAlbumImageWave
            is Either.Left -> value.playingAlbumImageBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingTrackTitle
        get() = when (this) {
            is Either.Right -> value.playingTrackTitleWave
            is Either.Left -> value.playingTrackTitleBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingTrackArtists
        get() = when (this) {
            is Either.Right -> value.playingTrackArtistsWave
            is Either.Left -> value.playingTrackArtistsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingNextTrack
        get() = when (this) {
            is Either.Right -> value.playingNextTrackWave
            is Either.Left -> value.playingNextTrackBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingPlayButton
        get() = when (this) {
            is Either.Right -> value.playingPlayButtonWave
            is Either.Left -> value.playingPlayButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playingPrevTrack
        get() = when (this) {
            is Either.Right -> value.playingPrevTrackWave
            is Either.Left -> value.playingPrevTrackBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackLayout
        get() = when (this) {
            is Either.Right -> value.trackLayoutWave
            is Either.Left -> value.trackLayoutBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.visualizer
        get() = when (this) {
            is Either.Right -> value.visualizerWave
            is Either.Left -> value.visualizerBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackSettingsButton
        get() = when (this) {
            is Either.Right -> value.trackSettingsButtonWave
            is Either.Left -> value.trackSettingsButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.albumPicture
        get() = when (this) {
            is Either.Right -> value.albumPictureWave
            is Either.Left -> value.albumPictureBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackPlayingBar
        get() = when (this) {
            is Either.Right -> value.trackPlayingBarWave
            is Either.Left -> value.trackPlayingBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.currentTime
        get() = when (this) {
            is Either.Right -> value.currentTimeWave
            is Either.Left -> value.currentTimeBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackLength
        get() = when (this) {
            is Either.Right -> value.trackLengthWave
            is Either.Left -> value.trackLengthBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trackTitleBig
        get() = when (this) {
            is Either.Right -> value.trackTitleBigWave
            is Either.Left -> value.trackTitleBigBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.artistsAlbum
        get() = when (this) {
            is Either.Right -> value.artistsAlbumWave
            is Either.Left -> value.artistsAlbumBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.primaryButtons
        get() = when (this) {
            is Either.Right -> value.primaryButtonsWave
            is Either.Left -> value.primaryButtonsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playButton
        get() = when (this) {
            is Either.Right -> value.playButtonWave
            is Either.Left -> value.playButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.previousTrackButton
        get() = when (this) {
            is Either.Right -> value.previousTrackButtonWave
            is Either.Left -> value.previousTrackButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.nextTrackButton
        get() = when (this) {
            is Either.Right -> value.nextTrackButtonWave
            is Either.Left -> value.nextTrackButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.secondaryButtons
        get() = when (this) {
            is Either.Right -> value.secondaryButtonsWave
            is Either.Left -> value.secondaryButtonsBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.equalizerButton
        get() = when (this) {
            is Either.Right -> value.equalizerButtonWave
            is Either.Left -> value.equalizerButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.repeatButton
        get() = when (this) {
            is Either.Right -> value.repeatButtonWave
            is Either.Left -> value.repeatButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.sleepTimer
        get() = when (this) {
            is Either.Right -> value.sleepTimerWave
            is Either.Left -> value.sleepTimerBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.recordButton
        get() = when (this) {
            is Either.Right -> value.recordButtonWave
            is Either.Left -> value.recordButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.playlistButton
        get() = when (this) {
            is Either.Right -> value.playlistButtonWave
            is Either.Left -> value.playlistButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.trimButton
        get() = when (this) {
            is Either.Right -> value.trimButtonWave
            is Either.Left -> value.trimButtonBar
        }

    private inline val Either<PlayingBarBinding, PlayingWaveBinding>.returnButton
        get() = when (this) {
            is Either.Right -> value.returnButtonWave
            is Either.Left -> value.returnButtonBar
        }

    private inline var Either<PlayingBarBinding, PlayingWaveBinding>.playingBarViewModel
        get() = when (this) {
            is Either.Right -> value.viewModel
            is Either.Left -> value.viewModel
        }
        set(value) = when (this) {
            is Either.Right -> this.value.viewModel = value
            is Either.Left -> this.value.viewModel = value
        }

    internal var mainLabelCurText
        get() = binding.mainLabel.text.toString()
        set(value) { binding.mainLabel.text = value }

    internal val switchToolbar
        get() = binding.switchToolbar

    private suspend fun getCurPath() = StorageUtil.getInstanceAsyncSynchronized().loadTrackPathLocking()

    private inline val curTrack
        get() = getFromWorkerThreadAsync {
            mainApplication.run {
                curPlaylist
                    .indexOfFirst { track -> track.path == getCurPath() }
                    .takeIf { it != -1 }
                    ?.let { curPlaylist[it] }
            }
        }

    private inline val curInd
        get() = getFromWorkerThreadAsync {
            mainApplication
                .curPlaylist
                .indexOfFirst { it.path == getCurPath() }
        }

    internal inline val isPlaying
        get() = try {
            mainApplication.musicPlayer?.isPlaying
        } catch (e: Exception) {
            false
        }

    private inline val curTimeData
        get() = getFromWorkerThreadAsync {
            try {
                when (isPlaying) {
                    true -> mainApplication.musicPlayer?.currentPosition
                        ?: StorageUtil.getInstanceAsyncSynchronized().loadTrackPauseTimeLocking()

                    else -> StorageUtil.getInstanceAsyncSynchronized().loadTrackPauseTimeLocking()
                }
            } catch (e: Exception) {
                StorageUtil.getInstanceAsyncSynchronized().loadTrackPauseTimeLocking()
            }
        }

    private inline val isMicRecording
        get() = mainApplication.isMicRecording

    private inline val isPlaybackRecording
        get() = mainApplication.isPlaybackRecording

    private val highlightTrackReceiver by lazy {
        object : BroadcastReceiver() {
            @SuppressLint("SyntheticAccessor")
            override fun onReceive(context: Context?, intent: Intent?) {
                highlightTrackFragmentsAsync()
            }
        }
    }

    private val customizeReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runOnUIThread {
                    val isImageUpd = intent!!.getBooleanExtra(
                        AudioPlayerService.UPD_IMAGE_ARG,
                        true
                    )

                    val newTrack = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            intent.getSerializableExtra(
                                AudioPlayerService.NEW_TRACK_ARG,
                                AbstractTrack::class.java
                            )

                        else -> intent.getSerializableExtra(AudioPlayerService.NEW_TRACK_ARG)
                                as? AbstractTrack?
                    }

                    customizeAsync(
                        newTrack = if (isImageUpd) newTrack else null,
                        isImageUpdateNeed = isImageUpd,
                        isLocking = true
                    )
                }
            }
        }
    }

    private val releaseAudioVisualizerReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = releaseAudioVisualizer()
        }
    }

    private val initAudioVisualizerReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = initAudioVisualizer()
        }
    }

    private val prepareForPlayingReceiver by lazy {
        object : BroadcastReceiver() {
            @SuppressLint("SyntheticAccessor")
            override fun onReceive(context: Context?, intent: Intent?) {
                runOnUIThread {
                    reinitializePlayingCoroutine(isLocking = true)

                    val isImageUpd = intent!!.getBooleanExtra(
                        AudioPlayerService.UPD_IMAGE_ARG,
                        false
                    )

                    val newTrack = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            intent.getSerializableExtra(
                                AudioPlayerService.NEW_TRACK_ARG,
                                AbstractTrack::class.java
                            )

                        else -> intent.getSerializableExtra(AudioPlayerService.NEW_TRACK_ARG)
                                as? AbstractTrack?
                    }

                    customizeAsync(
                        newTrack = if (isImageUpd) newTrack else null,
                        isImageUpdateNeed = isImageUpd,
                        isLocking = true
                    )
                }

                highlightTrackFragmentsAsync()
            }
        }
    }

    private val updateLoopingReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateLooping(isOnlyUIUpdate = true)
            }
        }
    }

    @Deprecated("Like button is not used anymore. Replaced by audio recording")
    private val setLikeButtonImageReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runOnUIThread {
                    setRecordButtonImage(
                        intent!!.getBooleanExtra(AudioPlayerService.LIKE_IMAGE_ARG, false),
                        isLocking = true
                    )
                }
            }
        }
    }

    private val setRecordButtonImageReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                runOnUIThread {
                    setRecordButtonImage(
                        intent.getBooleanExtra(MicRecordService.RECORD_BUTTON_IMAGE_ARG, false),
                        isLocking = true
                    )
                }
            }
        }
    }

    private val updateFavouriteTracksFragmentReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (currentFragment.unchecked is FavouriteTrackListFragment)
                    (currentFragment.unchecked as FavouriteTrackListFragment).run {
                        runOnUIThread { updateUIOnChangeContentAsync() }
                    }
            }
        }
    }

    internal val pickImageIntentResultListener: ActivityResultLauncher<Intent> by lazy {
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result != null) runOnUIThread {
                while (currentFragment.get() == null)
                    currentFragmentInitCondVar.blockAsync()

                (currentFragment.get() as? ChangeImageFragment)
                    ?.setUserImageAsync(result.data!!.data!!)
                    ?.join()

                setPlayingBackgroundImage()
            }
        }
    }

    internal val pickFolderIntentResultListener: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result
                ?.data
                ?.getStringExtra(FoldersActivity.FOLDER_KEY)
                ?.let {
                    runOnIOThread {
                        Params.getInstanceSynchronized().pathToSave = it
                        StorageUtil.getInstanceAsyncSynchronized().storePathToSave(it)
                        (currentFragment.unchecked as? SettingsFragment?)?.refreshSaveLocationButton()
                    }
                }
        }

    private val mediaProjectionIntentResultListener: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PlaybackRecordService.Caller(WeakReference(mainApplication))
                        .setFileName(recordFilename)
                        .setExtraData(it.data)
                        .call()
                }
            }
        }

    private val onBackPressedCallback by lazy {
        object : OnBackPressedCallback(true) {
            @SuppressLint("SyntheticAccessor")
            override fun handleOnBackPressed() {
                when (sheetBehavior.state) {
                    BottomSheetBehavior.STATE_EXPANDED ->
                        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                    else -> when {
                        binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                            binding.drawerLayout.closeDrawer(GravityCompat.START)

                        else -> try {
                            when (supportFragmentManager.backStackEntryCount) {
                                0 -> when {
                                    --backClicksCount == 0 -> finishWork()

                                    else -> runOnUIThread {
                                        Toast.makeText(
                                            this@MainActivity,
                                            R.string.press_to_exit,
                                            Toast.LENGTH_LONG
                                        ).show()
                                        setBackingCountToDefault()
                                    }
                                }

                                else -> finishWork()
                            }
                        } catch (ignored: Exception) {
                            // Equalizer error
                        }
                    }
                }
            }
        }
    }

    private val playingBackgroundViewTarget by lazy {
        object : CustomViewTarget<ConstraintLayout, Drawable>(
            binding.playingLayout.playing
        ) {
            override fun onLoadFailed(errorDrawable: Drawable?) =
                onResourceCleared(placeholder = null)

            @SuppressLint("SyntheticAccessor")
            override fun onResourceCleared(placeholder: Drawable?) {
                binding.playingLayout.playing.background = null
                binding.playingLayout.playing.setBackgroundColor(Params.instance.secondaryColor)
            }

            @SuppressLint("SyntheticAccessor")
            override fun onResourceReady(
                resource: Drawable,
                transition: Transition<in Drawable>?
            ) { binding.playingLayout.playing.background = resource }
        }
    }

    internal companion object {
        // AudioService Broadcasts
        internal const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
        internal const val Broadcast_PLAY_NEW_TRACK = "com.dinaraparanid.prima.PlayNewAudio"
        internal const val Broadcast_RESUME = "com.dinaraparanid.prima.Resume"
        internal const val Broadcast_PAUSE = "com.dinaraparanid.prima.Pause"
        internal const val Broadcast_LOOPING = "com.dinaraparanid.prima.StartLooping"
        internal const val Broadcast_STOP = "com.dinaraparanid.prima.Stop"
        internal const val Broadcast_UPDATE_NOTIFICATION = "com.dinaraparanid.prima.UpdateNotification"
        internal const val Broadcast_REMOVE_NOTIFICATION = "com.dinaraparanid.prima.RemoveNotification"
        internal const val Broadcast_RESTART_PLAYING_AFTER_TRACK_CHANGED = "com.dinaraparanid.prima.RestartPlayingAfterTrackChanged"

        // AudioService arguments
        internal const val RESUME_POSITION_ARG = "resume_position"
        internal const val PAUSED_PRESSED_ARG = "pause_pressed"
        internal const val IS_LOOPING_ARG = "is_looping"
        internal const val LOOPING_PRESSED_ARG = "looping_pressed"
        internal const val UPDATE_UI_ON_PAUSE_ARG = "update_ui"

        // MicRecordService Broadcast
        internal const val Broadcast_MIC_START_RECORDING = "com.dinaraparanid.prima.MicStartRecording"
        internal const val Broadcast_MIC_STOP_RECORDING = "com.dinaraparanid.prima.MicStopRecording"

        // PlaybackRecordService Broadcast
        internal const val Broadcast_PLAYBACK_START_RECORDING = "com.dinaraparanid.prima.PlaybackStartRecording"
        internal const val Broadcast_PLAYBACK_STOP_RECORDING = "com.dinaraparanid.prima.PlaybackStopRecording"

        // RecordService arguments
        internal const val FILE_NAME_ARG = "filename"

        private const val SHEET_BEHAVIOR_STATE_KEY = "sheet_behavior_state"
        private const val PROGRESS_KEY = "progress"
        private const val TRACK_SELECTED_KEY = "track_selected"

        @Deprecated("Switched to registerForActivityResult")
        private const val MEDIA_PROJECTION_REQUEST_CODE = 13
        private const val MAIN_PERMISSIONS_REQUEST_CODE = 14
        private const val WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 17
        private const val AUDIO_TASK_AWAIT_LIMIT = 500L

        /**
         * Calculates time in hh:mm:ss format
         * @param millis millisecond to convert
         * @return int[hh, mm, ss]
         */

        private fun calcTrackTime(millis: Int) =
            NativeLibrary.calcTrackTime(millis).let { (f, s, t) -> Triple(f, s, t) }

        /**
         * Converts [Triple] to hh:mm:ss formatted string
         * @return "hh:mm:ss"
         */

        private fun Triple<Int, Int, Int>.asTimeString() =
            "${first.let { if (it < 10) "0$it" else it }}:" +
                    "${second.let { if (it < 10) "0$it" else it }}:" +
                    "${third.let { if (it < 10) "0$it" else it }}"

        /**
         * Checks updates for Prima with GitHub API.
         * If new version is available, shows [PrimaReleaseDialogFragment]
         */

        private fun MainActivity.fetchPrimaUpdates() =
            GitHubFetcher().fetchLatestRelease().observe(this) { release ->
                try {
                    if (release.name > BuildConfig.VERSION_NAME)
                        PrimaReleaseDialogFragment(release, this, PrimaReleaseDialogFragment.Target.NEW).show()
                } catch (e: Exception) {
                    // API key limit exceeded
                }
            }

        @SuppressLint("SyntheticAccessor")
        private fun MainActivity.addOnBackPressedCallback() =
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        @SuppressLint("SyntheticAccessor")
        private fun MainActivity.clearGlideImages() {
            Glide.with(this).run {
                clear(binding.playingLayout.albumPicture)
                clear(binding.playingLayout.playingAlbumImage)
                clear(playingBackgroundViewTarget)
            }

            Glide.get(this).run {
                runOnIOThread { clearDiskCache() }
                bitmapPool.clearMemory()
                clearMemory()
            }
        }

        private inline fun <reified T : MainActivityFragment> MainActivity.isNotCurrentFragment() =
            currentFragment.unchecked !is T

        private fun getMainFragment(pos: Int) = ViewPagerFragment.newInstance(
            pos,
            UltimateCollectionFragment::class
        )

        private fun MainActivity.ExitDialog() =
            AlertDialog.Builder(this)
                .setMessage(R.string.exit_request)
                .setPositiveButton(R.string.ok) { d, _ ->
                    d.dismiss()
                    finishWork()

                    runOnWorkerThread {
                        delay(1500) // Time to save everything
                        exitProcess(0)
                    }
                }
                .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }

        private fun MainActivity.getFragmentIfNotCurrent(item: MenuItem) = when (item.itemId) {
            R.id.nav_tracks -> when {
                isNotCurrentFragment<DefaultTrackListFragment>() -> getMainFragment(pos = 0)
                else -> null
            }

            R.id.nav_playlists -> when {
                isNotCurrentFragment<AlbumListFragment>() && isNotCurrentFragment<DefaultPlaylistListFragment>() ->
                    AbstractFragment.defaultInstance(
                        null,
                        TrackCollectionsFragment::class
                    )
                else -> null
            }

            R.id.nav_artists -> when {
                isNotCurrentFragment<DefaultArtistListFragment>() -> getMainFragment(pos = 1)
                else -> null
            }

            R.id.nav_favourite -> when {
                isNotCurrentFragment<FavouriteTrackListFragment>() && isNotCurrentFragment<FavouriteArtistListFragment>() ->
                    AbstractFragment.defaultInstance(
                        null,
                        FavouritesFragment::class
                    )
                else -> null
            }

            R.id.nav_mp3_converter -> when {
                isNotCurrentFragment<MP3ConverterFragment>() -> getMainFragment(pos = 2)
                else -> null
            }

            R.id.nav_guess_the_melody -> when {
                isNotCurrentFragment<GTMMainFragment>() -> getMainFragment(pos = 3)
                else -> null
            }

            R.id.nav_statistics -> when {
                isNotCurrentFragment<StatisticsFragment>() -> AbstractFragment.defaultInstance(
                    null,
                    StatisticsHolderFragment::class
                )

                else -> null
            }

            R.id.nav_settings -> when {
                isNotCurrentFragment<SettingsFragment>() -> getMainFragment(pos = 4)
                else -> null
            }

            else -> when {
                isNotCurrentFragment<AboutAppFragment>() -> getMainFragment(pos = 5)
                else -> null
            }
        }

        private fun MainActivity.setAudioCommand(command: Runnable) {
            audioCommand.set(command)
            audioCondition.open()

            if (!sendIsRunning.get()) {
                sendIsRunning.set(true)
                runOnWorkerThread { sendAudioCommand() }
            }
        }

        private suspend fun MainActivity.sendAudioCommand() {
            while (true)
                if (audioCondition.blockAsync(AUDIO_TASK_AWAIT_LIMIT)) {
                    audioCommand.get().run()
                    sendIsRunning.set(false)
                    return
                }
        }

        @SuppressLint("SyntheticAccessor")
        private suspend fun MainActivity.initTrackProgressBarAsync(trackDuration: Int) =
            runOnUIThread {
                binding.playingLayout.run {
                    val curTime = curTimeData.await()
                    currentTime.text = calcTrackTime(curTime).asTimeString()

                    trackPlayingBar.run {
                        max = trackDuration
                        progress = curTime
                    }
                }
            }

        @SuppressLint("SyntheticAccessor")
        private fun MainActivity.showPlayingBar() {
            binding.playingLayout.playing.visibility = View.VISIBLE
        }

        private fun MainActivity.launchFragment(fragment: Fragment) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        private inline fun MainActivity.launchFragment(fragment: Fragment, onInit: () -> Unit) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .apply { onInit() }
                .commit()
        }

        private suspend fun MainActivity.storePlaylistAsync(vararg tracks: AbstractTrack) =
            StorageUtil.getInstanceAsyncSynchronized().storeCurPlaylistLocking(
                mainApplication.curPlaylist.apply {
                    clear()
                    addAll(tracks)
                }
            )

        private fun MainActivity.handleAudioCommand(
            isNeededToPlay: Boolean,
            shouldPlay: Boolean,
            isNewTrack: Boolean,
            trackPath: String
        ) = runOnWorkerThread {
            when {
                isNeededToPlay -> launch(Dispatchers.Main) {
                    setAudioCommand {
                        runOnWorkerThread {
                            when {
                                shouldPlay -> when {
                                    isNewTrack -> playAudio(trackPath, isLocking = true)
                                    else -> resumePlaying(isLocking = true)
                                }

                                else -> pausePlaying(isLocking = true, isUiUpdating = true)
                            }
                        }
                    }
                }

                else -> if (isPlaying == true) reinitializePlayingCoroutine(isLocking = true)
            }
        }

        private fun MainActivity.getTrackCollectionTrackListFragment(
            playlistType: AbstractPlaylist.PlaylistType,
            playlistTitle: String,
            playlistId: Long
        ): TrackCollectionTrackListFragment<*> = when (playlistType) {
            AbstractPlaylist.PlaylistType.ALBUM -> AbstractFragment.defaultInstance(
                playlistTitle,
                when {
                    isNotCurrentFragment<HiddenPlaylistListFragment>() -> AlbumTrackListFragment::class
                    else -> HiddenAlbumTrackListFragment::class
                }
            )

            AbstractPlaylist.PlaylistType.CUSTOM -> AbstractCustomPlaylistTrackListFragment.newInstance(
                playlistTitle,
                playlistId,
                when {
                    isNotCurrentFragment<HiddenPlaylistListFragment>() -> CustomPlaylistTrackListFragment::class
                    else -> HiddenCustomPlaylistTrackListFragment::class
                }
            )

            AbstractPlaylist.PlaylistType.GTM ->
                throw IllegalArgumentException("GTM Playlist in AbstractPlaylistFragment")
        }

        @SuppressLint("SyntheticAccessor")
        private suspend fun MainActivity.showUIForPlayingTrackAndPlayIfNeeded(
            track: AbstractTrack,
            isNeededToPlay: Boolean,
            vararg tracks: AbstractTrack
        ) {
            if (isNeededToPlay)
                storePlaylistAsync(*tracks)

            mainApplication.playingBarIsVisible = true
            viewModel.trackSelectedFlow.value = true

            runOnUIThread {
                // wait fragment to call onCreateView
                delay(500)
                (currentFragment.get() as? Rising?)?.up()
            }

            val isNewTrack = getCurPath() != track.path
            val oldTrack = curTrack.await()

            if (isNewTrack)
                releaseAudioVisualizer()

            val shouldPlay = when {
                mainApplication.isAudioServiceBounded -> when {
                    isNewTrack -> true
                    isNeededToPlay -> !isPlaying!!
                    else -> isPlaying!!
                }

                else -> isNeededToPlay
            }

            binding.playingLayout.playing.isVisible = true

            updateUIAsync(src = oldTrack to track, isLocking = isNeededToPlay)
            setPlayButtonSmallImage(isPlaying = shouldPlay, isLocking = isNeededToPlay)
            setPlayButtonImage(isPlaying = shouldPlay, isLocking = isNeededToPlay)
            setSmallAlbumImageAnimation(isPlaying = shouldPlay, isLocking = isNeededToPlay)
            StorageUtil.getInstanceAsyncSynchronized().storeTrackPathLocking(track.path)

            binding.playingLayout.playingTrackTitle.isSelected = true
            binding.playingLayout.playingTrackArtists.isSelected = true
            initTrackProgressBarAsync(trackDuration = track.duration.toInt())

            handleAudioCommand(
                isNeededToPlay,
                shouldPlay,
                isNewTrack,
                trackPath = track.path
            )

            if (isNewTrack)
                initAudioVisualizer()
        }

        private fun MainActivity.launchLyricsFragmentOrOpenUrl(
            trackUrl: String,
            trackGeniusTitle: String
        ) {
            NativeLibrary.getLyricsByUrl(trackUrl)?.let { lyrics ->
                launchFragment(
                    LyricsFragment.newInstance(
                        trackGeniusTitle,
                        lyrics
                    )
                )
            } ?: runOnUIThread {
                QuestionDialog(R.string.genius_access_failed) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trackUrl)))
                }.show(supportFragmentManager, null)
            }
        }

        private suspend fun MainActivity.launchTrackInfoFragment(trackId: Long) {
            GeniusFetcher().fetchTrackInfoSearch(trackId).run {
                runOnUIThread {
                    observe(this@launchTrackInfoFragment) {
                        runOnUIThread { awaitDialog?.await()?.dismiss() }
                        launchFragment(TrackInfoFragment.newInstance(it.response.song))
                    }
                }
            }
        }

        internal fun MainActivity.setSheetBehaviourFromExpandedToCollapsed() {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        internal fun MainActivity.restart() {
            finishAndRemoveTask()
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }
    }

    @SuppressLint("SyntheticAccessor")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()
        super.onCreate(savedInstanceState)
        initView(savedInstanceState)
        runOnIOThread { updateStatisticsAsync() }

        addOnBackPressedCallback()
        registerHighlightTrackReceiver()
        registerCustomizeReceiver()
        registerReleaseAudioVisualizerReceiver()
        registerInitAudioVisualizerReceiver()
        registerPrepareForPlayingReceiver()
        registerUpdateLoopingReceiver()
        registerMicRecordButtonSetImageReceiver()
        registerUpdateFavouriteTracksFragmentReceiver()

        fetchPrimaUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SHEET_BEHAVIOR_STATE_KEY, sheetBehavior.state)
        outState.putBoolean(PROGRESS_KEY, viewModel.hasStartedPlaying.value)
        outState.putBoolean(TRACK_SELECTED_KEY, viewModel.trackSelectedFlow.value)
        super.onSaveInstanceState(outState)
    }

    /** Frees UI */
    @SuppressLint("SyntheticAccessor")
    override fun onStop() {
        super.onStop()
        destroyAwaitDialog()
        clearGlideImages()
        finishWork()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseAudioVisualizer()
        finishWork()
        _binding = null
        Glide.get(this).clearMemory()

        unregisterReceiver(highlightTrackReceiver)
        unregisterReceiver(customizeReceiver)
        unregisterReceiver(releaseAudioVisualizerReceiver)
        unregisterReceiver(initAudioVisualizerReceiver)
        unregisterReceiver(prepareForPlayingReceiver)
        unregisterReceiver(updateLoopingReceiver)
        unregisterReceiver(setRecordButtonImageReceiver)
        unregisterReceiver(updateFavouriteTracksFragmentReceiver)
    }

    @SuppressLint("SyntheticAccessor")
    override fun onResume() {
        super.onResume()

        mainApplication.let {
            it.mainActivity = WeakReference(this)
            it.setLang()
        }

        runOnUIThread {
            initTrackProgressBarAsync(trackDuration = curTrack.await()?.duration?.toInt() ?: 0)
        }

        try {
            runOnUIThread {
                if (Params.getInstanceSynchronized().isCoverRotating)
                    startRotation()

                customizeAsync(
                    curTrack.await(),
                    isImageUpdateNeed = true,
                    isDefaultPlaying = false,
                    isLocking = true
                )
            }
        } catch (ignored: Exception) {
            // permissions not given
        }

        if (isPlaying == true)
            reinitializePlayingCoroutineNoLock()

        initAudioVisualizer()
    }

    @SuppressLint("SyntheticAccessor")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_exit) {
            ExitDialog().show()
            return true
        }

        getFragmentIfNotCurrent(item)?.let {
            launchFragment(it) {
                if (isPlaying == true) showPlayingBar()
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @SuppressLint("SyntheticAccessor")
    override fun onTrackSelected(
        track: AbstractTrack,
        tracks: Collection<AbstractTrack>,
        needToPlay: Boolean
    ) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) runOnUIThread {
            showUIForPlayingTrackAndPlayIfNeeded(
                track,
                isNeededToPlay = needToPlay,
                tracks = tracks.toTypedArray()
            )
        }
    }

    @SuppressLint("SyntheticAccessor")
    override fun onTrackSelected(track: AbstractTrack, tracks: Collection<AbstractTrack>) {
        runOnUIThread {
            showUIForPlayingTrackAndPlayIfNeeded(
                track,
                isNeededToPlay = true,
                tracks = tracks.toTypedArray()
            )
        }
    }

    @SuppressLint("SyntheticAccessor")
    override fun onArtistSelected(artist: Artist) = launchFragment(
        AbstractFragment.defaultInstance(
            artist.name,
            when {
                isNotCurrentFragment<HiddenArtistListFragment>() -> ArtistTrackListFragment::class
                else -> HiddenArtistTrackListFragment::class
            }
        )
    ) { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }

    @SuppressLint("SyntheticAccessor")
    override fun onPlaylistSelected(
        title: String,
        type: AbstractPlaylist.PlaylistType,
        id: Long
    ) = launchFragment(
        getTrackCollectionTrackListFragment(
            playlistType = type,
            playlistTitle = title,
            playlistId = id
        )
    ) { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }

    override fun onFontSelected(font: String) {
        supportFragmentManager.popBackStack()

        runOnIOThread {
            Params.getInstanceSynchronized().let {
                launch(Dispatchers.Main) { it.font = font }
            }

            StorageUtil.getInstanceAsyncSynchronized().storeFont(font)
        }

        runOnUIThread { setPlayingBackgroundImage() }
    }

    @SuppressLint("SyntheticAccessor")
    override suspend fun onTrackSelected(
        track: GeniusTrack,
        target: TrackListFoundFragment.Target
    ) = coroutineScope {
        awaitDialog = async(Dispatchers.Main) {
            createAndShowAwaitDialog(this@MainActivity, false)
        }

        launch(Dispatchers.IO) {
            when (target) {
                TrackListFoundFragment.Target.LYRICS -> {
                    launchLyricsFragmentOrOpenUrl(track.url, track.geniusTitle)
                    launch(Dispatchers.Main) { awaitDialog?.await()?.dismiss() }
                }

                TrackListFoundFragment.Target.INFO -> launchTrackInfoFragment(track.id)
            }
        }

        setSheetBehaviourFromExpandedToCollapsed()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onImageSelected(image: Bitmap, albumImageView: ImageView) {
        val width = albumImageView.width
        val height = albumImageView.height

        Glide.with(this)
            .load(image)
            .placeholder(
                try {
                    albumImageView
                        .drawable
                        .toBitmap(width, height)
                        .toDrawable(resources)
                } catch (e: Exception) {
                    resources.getDrawable(R.drawable.album_default, theme)
                }
            )
            .error(R.drawable.album_default)
            .fallback(R.drawable.album_default)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(width, height)
            .into(albumImageView)
    }

    // TODO: Stopped here
    override suspend fun onTrackSelected(
        selectedTrack: Song,
        titleInput: EditText,
        artistInput: EditText,
        albumInput: EditText,
        numberInAlbumInput: EditText
    ) = coroutineScope {
        titleInput.setText(selectedTrack.title, TextView.BufferType.EDITABLE)
        artistInput.setText(selectedTrack.primaryArtist.name, TextView.BufferType.EDITABLE)
        albumInput.setText(selectedTrack.album?.name ?: "", TextView.BufferType.EDITABLE)
        numberInAlbumInput.setText("${
            selectedTrack.album?.url?.let { url ->
                withContext(Dispatchers.IO) {
                    NativeLibrary
                        .getTrackNumberInAlbum(url, selectedTrack.title.trim())
                        .let { if (it > -1) it + 1 else -1 }
                        .also {
                            if (it == -1) runOnUIThread {
                                QuestionDialog(R.string.genius_access_failed) {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(selectedTrack.url)
                                        )
                                    )
                                }.show(supportFragmentManager, null)
                            }
                        }
                }
            } ?: -1
        }")
    }

    override fun onPlaylistSelected(
        playlist: AbstractPlaylist,
        fragment: GTMPlaylistSelectFragment
    ) = when (playlist.type) {
        AbstractPlaylist.PlaylistType.GTM -> GTMSetStartPlaybackDialogFragment()
        else -> GTMSetStartPropertiesDialogFragment(playlist, WeakReference(fragment))
    }.show(supportFragmentManager, null)

    override fun showChooseContactFragment(uri: Uri) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                ChooseContactFragment.newInstance(uri)
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onContactSelected(contact: Contact, ringtoneUri: Uri) {
        contentResolver.update(
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString()),
            ContentValues().apply {
                put(
                    ContactsContract.Contacts.CUSTOM_RINGTONE,
                    ringtoneUri.toString()
                )
            },
            null, null
        )

        Toast.makeText(
            this,
            "${resources.getString(R.string.success_contact_ringtone)} ${contact.displayName}",
            Toast.LENGTH_SHORT
        ).show()

        supportFragmentManager.popBackStack()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) = when (requestCode) {
        MAIN_PERMISSIONS_REQUEST_CODE -> requestMainPermissions()
        else -> requestWriteExternalStoragePermission()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) = when (requestCode) {
        MAIN_PERMISSIONS_REQUEST_CODE -> when {
            !isWriteExternalStoragePermissionGranted ->
                requestWriteExternalStoragePermission()
            else -> Unit
        }

        else -> {
            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
            val order = MediaStore.Audio.Media.TITLE + " ASC"

            val projection = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.TRACK
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                projection.add(MediaStore.Audio.Media.RELATIVE_PATH)

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                null,
                order
            ).use { cursor ->
                mainApplication.run {
                    if (cursor != null) runOnIOThread {
                        addTracksFromStorage(cursor, allTracks)
                    }
                }
            }
        }
    }

    @Deprecated("Switched to EasyPermissions API")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        } catch (ignored: Exception) {
            // first time opening app
        }

        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String, Int> = HashMap()

                perms[Manifest.permission.READ_PHONE_STATE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.RECORD_AUDIO] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.READ_CONTACTS] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.WRITE_CONTACTS] = PackageManager.PERMISSION_GRANTED

                if (grantResults.isNotEmpty()) {
                    var i = 0
                    while (i < permissions.size) {
                        perms[permissions[i]] = grantResults[i]
                        i++
                    }

                    when {
                        perms[Manifest.permission.READ_PHONE_STATE] ==
                                PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.READ_EXTERNAL_STORAGE] ==
                                PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.RECORD_AUDIO] ==
                                PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.READ_CONTACTS] ==
                                PackageManager.PERMISSION_GRANTED &&
                                perms[Manifest.permission.WRITE_CONTACTS] ==
                                PackageManager.PERMISSION_GRANTED -> Unit // all permissions are granted

                        else -> when {
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_EXTERNAL_STORAGE
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_PHONE_STATE
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.RECORD_AUDIO
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.READ_CONTACTS
                            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                this, Manifest.permission.WRITE_CONTACTS
                            ) -> AlertDialog
                                .Builder(this)
                                .setMessage("Phone state and storage permissions required for this app")
                                .setPositiveButton("OK") { _, which ->
                                    if (which == DialogInterface.BUTTON_POSITIVE)
                                        mainApplication.checkAndRequestPermissions()
                                }
                                .setNegativeButton("Cancel") { _, _ -> }
                                .create()
                                .show()

                            else -> Toast.makeText(
                                this,
                                "Go to settings and enable permissions, please",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            MAIN_PERMISSIONS_REQUEST_CODE -> Toast.makeText(
                this,
                when {
                    grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                        "Permissions to capture audio granted. Click the button once again."
                    else -> "Permissions to capture audio denied."
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * @param src first - old (current) track or null
     * @param src second - new track
     */

    @SuppressLint("UseCompatLoadingForDrawables")
    override suspend fun updateUIAsyncNoLock(src: Pair<AbstractTrack?, AbstractTrack>) {
        setRepeatButtonImage(isLocking = false)
        setRecordButtonImage(isMicRecording, isLocking = false)

        val oldTrack = src.first
        val newTrack = src.second

        val artistAlbum =
            "${
                newTrack.artist.let {
                    when (it) {
                        "<unknown>" -> resources.getString(R.string.unknown_artist)
                        else -> it
                    }
                }
            } / ${
                newTrack.album.let {
                    when (it) {
                        "<unknown>" -> resources.getString(R.string.unknown_album)
                        else -> it
                    }
                }
            }"

        binding.playingLayout.playingTrackTitle.text = newTrack.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        binding.playingLayout.playingTrackArtists.text = newTrack.artist.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_artist)
                else -> it
            }
        }

        binding.playingLayout.trackTitleBig.text = newTrack.title.let {
            when (it) {
                "<unknown>" -> resources.getString(R.string.unknown_track)
                else -> it
            }
        }

        val time = calcTrackTime(newTrack.duration.toInt()).asTimeString()

        binding.playingLayout.artistsAlbum.text = artistAlbum
        binding.playingLayout.playingTrackTitle.isSelected = true
        binding.playingLayout.playingTrackArtists.isSelected = true
        binding.playingLayout.trackTitleBig.isSelected = true
        binding.playingLayout.artistsAlbum.isSelected = true
        binding.playingLayout.trackLength.text = time

        if (albumImageWidth == 0) {
            albumImageWidth = binding.playingLayout.albumPicture.width
            albumImageHeight = binding.playingLayout.albumPicture.height
        }

        val app = mainApplication
        val newCover = app.getAlbumPictureAsync(newTrack.path).await()

        suspend fun getOldCover(trackPath: String, imageWidth: Int, imageHeight: Int) =
            app
                .getAlbumPictureAsync(trackPath)
                .await()
                .toDrawable(resources)
                .toBitmap(imageWidth, imageHeight)

        fun getNewCover(imageWidth: Int, imageHeight: Int) =
            newCover.scale(imageWidth, imageHeight)

        val oldCoverForAlbumPicture = oldTrack
            ?.let { getOldCover(it.path, albumImageWidth, albumImageHeight) }
            ?: getNewCover(albumImageWidth, albumImageHeight)

        val playingBackground = binding.playingLayout.playing

        val oldCoverForBackground = oldTrack
            ?.let {
                getOldCover(
                    it.path,
                    playingBackground.width,
                    playingBackground.height
                )
            }
            ?: getNewCover(playingBackground.width, playingBackground.height)

        Glide.with(this)
            .load(newCover.toDrawable(resources))
            .placeholder(oldCoverForAlbumPicture.toDrawable(resources))
            .fallback(R.drawable.album_default)
            .error(R.drawable.album_default)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(albumImageWidth, albumImageHeight)
            .into(binding.playingLayout.albumPicture)

        Glide.with(this)
            .load(newCover.toDrawable(resources))
            .placeholder(oldCoverForBackground.toDrawable(resources))
            .fallback(R.drawable.album_default)
            .error(R.drawable.album_default)
            .transition(DrawableTransitionOptions.withCrossFade())
            .run {
                when {
                    Params.getInstanceSynchronized().isBlurEnabled -> {
                        override(playingBackground.width, playingBackground.height)
                            .transform(BlurTransformation(15, 5))
                            .into(object : CustomViewTarget<ConstraintLayout, Drawable>(playingBackground) {
                                override fun onLoadFailed(errorDrawable: Drawable?) = Unit

                                @SuppressLint("SyntheticAccessor")
                                override fun onResourceCleared(placeholder: Drawable?) {
                                    binding.playingLayout.playing.background = null
                                    binding.playingLayout.playing.setBackgroundColor(Params.instance.secondaryColor)
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    transition: Transition<in Drawable>?
                                ) { playingBackground.background = resource }
                            })
                    }

                    else -> {
                        playingBackground.background = null
                        playingBackground.setBackgroundColor(Params.getInstanceSynchronized().secondaryColor)
                    }
                }
            }

        binding.playingLayout.playingAlbumImage.run {
            setImageDrawable(getNewCover(width, height).toDrawable(resources))
        }

        newCover.recycle()
    }

    @Deprecated("Switched to registerForActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK)
            when (requestCode) {
                ChangeImageFragment.PICK_IMAGE -> runOnUIThread {
                    while (currentFragment.get() == null)
                        currentFragmentInitCondVar.blockAsync()

                    (currentFragment.get() as? ChangeImageFragment)
                        ?.setUserImageAsync(data!!.data!!)
                        ?.join()

                    setPlayingBackgroundImage()
                }

                FoldersActivity.PICK_FOLDER -> data
                    ?.getStringExtra(FoldersActivity.FOLDER_KEY)
                    ?.let {
                        runOnIOThread {
                            Params.getInstanceSynchronized().pathToSave = it
                            StorageUtil.getInstanceAsyncSynchronized().storePathToSave(it)
                            (currentFragment.unchecked as? SettingsFragment?)?.refreshSaveLocationButton()
                        }
                    }

                MEDIA_PROJECTION_REQUEST_CODE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        PlaybackRecordService.Caller(WeakReference(mainApplication))
                            .setFileName(recordFilename)
                            .setExtraData(data)
                            .call()
                    }
                }
            }
    }

    internal suspend fun updateUIAsync(oldTrack: AbstractTrack?, isLocking: Boolean) =
        updateUIAsync(oldTrack to curTrack.await()!!, isLocking)

    private fun setPlayButtonSmallImageNoLock(isPlaying: Boolean) {
        binding.playingLayout.playingPlayButton.run {
            setImageResource(ViewSetter.getPlayButtonSmallImage(isPlaying))
            runOnUIThread { setTint(Params.getInstanceSynchronized().fontColor) }
        }
    }

    /**
     * Sets play or pause image for small button
     * @param isPlaying is music playing now
     */

    internal suspend fun setPlayButtonSmallImage(isPlaying: Boolean, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { setPlayButtonSmallImageNoLock(isPlaying) }
        else -> setPlayButtonSmallImageNoLock(isPlaying)
    }

    private fun setPlayButtonImageNoLock(isPlaying: Boolean) {
        binding.playingLayout.playButton.run {
            setImageResource(ViewSetter.getPlayButtonImage(isPlaying))
            runOnUIThread { setTint(Params.getInstanceSynchronized().primaryColor) }
        }
    }

    /**
     * Sets play or pause image for big button
     * @param isPlaying is music playing now
     */

    internal suspend fun setPlayButtonImage(isPlaying: Boolean, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { setPlayButtonImageNoLock(isPlaying) }
        else -> setPlayButtonImageNoLock(isPlaying)
    }

    private fun setRepeatButtonImageNoLock() = binding.playingLayout.repeatButton.run {
        setImageResource(ViewSetter.getRepeatButtonImage())
        runOnUIThread { setTint(Params.getInstanceSynchronized().primaryColor) }
    }

    /**
     * Sets looping button image
     * depending on current theme and repeat status
     */

    private suspend fun setRepeatButtonImage(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { setRepeatButtonImageNoLock() }
        else -> setRepeatButtonImageNoLock()
    }

    private fun setRecordButtonImageNoLock(isRecording: Boolean) =
        binding.playingLayout.recordButton.run {
            setImageResource(ViewSetter.getRecordButtonImage(isRecording))
            runOnUIThread { setTint(Params.getInstanceSynchronized().primaryColor) }
        }

    /**
     * Sets record button image
     * depending on current theme and recording status
     * @param isRecording are we recording
     */

    internal suspend fun setRecordButtonImage(isRecording: Boolean, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { setRecordButtonImageNoLock(isRecording) }
        else -> setRecordButtonImageNoLock(isRecording)
    }

    private fun setSmallAlbumImageAnimationNoLock(isPlaying: Boolean) {
        if (!Params.instance.isCoverRotating)
            return

        when {
            isPlaying -> smallAlbumImageAnimator.resume()
            else -> smallAlbumImageAnimator.pause()
        }
    }

    private suspend fun setSmallAlbumImageAnimation(
        isPlaying: Boolean,
        isLocking: Boolean
    ) = when {
        isLocking -> mutex.withLock { setSmallAlbumImageAnimationNoLock(isPlaying) }
        else -> setSmallAlbumImageAnimationNoLock(isPlaying)
    }

    private suspend fun playNextAndUpdUINoLock() = mainApplication.run {
        viewModel.hasStartedPlaying.value = true

        val curIndex = (curInd.await() + 1).let { if (it == curPlaylist.size) 0 else it }
        val curPath = curPlaylist[curIndex].path

        runOnWorkerThread { playAudio(curPath, isLocking = true) }
        setRepeatButtonImage(isLocking = false)
    }

    /** Plays next track and updates UI for it */
    internal suspend fun playNextAndUpdUI(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { playNextAndUpdUINoLock() }
        else -> playNextAndUpdUINoLock()
    }

    private suspend fun playPrevAndUpdUINoLock() = mainApplication.run {
        viewModel.hasStartedPlaying.value = true
        binding.playingLayout.trackPlayingBar.progress = 0

        val curIndex = (curInd.await() - 1).let { if (it < 0) curPlaylist.size - 1 else it }
        val curPath = curPlaylist[curIndex].path

        runOnWorkerThread {
            playAudio(curPath, isLocking = true)
            StorageUtil.getInstanceAsyncSynchronized().storeTrackPathLocking(curPath)
        }
        setRepeatButtonImage(isLocking = false)
        binding.playingLayout.currentTime.setText(R.string.undefined_time)
    }

    /** Plays previous track and updates UI for it */
    internal suspend fun playPrevAndUpdUI(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { playPrevAndUpdUINoLock() }
        else -> playPrevAndUpdUINoLock()
    }

    private suspend fun runCalculationOfSeekBarPosNoLock() {
        var currentPosition: Int
        val total = curTrack.await()!!.duration.toInt()
        binding.playingLayout.trackPlayingBar.max = total

        while (!this@MainActivity.isDestroyed && isPlaying == true && !isSeekBarDragging) {
            currentPosition = curTimeData.await()
            binding.playingLayout.trackPlayingBar.progress = currentPosition
            delay(50)
        }
    }

    /** Calculates current position for playing seek bar */
    private suspend fun runCalculationOfSeekBarPos(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { runCalculationOfSeekBarPosNoLock() }
        else -> runCalculationOfSeekBarPosNoLock()
    }

    private suspend fun playAudioNoLock(path: String) {
        val oldTrack = curTrack.await()
        StorageUtil.getInstanceAsyncSynchronized().storeTrackPathLocking(path)
        runOnUIThread { updateUIAsync(oldTrack, isLocking = false) }

        when {
            !mainApplication.isAudioServiceBounded -> {
                val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    applicationContext.startForegroundService(playerIntent)
                else
                    applicationContext.startService(playerIntent)

                applicationContext.bindService(
                    playerIntent,
                    mainApplication.audioServiceConnection,
                    BIND_AUTO_CREATE
                )
            }

            else -> {
                if (isPlaying == true)
                    pausePlaying(isLocking = false, isUiUpdating = false)
                sendBroadcast(Intent(Broadcast_PLAY_NEW_TRACK))
            }
        }
    }

    /**
     * Plays track with given path
     * @param path path to track (DATA column from MediaStore)
     */

    private suspend fun playAudio(path: String, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { playAudioNoLock(path) }
        else -> playAudioNoLock(path)
    }

    /**
     * Restarts playing after current track's tags have been changed
     * @param resumeTime time when track was pause to change tags
     */

    internal fun restartPlayingAfterTrackChangedLocked(resumeTime: Int) = sendBroadcast(
        Intent(Broadcast_RESTART_PLAYING_AFTER_TRACK_CHANGED)
            .putExtra(RESUME_POSITION_ARG, resumeTime)
    )

    private suspend fun resumePlayingNoLock(resumePos: Int) {
        when {
            !mainApplication.isAudioServiceBounded -> {
                val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)
                    .putExtra(RESUME_POSITION_ARG, resumePos)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    applicationContext.startForegroundService(playerIntent)
                else
                    applicationContext.startService(playerIntent)

                applicationContext.bindService(
                    playerIntent,
                    mainApplication.audioServiceConnection,
                    BIND_AUTO_CREATE
                )
            }

            else -> {
                if (isPlaying == true)
                    pausePlaying(isLocking = false, isUiUpdating = false)

                sendBroadcast(
                    Intent(Broadcast_RESUME).putExtra(
                        RESUME_POSITION_ARG,
                        resumePos
                    )
                )
            }
        }
    }

    /**
     * Resumes playing after pause
     * @param resumePos resume position in milliseconds
     * (or -1 to continue from paused position)
     */

    internal suspend fun resumePlaying(resumePos: Int = -1, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { resumePlayingNoLock(resumePos) }
        else -> resumePlayingNoLock(resumePos)
    }

    private fun pausePlayingNoLock(isUiUpdating: Boolean) {
        when {
            mainApplication.isAudioServiceBounded -> sendBroadcast(
                Intent(Broadcast_PAUSE).putExtra(UPDATE_UI_ON_PAUSE_ARG, isUiUpdating)
            )

            else -> {
                val playerIntent = Intent(applicationContext, AudioPlayerService::class.java)
                    .putExtra(UPDATE_UI_ON_PAUSE_ARG, isUiUpdating)
                    .setAction(PAUSED_PRESSED_ARG)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    applicationContext.startForegroundService(playerIntent)
                else
                    applicationContext.startService(playerIntent)

                applicationContext.bindService(
                    playerIntent,
                    mainApplication.audioServiceConnection,
                    BIND_AUTO_CREATE
                )
            }
        }
    }

    /**
     * Pauses playing and stores data
     * to [SharedPreferences] if user wishes it
     */

    internal suspend fun pausePlaying(isUiUpdating: Boolean, isLocking: Boolean) = when {
        isLocking -> mutex.withLock { pausePlayingNoLock(isUiUpdating) }
        else -> pausePlayingNoLock(isUiUpdating)
    }

    /**
     * Sets [Params.Looping] status for [AudioPlayerService]
     */

    private fun setLooping() {
        when {
            mainApplication.isAudioServiceBounded -> runOnIOThread {
                sendBroadcast(
                    Intent(Broadcast_LOOPING)
                        .putExtra(
                            IS_LOOPING_ARG,
                            Params.getInstanceSynchronized().loopingStatus.ordinal
                        )
                )
            }

            else -> {
                val playerIntent = Intent(this, AudioPlayerService::class.java)
                    .setAction(LOOPING_PRESSED_ARG)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    applicationContext.startForegroundService(playerIntent)
                else
                    applicationContext.startService(playerIntent)

                applicationContext.bindService(
                    playerIntent,
                    mainApplication.audioServiceConnection,
                    BIND_AUTO_CREATE
                )
            }
        }
    }

    /**
     * Shows popup menu about track
     * @param view settings button view
     * @param track [AbstractTrack] to modify
     * @param bottomSheetBehaviorState state in which function executes
     */

    internal fun onTrackSettingsButtonClicked(
        view: View,
        track: AbstractTrack,
        bottomSheetBehaviorState: Int
    ) {
        if (sheetBehavior.state == bottomSheetBehaviorState)
            PopupMenu(this, view).run {
                menuInflater.inflate(
                    when (currentFragment.get()) {
                        is HiddenTrackListFragment -> R.menu.menu_track_settings_remove_hide
                        else -> R.menu.menu_track_settings_hide
                    },
                    menu
                )

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.nav_change_track_info -> changeTrackInfo(track)
                        R.id.nav_add_to_queue_or_remove -> addOrRemoveTrackFromQueue(track)
                        R.id.nav_add_track_to_favourites_or_remove -> onTrackLikedClicked(track)
                        R.id.nav_add_to_playlist -> addToPlaylistAsync(track)
                        R.id.nav_remove_track -> removeTrack(track)
                        R.id.nav_track_lyrics -> showLyrics(track)
                        R.id.nav_track_info -> showInfo(track)
                        R.id.nav_trim -> trimTrack(track)
                        R.id.hide_track -> hideTrack(track)
                        R.id.remove_from_hidden -> removeTrackFromHidden(track)
                    }

                    true
                }

                show()
            }
    }

    /**
     * Shows popup menu about artist
     * @param view settings button view
     * @param artist [Artist] to modify
     */

    internal fun artistSettingsButtonAction(
        view: View,
        artist: Artist,
    ) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            PopupMenu(this, view).apply {
                menuInflater.inflate(
                    when {
                        isNotCurrentFragment<HiddenArtistListFragment>() -> R.menu.menu_artist_settings_hide
                        else -> R.menu.menu_artist_settings_remove_hide
                    },
                    menu
                )

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.nav_add_artist_to_favourites -> runOnIOThread {
                            val contain = FavouriteRepository
                                .getInstanceSynchronized()
                                .getArtistAsync(artist.name)
                                .await() != null

                            val favouriteArtist = artist.asFavourite()

                            when {
                                contain -> FavouriteRepository
                                    .getInstanceSynchronized()
                                    .removeArtistsAsync(favouriteArtist)

                                else -> FavouriteRepository
                                    .getInstanceSynchronized()
                                    .addArtistsAsync(favouriteArtist)
                            }.join()

                            launch(Dispatchers.Main) {
                                currentFragment.get()
                                    ?.takeIf { it is FavouriteArtistListFragment }
                                    ?.run {
                                        (this as FavouriteArtistListFragment)
                                            .updateUIOnChangeContentAsync()
                                    }
                            }
                        }

                        R.id.nav_hide_artist -> hideArtist(HiddenArtist(artist))
                        R.id.remove_from_hidden -> removeArtistFromHidden(artist)
                    }

                    true
                }

                show()
            }
    }

    /**
     * Call like action when like button pressed.
     * Add or removes track from favourite tracks
     * @param track track to add / remove
     */

    private fun onTrackLikedClicked(track: AbstractTrack) = runOnIOThread {
        val contain = FavouriteRepository
            .getInstanceSynchronized()
            .getTrackAsync(track.path).await() != null

        val favouriteTrack = track.asFavourite()

        when {
            contain -> FavouriteRepository
                .getInstanceSynchronized()
                .removeTracksAsync(favouriteTrack)

            else -> FavouriteRepository
                .getInstanceSynchronized()
                .addTracksAsync(favouriteTrack)
        }.join()

        sendBroadcast(Intent(Broadcast_UPDATE_NOTIFICATION))

        if (currentFragment.get() is FavouriteTrackListFragment)
            (currentFragment.unchecked as FavouriteTrackListFragment)
                .updateUIOnChangeContentAsync()
    }

    /**
     * Runs [TrackChangeFragment]
     * @param track [AbstractTrack] to change
     */

    private fun changeTrackInfo(track: AbstractTrack) {
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    track.androidId
                )

                try {
                    contentResolver.openFileDescriptor(uri, "w")
                        ?.use { showTrackChangeFragment(track) }
                } catch (securityException: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = securityException as?
                                RecoverableSecurityException
                            ?: throw RuntimeException(
                                securityException.message,
                                securityException
                            )

                        recoverableSecurityException
                            .userAction
                            .actionIntent
                            .intentSender
                            .let { intentSender ->
                                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                                    if (result != null)
                                        contentResolver
                                            .openFileDescriptor(uri, "w")
                                            ?.use { showTrackChangeFragment(track) }
                                }.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                    }
                }
            }

            else -> showTrackChangeFragment(track)
        }
    }

    /**
     * Adds track to queue
     * @param track [AbstractTrack] to add
     */

    private fun addTrackToQueue(track: AbstractTrack) {
        mainApplication.curPlaylist.add(track)
    }

    /**
     * Removes track from queue
     * @param track [AbstractTrack] to remove
     * @param willUpdateUI should [currentFragment] update its UI
     * @return true if track is not the last in playlist
     */

    internal suspend fun removeTrackFromQueue(
        track: AbstractTrack,
        willUpdateUI: Boolean
    ): Boolean {
        var isChanged = false

        mainApplication.run {
            if (track.path != getCurPath()) {
                isChanged = true
                curPlaylist.remove(track)
                StorageUtil.getInstanceAsyncSynchronized().storeCurPlaylistLocking(curPlaylist)
            }

            if (willUpdateUI) runOnUIThread {
                if (currentFragment.get() is CurPlaylistTrackListFragment)
                    (currentFragment.unchecked as CurPlaylistTrackListFragment)
                        .updateUIOnChangeContentForPlayingTrackListAsync()
            }
        }

        return isChanged
    }

    private fun addOrRemoveTrackFromQueue(track: AbstractTrack) = runOnWorkerThread {
        when (track) {
            in mainApplication.curPlaylist ->
                removeTrackFromQueue(track, willUpdateUI = true)

            else -> addTrackToQueue(track)
        }
    }

    /**
     * Adds track to playlist asynchronously
     * @param track [AbstractTrack] to add
     */

    private fun addToPlaylistAsync(track: AbstractTrack) = runOnIOThread {
        val task = CustomPlaylistsRepository
            .getInstanceSynchronized()
            .getPlaylistsByTrackAsync(track.path)

        launch(Dispatchers.Main) {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(
                    R.id.fragment_container,
                    PlaylistSelectFragment.newInstance(
                        track,
                        task.await().toTypedArray()
                    )
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()
        }
    }

    /**
     * Removes track from playlist
     * @param track [AbstractTrack] to remove
     */

    private fun removeTrack(track: AbstractTrack) = QuestionDialog(
        R.string.remove_track_message
    ) {
        runOnIOThread {
            StatisticsRepository
                .getInstanceSynchronized()
                .removeTrackAsync(track.path)

            FavouriteRepository
                .getInstanceSynchronized()
                .removeTrackAsync(track.path)

            CustomPlaylistsRepository
                .getInstanceSynchronized()
                .removeTrackAsync(track.path)
        }

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> try {
                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
                    .launch(
                        IntentSenderRequest
                            .Builder(
                                MediaStore
                                    .createDeleteRequest(contentResolver, listOf(uri))
                                    .intentSender
                            )
                            .build()
                    )
            } catch (ignored: Exception) {
            }

            else -> {
                contentResolver.delete(
                    uri,
                    "${MediaStore.Audio.Media._ID} = ?",
                    arrayOf(track.androidId.toString())
                )

                try {
                    File(track.path).delete()
                } catch (securityException: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val recoverableSecurityException = securityException as?
                                RecoverableSecurityException
                            ?: throw RuntimeException(
                                securityException.message,
                                securityException
                            )

                        recoverableSecurityException
                            .userAction
                            .actionIntent
                            .intentSender
                            .let { intentSender ->
                                registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                                    File(track.path).delete()
                                }.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                    }
                }
            }
        }

        runOnIOThread {
            mainApplication.loadAsync().join()
            launch(Dispatchers.Main) {
                (currentFragment.unchecked as? AbstractTrackListFragment<*>)
                    ?.updateUIOnChangeContentAsync()
            }
        }

    }.show(supportFragmentManager, null)

    /**
     * Shows dialog to input title and artist to search for lyrics
     * @param track searchable track
     */

    private fun showLyrics(track: AbstractTrack) = TrackSearchLyricsParamsDialog(track)
        .show(supportFragmentManager, null)

    /**
     * Shows dialog to input title and artist to search for info
     * @param track searchable track
     */

    private fun showInfo(track: AbstractTrack) = TrackSearchInfoParamsDialog(track)
        .show(supportFragmentManager, null)

    private suspend fun customizeNoLock(
        newTrack: AbstractTrack?,
        isImageUpdateNeed: Boolean,
        isDefaultPlaying: Boolean,
    ) {
        val isP = isPlaying ?: isDefaultPlaying
        setPlayButtonImage(isP, isLocking = false)
        setPlayButtonSmallImage(isP, isLocking = false)
        setSmallAlbumImageAnimation(isP, isLocking = false)

        if (isImageUpdateNeed) curTrack.await()?.let {
            updateUIAsync(newTrack to it, isLocking = false)
        }
    }

    /**
     * Update UI on service notification clicks
     * @param newTrack new track which should be used to replace current UI
     * @param isImageUpdateNeed does track image need update
     * @param isDefaultPlaying needs default playing
     */

    internal suspend fun customizeAsync(
        newTrack: AbstractTrack?,
        isImageUpdateNeed: Boolean,
        isDefaultPlaying: Boolean = true,
        isLocking: Boolean
    ) = when {
        isLocking -> mutex.withLock {
            customizeNoLock(newTrack, isImageUpdateNeed, isDefaultPlaying)
        }

        else -> customizeNoLock(newTrack, isImageUpdateNeed, isDefaultPlaying)
    }

    private suspend fun handlePlayEventNoLock() = when (isPlaying) {
        true -> runOnWorkerThread {
            pausePlaying(isLocking = true, isUiUpdating = true)
            viewModel.hasStartedPlaying.value = true
        }

        else -> runOnWorkerThread {
            if (!EqualizerSettings.instance.isEqualizerEnabled &&
                StorageUtil.instance.loadStartWithEqualizer()
            ) mainApplication.startEqualizer()

            resumePlaying(isLocking = false)
            reinitializePlayingCoroutine(isLocking = false)
        }
    }

    /**
     * Pauses or resumes playing
     */

    private suspend fun handlePlayEvent(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { handlePlayEventNoLock() }
        else -> handlePlayEventNoLock()
    }

    private fun reinitializePlayingCoroutineNoLock() {
        playingCoroutine = runOnWorkerThread {
            runCalculationOfSeekBarPos(isLocking = false)
        }
    }

    /** Reinitializes playing coroutine to show time */

    internal suspend fun reinitializePlayingCoroutine(isLocking: Boolean) = when {
        isLocking -> mutex.withLock { reinitializePlayingCoroutineNoLock() }
        else -> reinitializePlayingCoroutineNoLock()
    }

    /**
     * Sets rounding of playlists images
     * for different configurations of devices
     * @deprecated No need because of data binding
     */

    @Deprecated("No need because of data binding")
    internal fun setRoundingOfPlaylistImage() = runOnUIThread {
        binding.playingLayout.albumPicture.setCornerRadius(
            when {
                !Params.getInstanceSynchronized().areCoversRounded -> 0F
                else -> when (resources.configuration.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) {
                    Configuration.SCREENLAYOUT_SIZE_NORMAL -> 50F
                    Configuration.SCREENLAYOUT_SIZE_LARGE -> 60F
                    else -> 40F
                }
            }
        )
    }

    /**
     * Hides or shows cover on playback panel
     * @deprecated No need because of data binding
     */

    @Deprecated("No need because of data binding")
    internal fun setHidingCover() = runOnUIThread {
        binding.playingLayout.albumPicture.visibility = when {
            Params.getInstanceSynchronized().isCoverHidden -> View.INVISIBLE
            else -> View.VISIBLE
        }
    }

    /**
     * Starts or stop rotation of
     * track's cover on small playback panel
     * @param isCoverRotating if true [startRotation] else [stopRotation]
     */

    internal fun startOrStopCoverRotating(isCoverRotating: Boolean) = runOnUIThread {
        when {
            isCoverRotating -> startRotation()
            else -> stopRotation()
        }
    }

    private fun startRotation() {
        smallAlbumImageAnimator.start()
        smallAlbumImageAnimator.pause()

        if (isPlaying == true)
            smallAlbumImageAnimator.resume()
    }

    private fun stopRotation() {
        if (smallAlbumImageAnimator.isStarted)
            smallAlbumImageAnimator.end()
    }

    /** Initialises audio visualizer */

    internal fun initAudioVisualizer() = try {
        binding.playingLayout.visualizer.run {
            setAnimationSpeed(AnimSpeed.FAST)
            runOnUIThread { setColor(Params.getInstanceSynchronized().primaryColor) }
            setAudioSessionId(((mainApplication.audioSessionId) ?: 0))
        }
    } catch (e: Exception) {
        // already initialized or first time open app
        releaseAudioVisualizer()
        binding.playingLayout.visualizer.run {
            setAnimationSpeed(AnimSpeed.FAST)
            runOnUIThread { setColor(Params.getInstanceSynchronized().primaryColor) }

            try {
                setAudioSessionId(((mainApplication.audioSessionId) ?: 0))
            } catch (ignored: Exception) {
                // Open app for the first time
            }
        }
    }

    internal fun releaseAudioVisualizer() = binding.playingLayout.visualizer.release()

    private fun showTrackChangeFragment(track: AbstractTrack) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                TrackChangeFragment.newInstance(track)
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    internal fun showHiddenFragment() {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                AbstractFragment.defaultInstance(null, HiddenHolderFragment::class)
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /** Updates looping status in activity */

    internal fun updateLooping(isOnlyUIUpdate: Boolean) = runOnUIThread {
        if (!isOnlyUIUpdate) {
            Params.getInstanceSynchronized().loopingStatus++
            setLooping()
        }

        setRepeatButtonImage(isLocking = true)
    }

    internal fun liftPlayingMenu() {
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    internal fun onPlayingPrevTrackClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            runOnUIThread { playPrevAndUpdUI(isLocking = true) }
    }

    internal fun onPlayingNextTrackClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            runOnUIThread { playNextAndUpdUI(isLocking = true) }
    }

    internal fun onRecordButtonClicked() = when {
        isMicRecording -> {
            sendBroadcast(Intent(Broadcast_MIC_STOP_RECORDING))
            runOnUIThread { setRecordButtonImage(false, isLocking = true) }
            Unit
        }

        isPlaybackRecording -> {
            sendBroadcast(Intent(Broadcast_PLAYBACK_STOP_RECORDING))
            runOnUIThread { setRecordButtonImage(false, isLocking = true) }
            Unit
        }

        else -> RecordParamsDialogFragment(this).show()
    }

    internal fun onPlaylistButtonClicked() = CurPlaylistTrackListFragment
        .newInstance()
        .show(supportFragmentManager, "CurPlaylistTrackListFragment")

    @Deprecated(
        message = "Now using BottomSheetDialogFragment",
        replaceWith = ReplaceWith("onPlaylistButtonClicked")
    )
    internal fun onPlaylistButtonClickedOld() {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
            )
            .replace(
                R.id.fragment_container,
                AbstractFragment.defaultInstance(
                    resources.getString(R.string.current_playlist),
                    CurPlaylistTrackListFragmentOld::class
                )
            )
            .addToBackStack(null)
            .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
            .commit()
    }

    internal fun onSleepTimerClicked() = SleepDialog(mainApplication)
        .show(supportFragmentManager, null)

    internal fun onReturnButtonClicked() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    internal fun onTrackSettingsButtonClicked(view: View) = runOnUIThread {
        onTrackSettingsButtonClicked(
            view,
            curTrack.await()!!,
            BottomSheetBehavior.STATE_EXPANDED
        )
    }

    internal fun onPlayButtonClicked() = runOnUIThread {
        val isPlaying = isPlaying?.let { !it } ?: true
        setPlayButtonImage(isPlaying, isLocking = true)
        setSmallAlbumImageAnimation(isPlaying, isLocking = true)
        handlePlayEvent(isLocking = true)
    }

    internal fun onPlayingPlayButtonClicked() = runOnUIThread {
        val isPlaying = isPlaying?.let { !it } ?: true
        setPlayButtonSmallImage(isPlaying, isLocking = true)
        setSmallAlbumImageAnimation(isPlaying, isLocking = true)
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED)
            handlePlayEvent(isLocking = true)
    }

    internal fun onEqualizerButtonClicked() = when (isPlaying) {
        null -> Toast.makeText(
            this,
            R.string.first_play,
            Toast.LENGTH_LONG
        ).show()

        else -> try {
            mainApplication.musicPlayer?.playbackParams

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    EqualizerFragment.newInstance(
                        mainApplication.audioSessionId!!
                    )
                )
                .addToBackStack(null)
                .commit()

            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            else Unit
        } catch (e: Exception) {
            // AudioService is dead

            Toast.makeText(
                this,
                R.string.first_play,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    internal fun onTrimButtonClicked() = runOnUIThread { trimTrack(curTrack.await()!!) }

    @SuppressLint("SyntheticAccessor")
    override fun initView(savedInstanceState: Bundle?) {
        _binding = when (Params.instance.visualizerStyle) {
            Params.Companion.VisualizerStyle.BAR -> Either.Left(
                DataBindingUtil
                    .setContentView<ActivityMainBarBinding>(this, R.layout.activity_main_bar)
                    .apply {
                        val vm = com.dinaraparanid.prima.mvvmp.old_shit.MainActivityViewModel(
                            WeakReference(this@MainActivity)
                        )

                        playingLayoutBar.viewModel = vm
                        viewModel = vm

                        val headerBinding = DataBindingUtil.inflate<NavHeaderMainBinding>(
                            layoutInflater,
                            R.layout.nav_header_main,
                            navView,
                            false
                        )

                        mainLabel.isSelected = true
                        navView.addHeaderView(headerBinding.root)
                        headerBinding.viewModel = BasePresenter()
                        executePendingBindings()
                    }
            )

            Params.Companion.VisualizerStyle.WAVE -> Either.Right(
                DataBindingUtil
                    .setContentView<ActivityMainWaveBinding>(this, R.layout.activity_main_wave)
                    .apply {
                        val vm = com.dinaraparanid.prima.mvvmp.old_shit.MainActivityViewModel(
                            WeakReference(this@MainActivity)
                        )

                        playingLayoutWave.viewModel = vm
                        viewModel = vm

                        val headerBinding = DataBindingUtil.inflate<NavHeaderMainBinding>(
                            layoutInflater,
                            R.layout.nav_header_main,
                            navView,
                            false
                        )

                        navView.addHeaderView(headerBinding.root)
                        headerBinding.viewModel = BasePresenter()
                        executePendingBindings()
                    }
            )
        }

        awaitBindingInitCondition.open()

        runOnUIThread {
            Params.getInstanceSynchronized().backgroundImage?.run {
                binding.drawerLayout.background = toBitmap().toDrawable(resources)
            }
        }

        viewModel.run {
            load(
                savedInstanceState?.getInt(SHEET_BEHAVIOR_STATE_KEY),
                savedInstanceState?.getBoolean(PROGRESS_KEY),
                savedInstanceState?.getBoolean(TRACK_SELECTED_KEY),
            )

            if (!hasStartedPlaying.value) runOnIOThread {
                hasStartedPlaying.value = StorageUtil
                    .getInstanceAsyncSynchronized()
                    .loadTrackPauseTimeLocking() != -1
            }
        }

        setSupportActionBar(binding.switchToolbar)
        setRoundingOfPlaylistImage()

        if (Params.instance.isCoverRotating)
            startRotation()

        runOnUIThread {
            binding.playingLayout.currentTime.text =
                calcTrackTime(curTimeData.await()).asTimeString()
        }

        mainApplication.run {
            mainActivity = WeakReference(this@MainActivity)
            runOnWorkerThread { loadAsync() }
        }

        Glide.with(this)
            .load(ViewSetter.getRecordButtonImage(isMicRecording))
            .into(binding.playingLayout.recordButton)

        binding.playingLayout.trackPlayingBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isSeekBarDragging = true
                }

                @SuppressLint("SyntheticAccessor")
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    binding.playingLayout.currentTime.text =
                        calcTrackTime(progress).asTimeString()

                    if (ceil(progress / 1000.0).toInt() == 0 && isPlaying == false)
                        binding.playingLayout.trackPlayingBar.progress = 0
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    mainApplication.run {
                        isSeekBarDragging = false

                        val time = seekBar!!.progress

                        runOnWorkerThread {
                            if (isPlaying == true)
                                pausePlaying(isLocking = true, isUiUpdating = false)

                            resumePlaying(time, isLocking = true)
                            reinitializePlayingCoroutine(isLocking = true)
                        }
                    }
                }
            }
        )

        runOnUIThread {
            val isPlaying = isPlaying ?: false
            setPlayButtonImage(isPlaying, isLocking = true)
            setPlayButtonSmallImage(isPlaying, isLocking = true)
            setSmallAlbumImageAnimation(isPlaying, isLocking = true)

            mainApplication.apply {
                mainActivity = WeakReference(this@MainActivity)

                getCurPath()
                    .takeIf { it != Params.NO_PATH }
                    ?.let { highlightedPath = it }
            }
        }

        initFirstFragment()
        sheetBehavior = BottomSheetBehavior.from(binding.playingLayout.playing)

        if (viewModel.trackSelectedFlow.value || viewModel.hasStartedPlaying.value) {
            when (viewModel.sheetBehaviorPositionFlow.value) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    binding.playingLayout.returnButton?.alpha = 1F
                    binding.playingLayout.trackSettingsButton.alpha = 1F
                    binding.playingLayout.albumPicture.alpha = 1F
                    binding.appbar.alpha = 0F
                    binding.playingLayout.playingToolbar.alpha = 0F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = false
                }

                else -> {
                    binding.playingLayout.returnButton?.alpha = 0F
                    binding.playingLayout.trackSettingsButton.alpha = 0F
                    binding.playingLayout.albumPicture.alpha = 0F
                    binding.appbar.alpha = 1F
                    binding.playingLayout.playingToolbar.alpha = 1F
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                    binding.switchToolbar.isVisible = true
                }
            }

            runOnUIThread {
                if (getCurPath() != Params.NO_PATH)
                    viewModel.trackSelectedFlow.value = true

                curTrack.await()?.let {
                    mainApplication.startPath = when (Params.NO_PATH) {
                        getCurPath() -> null
                        else -> getCurPath()
                    }

                    initPlayingView(it)
                }
            }
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                @SuppressLint("SyntheticAccessor")
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        val binding = binding
                        binding.playingLayout.returnButton?.alpha = 1F
                        binding.playingLayout.trackSettingsButton.alpha = 1F
                        binding.playingLayout.albumPicture.alpha = 1F
                        binding.appbar.alpha = 0F
                        binding.playingLayout.playingToolbar.alpha = 0F
                        binding.playingLayout.playingTrackTitle.isSelected = true
                        binding.playingLayout.playingTrackArtists.isSelected = true
                        binding.switchToolbar.isVisible = false
                    }
                }

                @SuppressLint("SyntheticAccessor")
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val binding = binding

                    if (!binding.switchToolbar.isVisible)
                        binding.switchToolbar.isVisible = true

                    val isPlaying = isPlaying ?: false

                    runOnUIThread {
                        setPlayButtonSmallImage(isPlaying, isLocking = true)
                        setPlayButtonImage(isPlaying, isLocking = true)
                        setSmallAlbumImageAnimation(isPlaying, isLocking = true)

                        binding.playingLayout.trimButton
                            .setTint(Params.getInstanceSynchronized().primaryColor)
                    }

                    binding.appbar.alpha = 1 - slideOffset
                    binding.playingLayout.playingToolbar.alpha = 1 - slideOffset
                    binding.playingLayout.returnButton?.alpha = slideOffset
                    binding.playingLayout.trackSettingsButton.alpha = slideOffset
                    binding.playingLayout.albumPicture.alpha = slideOffset
                    binding.playingLayout.playingTrackTitle.isSelected = true
                    binding.playingLayout.playingTrackArtists.isSelected = true
                }
            }
        )

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.switchToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.run {
            setNavigationItemSelectedListener(this@MainActivity)
            itemIconTintList = ViewSetter.colorStateList
        }

        runOnUIThread {
            if (getCurPath() != Params.NO_PATH) {
                val isPlaying = isPlaying ?: false
                setPlayButtonImage(isPlaying, isLocking = true)
                setPlayButtonSmallImage(isPlaying, isLocking = true)
                setSmallAlbumImageAnimation(isPlaying, isLocking = true)

                if (viewModel.sheetBehaviorPositionFlow.value ==
                    BottomSheetBehavior.STATE_EXPANDED
                ) sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val tv = TypedValue()

        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun initFirstFragment() {
        val viewPager = WeakReference(
            supportFragmentManager.findFragmentById(R.id.fragment_container)
        )

        if (viewPager.get() == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    when (Params.instance.homeScreen) {
                        Params.Companion.HomeScreen.TRACK_COLLECTION -> AbstractFragment.defaultInstance(
                            null,
                            TrackCollectionsFragment::class
                        )

                        Params.Companion.HomeScreen.FAVOURITES -> AbstractFragment.defaultInstance(
                            null,
                            FavouritesFragment::class
                        )

                        Params.Companion.HomeScreen.TRACKS -> getMainFragment(0)
                        Params.Companion.HomeScreen.ARTISTS -> getMainFragment(1)
                        Params.Companion.HomeScreen.MP3_CONVERTER -> getMainFragment(2)
                        Params.Companion.HomeScreen.GUESS_THE_MELODY -> getMainFragment(3)
                        Params.Companion.HomeScreen.SETTINGS -> getMainFragment(4)
                        Params.Companion.HomeScreen.ABOUT_APP -> getMainFragment(5)

                        else -> throw IllegalArgumentException("OldCurrentPlaylistTrackListFragment was called")
                    }
                )
                .commit()
    }

    /**
     * Initializes playing view
     * when [onCreate] and [onResume] called
     *
     * @param track that should be played
     */

    private fun initPlayingView(track: AbstractTrack) = onTrackSelected(
        track,
        mainApplication.allTracks,
        needToPlay = false // Only for playing panel
    )

    /**
     * Sets bloom (shadow) color if settings have changed
     * @param color to set
     */

    internal fun setBloomColor(color: Int) = binding.playingLayout.run {
        trackSettingsButton.setShadowColor(color)
        albumPicture.setShadowColor(color)
        playButton.setShadowColor(color)
        previousTrackButton.setShadowColor(color)
        nextTrackButton.setShadowColor(color)
        equalizerButton.setShadowColor(color)
        repeatButton.setShadowColor(color)
        sleepTimer.setShadowColor(color)
        recordButton.setShadowColor(color)
        playlistButton.setShadowColor(color)
        trimButton.setShadowColor(color)
        returnButton?.setShadowColor(color)
    }

    /**
     * Makes background invisible and sets given [image] as background image
     * @param image to set on background
     */

    internal fun updateViewOnUserImageSelectedAsync(image: Uri) = runOnIOThread {
        val cr = contentResolver
        val bytes = cr.openInputStream(image)!!.use {
            it.buffered().use(BufferedInputStream::readBytes)
        }

        StorageUtil.getInstanceAsyncSynchronized().storeBackgroundImageAsync(bytes)
        Params.getInstanceSynchronized().backgroundImage = bytes

        runOnUIThread {
            binding.run {
                switchToolbar.setBackgroundColor(Color.TRANSPARENT)
                appbar.setBackgroundColor(Color.TRANSPARENT)
                drawerLayout.background = Drawable.createFromStream(
                    cr.openInputStream(image)!!,
                    image.toString()
                )
            }
        }
    }

    /**
     * Sets colors on background
     * if user chose to remove background image
     */

    internal fun updateBackgroundViewOnRemoveUserImage() = binding.run {
        runOnUIThread {
            drawerLayout.setBackgroundColor(Params.getInstanceSynchronized().secondaryColor)
            appbar.setBackgroundColor(Params.getInstanceSynchronized().primaryColor)
            switchToolbar.setBackgroundColor(Params.getInstanceSynchronized().primaryColor)
        }
    }

    /**
     * Should be called before [MainActivity] is stopped or destroyed.
     * Saves time and releases everything
     */

    internal fun finishWork() {
        mainApplication.run {
            runOnWorkerThread { savePauseTimeAsync() }
            mainActivity = WeakReference(null)
        }

        playingCoroutine?.cancel(null)
        playingCoroutine = null
        destroyAwaitDialog()
        stopRotation()
    }

    private fun registerHighlightTrackReceiver() = registerReceiver(
        highlightTrackReceiver,
        IntentFilter(AudioPlayerService.Broadcast_HIGHLIGHT_TRACK)
    )

    private fun registerCustomizeReceiver() = registerReceiver(
        customizeReceiver,
        IntentFilter(AudioPlayerService.Broadcast_CUSTOMIZE)
    )

    private fun registerReleaseAudioVisualizerReceiver() = registerReceiver(
        releaseAudioVisualizerReceiver,
        IntentFilter(AudioPlayerService.Broadcast_RELEASE_AUDIO_VISUALIZER)
    )

    private fun registerInitAudioVisualizerReceiver() = registerReceiver(
        initAudioVisualizerReceiver,
        IntentFilter(AudioPlayerService.Broadcast_INIT_AUDIO_VISUALIZER)
    )

    private fun registerPrepareForPlayingReceiver() = registerReceiver(
        prepareForPlayingReceiver,
        IntentFilter(AudioPlayerService.Broadcast_PREPARE_FOR_PLAYING)
    )

    private fun registerUpdateLoopingReceiver() = registerReceiver(
        updateLoopingReceiver,
        IntentFilter(AudioPlayerService.Broadcast_UPDATE_LOOPING)
    )

    @Deprecated("Like button is not used anymore. Replaced by audio recording")
    private fun registerSetLikeButtonImageReceiver() = registerReceiver(
        setLikeButtonImageReceiver,
        IntentFilter(AudioPlayerService.Broadcast_SET_LIKE_BUTTON_IMAGE)
    )

    private fun registerMicRecordButtonSetImageReceiver() = registerReceiver(
        setRecordButtonImageReceiver,
        IntentFilter(MicRecordService.Broadcast_SET_RECORD_BUTTON_IMAGE)
    )

    private fun registerUpdateFavouriteTracksFragmentReceiver() = registerReceiver(
        updateFavouriteTracksFragmentReceiver,
        IntentFilter(AudioPlayerService.Broadcast_UPDATE_FAVOURITE_TRACKS_FRAGMENT)
    )

    private suspend fun setBackingCountToDefault() = coroutineScope {
        delay(1000)
        backClicksCount = 2
    }

    private fun trimTrack(track: AbstractTrack) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrimFragment.newInstance(track)
            )
            .addToBackStack(null)
            .commit()

        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun updateContentOfCurrentFragmentAsync() =
        (currentFragment.unchecked as MainActivityUpdatingListFragment<*, *, *, *>)
            .updateUIOnChangeContentAsync()

    private fun hideTrack(track: AbstractTrack) = runOnIOThread {
        HiddenRepository
            .getInstanceSynchronized()
            .insertTracksAsync(HiddenTrack(track))
            .join()

        mainApplication.loadAsync()

        updateContentOfCurrentFragmentAsync()
        curPlaylistFragment.get()?.updateUIOnChangeContentForPlayingTrackListAsync()
    }

    fun hideArtist(artist: HiddenArtist) = runOnIOThread {
        val insertArtistTask: Job
        HiddenRepository
            .getInstanceSynchronized()
            .run {
                insertArtistTask = insertArtistsAsync(artist)

                launch(Dispatchers.IO) {
                    val name = artist.name

                    insertTracksAsync(
                        *mainApplication.allTracksWithoutHidden
                            .filter { it.artist == name }
                            .map(::HiddenTrack)
                            .toTypedArray()
                    ).join()

                    mainApplication.loadAsync()
                }
            }

        insertArtistTask.join()
        updateContentOfCurrentFragmentAsync()
        curPlaylistFragment.get()?.updateUIOnChangeContentForPlayingTrackListAsync()
    }

    fun hidePlaylist(playlist: HiddenPlaylist) = runOnIOThread {
        val insertPlaylistTask: Job
        HiddenRepository
            .getInstanceSynchronized()
            .run {
                insertPlaylistTask = insertPlaylistsAsync(playlist)

                launch(Dispatchers.IO) {
                    val title = playlist.title

                    when (playlist.type) {
                        AbstractPlaylist.PlaylistType.ALBUM -> insertTracksAsync(
                            *mainApplication.allTracksWithoutHidden
                                .filter { it.album == title }
                                .map(::HiddenTrack)
                                .toTypedArray()
                        )

                        AbstractPlaylist.PlaylistType.CUSTOM -> insertTracksAsync(
                            *CustomPlaylistsRepository
                                .getInstanceSynchronized()
                                .getTracksOfPlaylistAsync(title)
                                .await()
                                .map(::HiddenTrack)
                                .toTypedArray()
                        )

                        AbstractPlaylist.PlaylistType.GTM -> throw IllegalArgumentException("GTM playlist in hidden")
                    }.join()

                    mainApplication.loadAsync()
                }
            }

        insertPlaylistTask.join()
        updateContentOfCurrentFragmentAsync()
        curPlaylistFragment.get()?.updateUIOnChangeContentForPlayingTrackListAsync()
    }

    private fun removeTrackFromHidden(track: AbstractTrack) = runOnIOThread {
        HiddenRepository
            .getInstanceSynchronized()
            .removeTracksAsync(HiddenTrack(track))
            .join()

        mainApplication.loadAsync()
        updateContentOfCurrentFragmentAsync()
    }

    fun removeArtistFromHidden(artist: Artist) = runOnIOThread {
        val removeArtistTask: Job
        HiddenRepository
            .getInstanceSynchronized()
            .run {
                val hiddenArtist = HiddenArtist(artist)
                removeArtistTask = removeArtistsAsync(hiddenArtist)

                launch(Dispatchers.IO) {
                    removeArtistTask.join()
                    removeTracksOfArtistAsync(artist.name).join()
                    mainApplication.loadAsync()
                }
            }

        removeArtistTask.join()
        updateContentOfCurrentFragmentAsync()
    }

    fun removePlaylistFromHidden(playlist: AbstractPlaylist) = runOnIOThread {
        val removePlaylistTask: Job
        HiddenRepository
            .getInstanceSynchronized()
            .run {
                removePlaylistTask = removePlaylistAsync(playlist.title, playlist.type)

                launch(Dispatchers.IO) {
                    val title = playlist.title

                    when (playlist.type) {
                        AbstractPlaylist.PlaylistType.ALBUM -> removeTracksOfAlbumAsync(title)

                        AbstractPlaylist.PlaylistType.CUSTOM -> removeTracksAsync(
                            *CustomPlaylistsRepository
                                .getInstanceSynchronized()
                                .getTracksOfPlaylistAsync(title)
                                .await()
                                .map(::HiddenTrack)
                                .toTypedArray()
                        )

                        AbstractPlaylist.PlaylistType.GTM -> throw IllegalArgumentException("GTM playlist in hidden")
                    }.join()

                    mainApplication.loadAsync()
                }
            }

        removePlaylistTask.join()
        updateContentOfCurrentFragmentAsync()
    }

    internal fun requestMainPermissions() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> EasyPermissions.requestPermissions(
            this,
            resources.getString(R.string.main_permissions_why),
            MAIN_PERMISSIONS_REQUEST_CODE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )

        else -> EasyPermissions.requestPermissions(
            this,
            resources.getString(R.string.main_permissions_why),
            MAIN_PERMISSIONS_REQUEST_CODE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )
    }

    internal fun requestWriteExternalStoragePermission() = EasyPermissions.requestPermissions(
        this,
        resources.getString(R.string.write_external_storage_permission_why),
        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    internal fun startMediaProjectionRequest() {
        mediaProjectionManager = applicationContext
            .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionIntentResultListener.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun destroyAwaitDialog() = runOnUIThread {
        awaitDialog?.await()?.dismiss()
        awaitDialog = null
    }

    private fun highlightTrackFragmentsAsync() = runOnWorkerThread {
        val path = curTrack.await()!!.path
        curPlaylistFragment.get()?.highlightAsync(path)?.join()
        (currentFragment.get() as? AbstractTrackListFragment<*>?)?.highlightAsync(path)?.join()
        mainApplication.highlightedPath = path
    }

    private inline val isWriteExternalStoragePermissionGranted
        get() = when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ->
                EasyPermissions.hasPermissions(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            else -> true
        }

    /** Sets playing background image when activity was refreshed */
    private suspend fun setPlayingBackgroundImage() {
        curTrack.await()?.path?.let {
            val cover = mainApplication
                .getAlbumPictureAsync(it).await()

            Glide.with(this)
                .load(cover.toDrawable(resources))
                .placeholder(cover.toDrawable(resources))
                .fallback(R.drawable.album_default)
                .error(R.drawable.album_default)
                .transition(DrawableTransitionOptions.withCrossFade())
                .run {
                    val playing = binding.playingLayout.playing

                    when {
                        Params.getInstanceSynchronized().isBlurEnabled -> {
                            override(playing.width, playing.height)
                                .transform(BlurTransformation(15, 5))
                                .into(object : CustomViewTarget<ConstraintLayout, Drawable>(playing) {
                                    override fun onLoadFailed(errorDrawable: Drawable?) = Unit

                                    @SuppressLint("SyntheticAccessor")
                                    override fun onResourceCleared(placeholder: Drawable?) {
                                        binding.playingLayout.playing.background = null
                                        binding.playingLayout.playing.setBackgroundColor(Params.instance.secondaryColor)
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) { playing.background = resource }
                                })
                        }

                        else -> {
                            playing.background = null
                            playing.setBackgroundColor(Params.getInstanceSynchronized().secondaryColor)
                        }
                    }
                }
        }
    }
}