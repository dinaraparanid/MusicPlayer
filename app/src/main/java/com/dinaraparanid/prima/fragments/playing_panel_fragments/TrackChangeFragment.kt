package com.dinaraparanid.prima.fragments.playing_panel_fragments

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ConditionVariable
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import arrow.core.toOption
import carbon.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.databases.entities.images.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databases.repositories.StatisticsRepository
import com.dinaraparanid.prima.databinding.FragmentChangeTrackInfoBinding
import com.dinaraparanid.prima.databinding.ListItemImageBinding
import com.dinaraparanid.prima.databinding.ListItemSongBinding
import com.dinaraparanid.prima.dialogs.MessageDialog
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.unwrapOr
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.*
import com.dinaraparanid.prima.utils.polymorphism.fragments.ChangeImageFragment
import com.dinaraparanid.prima.utils.polymorphism.fragments.setMainLabelInitialized
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.utils.web.genius.songs_response.SongsResponse
import com.dinaraparanid.prima.viewmodels.androidx.TrackChangeViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Fragment to change track's metadata */

class TrackChangeFragment :
    CallbacksFragment<FragmentChangeTrackInfoBinding, MainActivity>(),
    Rising,
    UIUpdatable<Pair<String, String>>,
    ChangeImageFragment,
    MainActivityFragment,
    AsyncContext,
    StatisticsUpdatable {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Makes selected image new track's album image
         * @param image image to select
         * @param albumImageView album image view which image should be replaced
         */

        fun onImageSelected(image: Bitmap, albumImageView: ImageView)

        /**
         * Makes changeable track's metadata
         * equal to selected found track's metadata
         * @param selectedTrack track to select
         * @param titleInput [EditText] for title
         * @param artistInput [EditText] for artist
         * @param albumInput [EditText] for album image
         * @param numberInAlbumInput [EditText] for track's number in album
         */

        suspend fun onTrackSelected(
            selectedTrack: Song,
            titleInput: EditText,
            artistInput: EditText,
            albumInput: EditText,
            numberInAlbumInput: EditText
        )
    }

    override val mutex = Mutex()
    override var isMainLabelInitialized = false
    override val awaitMainLabelInitCondition = ConditionVariable()
    override lateinit var mainLabelCurText: String

    override var binding: FragmentChangeTrackInfoBinding? = null
    override val updateStyle = Statistics::withIncrementedNumberOfChanged

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private lateinit var track: AbstractTrack
    private var awaitDialog: Deferred<KProgressHUD>? = null

    private inline val isPlaying
        get() = application.musicPlayer?.isPlaying

    private suspend fun getCurPath() = StorageUtil.getInstanceSynchronized().loadTrackPathLocking()

    private val imagesAdapter by lazy {
        ImageAdapter().apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    private val tracksAdapter by lazy {
        TrackAdapter().apply {
            stateRestorationPolicy =
                androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
    }

    private val viewModel: TrackChangeViewModel by lazy {
        ViewModelProvider(this)[TrackChangeViewModel::class.java]
    }

    private val geniusFetcher by lazy { GeniusFetcher() }

    internal companion object {
        private const val ALBUM_IMAGE_PATH_KEY = "album_image_path"
        private const val ALBUM_IMAGE_URI_KEY = "album_image_uri"
        private const val TITLE_KEY = "title"
        private const val ARTIST_KEY = "artist"
        private const val ALBUM_KEY = "album"
        private const val TRACK_NUMBER_IN_ALBUM_KEY = "track_number_in_album"
        private const val TRACK_LIST_KEY = "track_list"
        private const val WAS_LOADED_KEY = "was_loaded"
        private const val TRACK_KEY = "track"
        private const val ADD_IMAGE_FROM_STORAGE = "add_image_from_storage"

        /**
         * Creates new instance of fragment with params
         * @param track track to change
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(track: AbstractTrack) = TrackChangeFragment().apply {
            arguments = Bundle().apply { putSerializable(TRACK_KEY, track) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack
        mainLabelCurText = resources.getString(R.string.change_track_s_information)

        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setMainActivityMainLabel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.load(
            savedInstanceState?.getBoolean(WAS_LOADED_KEY),
            savedInstanceState?.getString(ALBUM_IMAGE_PATH_KEY),
            savedInstanceState?.getParcelable(ALBUM_IMAGE_URI_KEY) as Uri?,
            savedInstanceState?.getString(TITLE_KEY) ?: track.title,
            savedInstanceState?.getString(ARTIST_KEY) ?: track.artist,
            savedInstanceState?.getString(ALBUM_KEY) ?: track.album,
            savedInstanceState?.getByte(TRACK_NUMBER_IN_ALBUM_KEY) ?: track.trackNumberInAlbum,
            savedInstanceState?.getSerializable(TRACK_LIST_KEY) as Array<Song>?
        )

        binding = DataBindingUtil.inflate<FragmentChangeTrackInfoBinding>(
            inflater,
            R.layout.fragment_change_track_info,
            container,
            false
        ).apply {
            viewModel = TrackItemViewModel(_pos = 0, track)
            title = this@TrackChangeFragment.viewModel.titleFlow.value
            artist = this@TrackChangeFragment.viewModel.artistFlow.value
            album = this@TrackChangeFragment.viewModel.albumFlow.value
            numberInAlbum = this@TrackChangeFragment.viewModel.trackNumberInAlbumFlow.value
        }

        setCurrentImageAsync()

        binding!!.run {
            trackTitleChangeInput.setHintTextColor(Color.GRAY)
            trackArtistChangeInput.setHintTextColor(Color.GRAY)
            trackAlbumChangeInput.setHintTextColor(Color.GRAY)
            trackPosChangeInput.setHintTextColor(Color.GRAY)

            similarTracksRecyclerView.run {
                layoutManager = LinearLayoutManager(requireContext())
                addItemDecoration(VerticalSpaceItemDecoration(30))
            }

            imagesRecyclerView.run {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                runOnUIThread { imagesAdapter.setCurrentList(listOf(ADD_IMAGE_FROM_STORAGE)) }
                adapter = imagesAdapter
                addItemDecoration(HorizontalSpaceItemDecoration(30))
            }
        }

        when {
            viewModel.wasLoadedFlow.value -> initRecyclerViews()

            else -> {
                viewModel.wasLoadedFlow.value = true
                runOnUIThread { updateUIAsync(track.artist to track.title, isLocking = true) }
            }
        }

        binding!!.imagesRecyclerView.scrollToPosition(0)
        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(WAS_LOADED_KEY, viewModel.wasLoadedFlow.value)
        outState.putString(ALBUM_IMAGE_PATH_KEY, viewModel.albumImageUrlFlow.value)
        outState.putParcelable(ALBUM_IMAGE_URI_KEY, viewModel.albumImagePathFlow.value)
        outState.putString(TITLE_KEY, binding?.trackTitleChangeInput?.text.toString())
        outState.putString(ARTIST_KEY, binding?.trackArtistChangeInput?.text.toString())
        outState.putString(ALBUM_KEY, binding?.trackAlbumChangeInput?.text.toString())
        outState.putSerializable(TRACK_LIST_KEY, viewModel.trackListFlow.value.toTypedArray())
        outState.putByte(
            TRACK_NUMBER_IN_ALBUM_KEY,
            binding?.trackPosChangeInput?.text?.toString()?.toByteOrNull() ?: -1
        )
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        freeUI()
    }

    override fun onResume() {
        super.onResume()
        setCurrentImageAsync()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        freeUI()

        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_change_track, menu)

        fragmentActivity.run {
            runOnWorkerThread {
                while (!isMainLabelInitialized)
                    awaitMainLabelInitCondition.block()

                launch(Dispatchers.Main) {
                    mainLabelCurText = this@TrackChangeFragment.mainLabelCurText
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accept_change -> runOnIOThread { updateAndSaveTrack() }

            R.id.update_change -> runOnUIThread {
                val drawableWrapper = AnimationDrawableWrapper(
                    requireActivity().resources,
                    item.icon
                )

                item.icon = drawableWrapper

                val animator = ObjectAnimator.ofInt(0, 360).apply {
                    addUpdateListener { drawableWrapper.setRotation(it) }
                    start()
                }

                updateUIAsync(isLocking = true)
                animator.cancel()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Rise fragment if playing bar is active.
     * It handles GUI error when playing bar was hiding some content
     */

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.trackChangeView.layoutParams =
                (binding!!.trackChangeView.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    /** Refreshes UI without any synchronization */
    override suspend fun updateUIAsyncNoLock(src: Pair<String, String>) = coroutineScope {
        runOnIOThread {
            val cnt = AtomicInteger(1)
            val condition = ConditionVariable()
            val tasks = mutableListOf<LiveData<SongsResponse>>()

            launch(Dispatchers.Main) {
                geniusFetcher
                    .fetchTrackDataSearch("${src.first} ${src.second}")
                    .observe(viewLifecycleOwner) { searchResponse ->
                        runOnWorkerThread {
                            when (searchResponse.meta.status) {
                                !in 200 until 300 -> cnt.set(0)

                                else -> {
                                    cnt.set(searchResponse.response.hits.size)

                                    searchResponse.response.hits
                                        .takeIf(Array<*>::isNotEmpty)
                                        ?.forEach { data ->
                                            tasks.add(
                                                geniusFetcher
                                                    .fetchTrackInfoSearch(data.result.id)
                                            )

                                            if (cnt.decrementAndGet() == 0)
                                                condition.open()
                                        }
                                        ?: condition.open()
                                }
                            }
                        }
                    }
            }.join()

            while (cnt.get() > 0)
                condition.block()

            launch(Dispatchers.Main) {
                val trackList = mutableListOf<Song>()
                val condition2 = ConditionVariable()
                val cnt2 = AtomicInteger(tasks.size)

                tasks.takeIf(List<*>::isNotEmpty)?.forEach { liveData ->
                    val isObservingStarted = AtomicBoolean()
                    val itemCondition = ConditionVariable()

                    liveData.observe(viewLifecycleOwner) { songResponse ->
                        isObservingStarted.set(true)
                        itemCondition.open()

                        songResponse
                            .takeIf { it.meta.status in 200 until 300 }
                            ?.let { trackList.add(it.response.song) }

                        if (cnt2.decrementAndGet() == 0)
                            condition2.open()
                    }

                    launch(Dispatchers.IO) {
                        if (!isObservingStarted.get())
                            itemCondition.block(5000)

                        if (!isObservingStarted.get()) {
                            if (cnt2.decrementAndGet() == 0)
                                condition2.open()
                        }
                    }

                } ?: run {
                    cnt2.set(0)
                    condition2.open()
                }

                launch(Dispatchers.IO) {
                    while (cnt2.get() > 0)
                        condition2.block()

                    launch(Dispatchers.Main) {
                        viewModel.trackListFlow.value = trackList
                        initRecyclerViews()
                    }
                }.join()
            }
        }.join()
    }

    /**
     * Changes album image source
     * @param image Uri of image
     */

    override fun setUserImage(image: Uri) {
        viewModel.albumImageUrlFlow.value = null
        viewModel.albumImagePathFlow.value = image

        Glide.with(this)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding!!.currentImage.width, binding!!.currentImage.height)
            .into(binding!!.currentImage)
    }

    private suspend fun updateUIAsync(isLocking: Boolean) = updateUIAsync(
        binding!!.trackArtistChangeInput.text.toString() to
                binding!!.trackTitleChangeInput.text.toString(),
        isLocking
    )

    /** Initialises (or reinitialises) recycler views */

    private fun initRecyclerViews() = runOnUIThread {
        imagesAdapter.setCurrentList(
            viewModel.trackListFlow.value
                .flatMap {
                    listOfNotNull(
                        it.headerImageUrl,
                        it.songArtImageUrl,
                        it.album?.coverArtUrl,
                        it.primaryArtist.imageUrl,
                    ) + it.featuredArtists.map(Artist::imageUrl)
                }
                .distinct()
                .toMutableList()
                .apply { add(ADD_IMAGE_FROM_STORAGE) }
        )

        tracksAdapter.setCurrentList(viewModel.trackListFlow.value)

        binding!!.run {
            similarTracksRecyclerView.adapter = tracksAdapter
            similarTracksRecyclerView.scrollToPosition(0)

            imagesRecyclerView.adapter = imagesAdapter
            imagesRecyclerView.scrollToPosition(0)

            emptySimilarTracks.visibility = when {
                this@TrackChangeFragment.viewModel.trackListFlow.value.isEmpty() ->
                    carbon.widget.TextView.VISIBLE
                else -> carbon.widget.TextView.INVISIBLE
            }
        }
    }

    /** Updates tags and all databases */
    private suspend fun updateAndSaveTrack() = coroutineScope {
        var isUpdated = false

        val path = track.path
        val newTitle = binding!!.trackTitleChangeInput.text.toString()
        val newArtist = binding!!.trackArtistChangeInput.text.toString()
        val newAlbum = binding!!.trackAlbumChangeInput.text.toString()
        val newNumberInAlbum = binding!!.trackPosChangeInput.text.toString().toByteOrNull()
            ?.let { if (it < -1) -1 else it } ?: -1

        val newTrack = DefaultTrack(
            track.androidId,
            newTitle,
            newArtist,
            newAlbum,
            path,
            track.duration,
            track.relativePath,
            track.displayName,
            track.addDate,
            newNumberInAlbum
        )

        FavouriteRepository
            .getInstanceSynchronized()
            .updateTrackAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        CustomPlaylistsRepository
            .getInstanceSynchronized()
            .updateTrackAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        StatisticsRepository
            .getInstanceSynchronized()
            .updateTrackAsync(path, newTitle, newArtist, newAlbum, newNumberInAlbum)

        application.curPlaylist.run {
            replace(track, newTrack)
            launch(Dispatchers.IO) {
                StorageUtil
                    .getInstanceSynchronized()
                    .storeCurPlaylistLocking(this@run)
            }
        }

        launch(Dispatchers.IO) {
            val content = ContentValues().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                put(MediaStore.Audio.Media.TITLE, newTrack.title)
                put(MediaStore.Audio.Media.ARTIST, newTrack.artist)
                put(MediaStore.Audio.Media.ALBUM, newTrack.album)
                put(MediaStore.Audio.Media.TRACK, newTrack.trackNumberInAlbum)
            }

            try {
                fragmentActivity.contentResolver.update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    content,
                    "${MediaStore.Audio.Media.DATA} = ?",
                    arrayOf(newTrack.path)
                )
            } catch (ignored: Exception) {
                // Android 10+ Error
            }

            try {
                awaitDialog = async(Dispatchers.Main) {
                    createAndShowAwaitDialog(
                        context = requireContext(),
                        isCancelable = false
                    )
                }

                val wasPlaying = try { isPlaying ?: false } catch (e: Exception) { false }

                val resumeTime = try {
                    application.musicPlayer?.currentPosition
                        ?: StorageUtil.getInstanceSynchronized().loadTrackPauseTimeLocking()
                } catch (e: Exception) {
                    StorageUtil.getInstanceSynchronized().loadTrackPauseTimeLocking()
                }

                if (wasPlaying && getCurPath() == track.path)
                    fragmentActivity.pausePlaying(isLocking = true, isUiUpdating = true)

                isUpdated = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        updateTrackFileTagsAsync(content).await()
                    else -> run {
                        val task = ImageRepository
                            .getInstanceSynchronized()
                            .removeTrackWithImageAsync(path)

                        val bitmapTarget = object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                try {
                                    val stream = ByteArrayOutputStream()
                                    resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val byteArray = stream.toByteArray()

                                    runOnIOThread {
                                        task.join()

                                        ImageRepository
                                            .getInstanceSynchronized()
                                            .addTrackWithImageAsync(TrackImage(path, byteArray))
                                    }
                                } catch (e: Exception) {
                                    runOnUIThread {
                                        MessageDialog(R.string.image_not_supported)
                                            .show(parentFragmentManager, null)
                                    }
                                }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) = Unit
                        }

                        viewModel.albumImageUrlFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        } ?: viewModel.albumImagePathFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        }

                        true
                    }
                }

                if (wasPlaying && getCurPath() == track.path)
                    fragmentActivity.restartPlayingAfterTrackChangedLocked(resumeTime)

                runOnUIThread { awaitDialog?.await()?.dismiss() }
                application.scanSingleFile(track.path)
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
                        .let {
                            startIntentSenderForResult(
                                it, 125,
                                null, 0, 0, 0, null
                            )
                        }
                }
            }

            launch(Dispatchers.Main) {
                if (getCurPath() == newTrack.path)
                    fragmentActivity.updateUIAsync(track.toOption() to newTrack, isLocking = true)
            }
        }.join()

        updateStatisticsAsync()

        if (isUpdated) {
            requireActivity().sendBroadcast(Intent(MainActivity.Broadcast_UPDATE_NOTIFICATION))
            fragmentActivity.supportFragmentManager.popBackStack()
        }
    }

    /**
     * Updates columns in MediaStore
     * @param content new columns to set
     */

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTrackFileTagsAsync(content: ContentValues): Deferred<Boolean> {
        val resolver = requireActivity().contentResolver

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            resolver.update(
                uri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }, null, null
            )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            resolver.update(
                uri,
                content,
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(track.androidId.toString())
            )

        val upd = {
            try {
                AudioFileIO.read(File(track.path)).run {
                    val condition = ConditionVariable()
                    val isUpdated = AtomicBoolean()

                    tagOrCreateAndSetDefault?.let { tag ->
                        val bitmapTarget = object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                try {
                                    val stream = ByteArrayOutputStream()
                                    resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val byteArray = stream.toByteArray()
                                    tagOrCreateAndSetDefault.deleteArtworkField()

                                    tagOrCreateAndSetDefault.setField(
                                        ArtworkFactory
                                            .createArtworkFromFile(File(track.path))
                                            .apply { binaryData = byteArray }
                                    )
                                } catch (e: Exception) {
                                    runOnUIThread {
                                        MessageDialog(R.string.image_not_supported)
                                            .show(parentFragmentManager, null)
                                    }
                                }

                                isUpdated.set(true)
                                condition.open()
                            }

                            override fun onLoadCleared(placeholder: Drawable?) = Unit
                        }

                        isUpdated.set(
                            viewModel.albumImageUrlFlow.value == null &&
                                    viewModel.albumImagePathFlow.value == null
                        )

                        viewModel.albumImageUrlFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        } ?: viewModel.albumImagePathFlow.value?.let {
                            Glide.with(this@TrackChangeFragment)
                                .asBitmap()
                                .load(it)
                                .into(bitmapTarget)
                        }

                        tag.setField(
                            FieldKey.TITLE,
                            binding!!.trackTitleChangeInput.text.toString()
                        )

                        tag.setField(
                            FieldKey.ARTIST,
                            binding!!.trackArtistChangeInput.text.toString()
                        )

                        tag.setField(
                            FieldKey.ALBUM,
                            binding!!.trackAlbumChangeInput.text.toString()
                        )

                        tag.setField(
                            FieldKey.TRACK,
                            "${
                                binding!!.trackPosChangeInput.text.toString().toByteOrNull()
                                    ?.let { if (it < -1) -1 else it } ?: -1
                            }"
                        )
                    }

                    while (!isUpdated.get())
                        condition.block()
                    commit()
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()

                runOnUIThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.failure)
                        .setMessage(e.message ?: resources.getString(R.string.unknown_error))
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            dialog.dismiss()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                        .setCancelable(false)
                        .show()
                }

                false
            }
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                getFromUIThreadAsync {
                    application
                        .checkAndRequestManageExternalStoragePermission {
                            getFromIOThreadAsync { upd() }.await()
                        }
                        .unwrapOr(false)
                }

            else -> getFromIOThreadAsync { upd() }
        }
    }

    /** Sets current image by url, or path, or track's cover */
    private fun setCurrentImageAsync() = runOnUIThread {
        val width = binding!!.currentImage.width
        val height = binding!!.currentImage.height

        Glide.with(this@TrackChangeFragment)
            .load(
                viewModel.albumImageUrlFlow.value
                    ?: viewModel.albumImagePathFlow.value
                    ?: application
                        .getAlbumPictureAsync(track.path)
                        .await()
            )
            .placeholder(R.drawable.album_default)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(width, height)
            .fallback(R.drawable.album_default)
            .error(R.drawable.album_default)
            .into(binding!!.currentImage)
    }

    private fun freeUI() {
        binding?.currentImage?.let(Glide.with(this)::clear)

        Glide.get(requireContext()).run {
            runOnIOThread { clearDiskCache() }
            bitmapPool.clearMemory()
            clearMemory()
        }
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (tracks) */
    inner class TrackAdapter : AsyncListDifferAdapter<Song, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(first: Song, second: Song) = first == second

        /** [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter] */
        inner class TrackHolder(private val trackBinding: ListItemSongBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Song

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                runOnUIThread {
                    awaitDialog = async(Dispatchers.Main) {
                        createAndShowAwaitDialog(requireContext(), isCancelable = false)
                    }

                    (callbacker as Callbacks?)?.onTrackSelected(
                        track,
                        binding!!.trackTitleChangeInput,
                        binding!!.trackArtistChangeInput,
                        binding!!.trackAlbumChangeInput,
                        binding!!.trackPosChangeInput
                    )

                    val curImage = binding!!.currentImage
                    viewModel.albumImageUrlFlow.value = track.songArtImageUrl
                    viewModel.albumImagePathFlow.value = null

                    Glide.with(this@TrackChangeFragment)
                        .asDrawable()
                        .load(track.songArtImageUrl)
                        .placeholder(R.drawable.album_default)
                        .error(R.drawable.album_default)
                        .fallback(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .override(curImage.width, curImage.height)
                        .into(curImage)

                    awaitDialog?.await()?.dismiss()
                }
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            internal fun bind(_track: Song) {
                track = _track.apply {
                    album
                        ?.takeIf { it.name == "null" }
                        ?.run { name = resources.getString(R.string.unknown_album) }
                }

                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(_pos = layoutPosition + 1)
                trackBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackHolder(
            ListItemSongBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: TrackHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (images) */
    inner class ImageAdapter : AsyncListDifferAdapter<String, ImageAdapter.ImageHolder>() {
        override fun areItemsEqual(first: String, second: String) = first == second

        /** ViewHolder for tracks of [TrackAdapter] */
        inner class ImageHolder(private val imageBinding: ListItemImageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(imageBinding.root),
            View.OnClickListener {
            private lateinit var image: String

            init {
                itemView.setOnClickListener(this)
                imageBinding.viewModel = ArtistListViewModel()
            }

            override fun onClick(v: View?) {
                viewModel.albumImageUrlFlow.value = image
                viewModel.albumImagePathFlow.value = null

                when (image) {
                    ADD_IMAGE_FROM_STORAGE -> requireActivity().startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        ), ChangeImageFragment.PICK_IMAGE
                    )

                    else -> (callbacker as Callbacks?)?.onImageSelected(
                        image = (imageBinding.imageItem.drawable.current as BitmapDrawable).bitmap,
                        albumImageView = binding!!.currentImage
                    )
                }
            }

            /**
             * Constructs GUI for image item
             * @param _image track to bind and use
             */

            internal fun bind(_image: String) {
                runOnUIThread {
                    image = _image
                    Glide.with(this@TrackChangeFragment)
                        .run {
                            when (_image) {
                                ADD_IMAGE_FROM_STORAGE -> load(
                                    when {
                                        Params.getInstanceSynchronized().themeColor.second != -1 ->
                                            when (Params.getInstanceSynchronized().themeColor.second) {
                                                0 -> R.drawable.image_icon_day
                                                else -> R.drawable.image_icon_night
                                            }

                                        Params.getInstanceSynchronized().theme.isNight ->
                                            R.drawable.image_icon_night

                                        else -> R.drawable.image_icon_day
                                    }
                                )
                                else -> load(_image)
                            }
                        }
                        .placeholder(R.drawable.album_default)
                        .skipMemoryCache(true)
                        .override(imageBinding.imageItem.width, imageBinding.imageItem.height)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                runOnUIThread {
                                    val ind = imagesAdapter.currentList.indexOf(_image)
                                    imagesAdapter.setCurrentList(imagesAdapter.currentList - _image)
                                    imagesAdapter.notifyItemChanged(ind)
                                }

                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ) = false

                        })
                        .into(imageBinding.imageItem)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageHolder(
            ListItemImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: ImageHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}