package com.dinaraparanid.prima.fragments

import android.animation.ObjectAnimator
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
import android.provider.MediaStore
import android.view.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.core.DefaultTrack
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentChangeTrackInfoBinding
import com.dinaraparanid.prima.databinding.ListItemImageBinding
import com.dinaraparanid.prima.databinding.ListItemSongBinding
import com.dinaraparanid.prima.utils.AnimationDrawableWrapper
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toByteArray
import com.dinaraparanid.prima.utils.extensions.unwrapOr
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.dinaraparanid.prima.utils.web.genius.GeniusFetcher
import com.dinaraparanid.prima.utils.web.genius.songs_response.Song
import com.dinaraparanid.prima.utils.web.genius.songs_response.SongsResponse
import com.dinaraparanid.prima.viewmodels.androidx.TrackChangeViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.ArtistListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Fragment to change track's metadata.
 * @since Android 11 it only changes entities in app,
 * but not metadata of track itself
 */

class TrackChangeFragment :
    CallbacksFragment<FragmentChangeTrackInfoBinding, MainActivity>(),
    Rising,
    UIUpdatable<Pair<String, String>>,
    ChangeImageFragment,
    MainActivityFragment,
    AsyncContext {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Makes selected image new track's album image
         * @param image image to select
         * @param albumImage album image view which image should be replaced
         */

        fun onImageSelected(image: Bitmap, albumImage: ImageView)

        /**
         * Makes changeable track's metadata
         * equal to selected found track's metadata
         * @param selectedTrack track to select
         * @param titleInput [EditText] for title
         * @param artistInput [EditText] for artist
         * @param albumInput [EditText] for album image
         */

        fun onTrackSelected(
            selectedTrack: Song,
            titleInput: EditText,
            artistInput: EditText,
            albumInput: EditText
        )
    }

    override val mutex = Mutex()
    override var isMainLabelInitialized = false
    override val awaitMainLabelInitLock: Lock = ReentrantLock()
    override val awaitMainLabelInitCondition: Condition = awaitMainLabelInitLock.newCondition()

    override lateinit var mainLabelOldText: String
    override lateinit var mainLabelCurText: String

    override var binding: FragmentChangeTrackInfoBinding? = null

    override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    private lateinit var track: AbstractTrack

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

    private val geniusFetcher: GeniusFetcher by lazy { GeniusFetcher() }

    private inline val rotationAnimation
        get() = RotateAnimation(
            0F, 360F,
            Animation.RELATIVE_TO_SELF, 0.5F,
            Animation.RELATIVE_TO_SELF, 0.5F
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
        }

    internal companion object {
        private const val ALBUM_IMAGE_PATH_KEY = "album_image_path"
        private const val ALBUM_IMAGE_URI_KEY = "album_image_uri"
        private const val TITLE_KEY = "title"
        private const val ARTIST_KEY = "artist"
        private const val ALBUM_KEY = "album"
        private const val TRACK_LIST_KEY = "track_list"
        private const val WAS_LOADED_KEY = "was_loaded"
        private const val TRACK_KEY = "track"
        private const val ADD_IMAGE_FROM_STORAGE = "add_image_from_storage"

        /**
         * Creates new instance of fragment with params
         * @param mainLabelOldText old main label text (to return)
         * @param mainLabelCurText main label text for current fragment
         * @param track track to change
         * @return new instance of fragment with params in bundle
         */

        @JvmStatic
        internal fun newInstance(
            mainLabelOldText: String,
            mainLabelCurText: String,
            track: AbstractTrack,
        ) = TrackChangeFragment().apply {
            arguments = Bundle().apply {
                putSerializable(TRACK_KEY, track)
                putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
                putString(MAIN_LABEL_CUR_TEXT_KEY, mainLabelCurText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        track = requireArguments().getSerializable(TRACK_KEY) as AbstractTrack
        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)!!
        mainLabelCurText = requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY)!!

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
            savedInstanceState?.getString(ALBUM_KEY) ?: track.playlist,
            savedInstanceState?.getSerializable(TRACK_LIST_KEY) as Array<Song>?
        )

        binding = DataBindingUtil.inflate<FragmentChangeTrackInfoBinding>(
            inflater,
            R.layout.fragment_change_track_info,
            container,
            false
        ).apply {
            viewModel = TrackItemViewModel(0, track)
            title = this@TrackChangeFragment.viewModel.titleFlow.value
            artist = this@TrackChangeFragment.viewModel.artistFlow.value
            album = this@TrackChangeFragment.viewModel.albumFlow.value
        }

        runOnUIThread {
            val width = binding!!.currentImage.width
            val height = binding!!.currentImage.height

            Glide.with(this@TrackChangeFragment)
                .load(viewModel.albumImagePathFlow.value
                    ?: viewModel.albumImageUriFlow.value
                    ?: application
                        .getAlbumPictureAsync(track.path)
                        .await()
                )
                .placeholder(R.drawable.album_default)
                .skipMemoryCache(true)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(width, height)
                .into(binding!!.currentImage)
        }

        binding!!.run {
            trackTitleChangeInput.setHintTextColor(Color.GRAY)
            trackArtistChangeInput.setHintTextColor(Color.GRAY)
            trackAlbumChangeInput.setHintTextColor(Color.GRAY)

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
                runOnUIThread { updateUI(track.artist to track.title, isLocking = true) }
            }
        }

        binding!!.imagesRecyclerView.scrollToPosition(0)
        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(WAS_LOADED_KEY, viewModel.wasLoadedFlow.value)
        outState.putString(ALBUM_IMAGE_PATH_KEY, viewModel.albumImagePathFlow.value)
        outState.putParcelable(ALBUM_IMAGE_URI_KEY, viewModel.albumImageUriFlow.value)
        outState.putString(TITLE_KEY, binding?.trackTitleChangeInput?.text.toString())
        outState.putString(ARTIST_KEY, binding?.trackArtistChangeInput?.text.toString())
        outState.putString(ALBUM_KEY, binding?.trackAlbumChangeInput?.text.toString())
        outState.putSerializable(TRACK_LIST_KEY, viewModel.trackListFlow.value.toTypedArray())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_change_track, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.accept_change -> runOnIOThread { updateAndSaveTrack() }

            R.id.update_change -> runOnUIThread {
                val drawableWrapper = AnimationDrawableWrapper(requireActivity().resources, item.icon)
                item.icon = drawableWrapper

                val animator = ObjectAnimator.ofInt(0, 360).apply {
                    addUpdateListener { drawableWrapper.setRotation(it) }
                    start()
                }

                updateUI(isLocking = true)
                animator.cancel()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun up() {
        if (!fragmentActivity.isUpped)
            binding!!.trackChangeView.layoutParams =
                (binding!!.trackChangeView.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = Params.PLAYING_TOOLBAR_HEIGHT
                }
    }

    override suspend fun updateUINoLock(src: Pair<String, String>) = coroutineScope {
        runOnIOThread {
            val cnt = AtomicInteger(1)
            val lock = ReentrantLock()
            val condition = lock.newCondition()
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
                                                lock.withLock(condition::signal)
                                        }
                                        ?: lock.withLock(condition::signal)
                                }
                            }
                        }
                    }
            }.join()

            lock.withLock {
                while (cnt.get() > 0)
                    condition.await()

                launch(Dispatchers.Main) {
                    val trackList = mutableListOf<Song>()
                    val lock2 = ReentrantLock()
                    val condition2 = lock2.newCondition()
                    val cnt2 = AtomicInteger(tasks.size)

                    tasks.takeIf(List<*>::isNotEmpty)?.forEach { liveData ->
                        val isObservingStarted = AtomicBoolean()
                        val itemLock = ReentrantLock()
                        val itemCondition = itemLock.newCondition()

                        liveData.observe(viewLifecycleOwner) { songResponse ->
                            isObservingStarted.set(true)
                            itemLock.withLock(itemCondition::signal)

                            songResponse
                                .takeIf { it.meta.status in 200 until 300 }
                                ?.let { trackList.add(it.response.song) }

                            if (cnt2.decrementAndGet() == 0)
                                lock2.withLock(condition2::signal)
                        }

                        launch(Dispatchers.IO) {
                            itemLock.withLock {
                                if (!isObservingStarted.get())
                                    itemCondition.await(5, TimeUnit.SECONDS)

                                if (!isObservingStarted.get()) {
                                    if (cnt2.decrementAndGet() == 0)
                                        lock2.withLock(condition2::signal)
                                }
                            }
                        }

                    } ?: run {
                        cnt2.set(0)
                        lock2.withLock(condition2::signal)
                    }

                    launch(Dispatchers.IO) {
                        lock2.withLock {
                            while (cnt2.get() > 0)
                                condition2.await()

                            Exception("KEK").printStackTrace()

                            launch(Dispatchers.Main) {
                                viewModel.trackListFlow.value = trackList
                                initRecyclerViews()
                            }
                        }
                    }.join()
                }
            }
        }.join()
    }

    /**
     * Changes album image source
     * @param image Uri of image
     */

    override fun setUserImage(image: Uri) {
        viewModel.albumImagePathFlow.value = null
        viewModel.albumImageUriFlow.value = image

        Glide.with(this)
            .load(image)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(binding!!.currentImage.width, binding!!.currentImage.height)
            .into(binding!!.currentImage)
    }

    private suspend fun updateUI(isLocking: Boolean) = updateUI(
        binding!!.trackArtistChangeInput.text.toString() to
                binding!!.trackTitleChangeInput.text.toString(),
        isLocking
    )

    /**
     * Initialises (or reinitialises) recycler views
     */

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

    private suspend fun updateAndSaveTrack() = coroutineScope {
        var isUpdated = false

        val path = track.path
        val newTitle = binding!!.trackTitleChangeInput.text.toString()
        val newArtist = binding!!.trackArtistChangeInput.text.toString()
        val newAlbum = binding!!.trackAlbumChangeInput.text.toString()

        val newTrack = DefaultTrack(
            track.androidId,
            newTitle,
            newArtist,
            newAlbum,
            path,
            track.duration,
            track.relativePath,
            track.displayName,
            track.addDate
        )

        FavouriteRepository.instance.updateTrackAsync(path, newTitle, newArtist, newAlbum)
        CustomPlaylistsRepository.instance.updateTrackAsync(path, newTitle, newArtist, newAlbum)

        application.curPlaylist.run {
            replace(track, newTrack)
            launch(Dispatchers.IO) {
                StorageUtil.getInstanceSynchronized().storeCurPlaylist(this@run)
            }
        }

        val mediaStoreTask = launch(Dispatchers.IO) {
            val lock = ReentrantLock()
            val condition = lock.newCondition()
            var imageTask: Job? = null

            val bitmapTarget = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    val trackImage = TrackImage(newTrack.path, resource.toByteArray())

                    imageTask = runOnIOThread {
                        val rep = ImageRepository.instance
                        rep.removeTrackWithImageAsync(newTrack.path).join()

                        try {
                            rep.addTrackWithImageAsync(trackImage).join()
                            lock.withLock(condition::signal)
                        } catch (e: Exception) {
                            rep.removeTrackWithImageAsync(newTrack.path)

                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            }

            val willImageUpdate = viewModel.albumImagePathFlow.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .asBitmap()
                    .load(it)
                    .into(bitmapTarget)
                true
            } ?: viewModel.albumImageUriFlow.value?.let {
                Glide.with(this@TrackChangeFragment)
                    .asBitmap()
                    .load(it)
                    .into(bitmapTarget)
                true
            } ?: false

            val content = ContentValues().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    put(MediaStore.Audio.Media.IS_PENDING, 0)

                put(MediaStore.Audio.Media.TITLE, newTrack.title)
                put(MediaStore.Audio.Media.ARTIST, newTrack.artist)
                put(MediaStore.Audio.Media.ALBUM, newTrack.playlist)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // It's properly works only on second time...
                    updateMediaStoreQAsync(content).join()
                    isUpdated = updateMediaStoreQAsync(content).await()
                } catch (securityException: SecurityException) {
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
            } else {
                isUpdated = true

                fragmentActivity.contentResolver.update(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    content,
                    "${MediaStore.Audio.Media.DATA} = ?",
                    arrayOf(newTrack.path)
                )
            }

            launch(Dispatchers.IO) {
                if (application.curPath == newTrack.path)
                    lock.withLock {
                        while (imageTask == null && willImageUpdate)
                            condition.await()

                        launch(Dispatchers.Main) {
                            fragmentActivity.updateUI(newTrack to false, isLocking = true)
                        }
                    }
            }.join()
        }

        mediaStoreTask.join()

        if (isUpdated) {
            requireActivity().sendBroadcast(Intent(MainActivity.Broadcast_UPDATE_NOTIFICATION))
            fragmentActivity.supportFragmentManager.popBackStack()
        }
    }

    /**
     * Updates columns in MediaStore for Android Api 29+
     * @param content new columns to set
     */

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateMediaStoreQAsync(content: ContentValues): Deferred<Boolean> {
        val act = requireActivity()
        val resolver = act.contentResolver

        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            track.androidId
        )

        resolver.update(
            uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }, null, null
        )

        resolver.update(
            uri,
            content,
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(track.androidId.toString())
        )

        val upd = {
            try {
                AudioFileIO.read(File(track.path)).run {
                    tag.run {
                        setField(
                            FieldKey.TITLE,
                            binding!!.trackTitleChangeInput.text.toString()
                        )

                        setField(
                            FieldKey.ARTIST,
                            binding!!.trackArtistChangeInput.text.toString()
                        )

                        setField(
                            FieldKey.ALBUM,
                            binding!!.trackAlbumChangeInput.text.toString()
                        )
                    }

                    commit()
                }

                true
            } catch (e: Exception) {
                false
            }
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                getFromUIThreadAsync {
                    application
                        .checkAndRequestManageExternalStoragePermission { upd() }
                        .unwrapOr(false)
                }

            else -> getFromIOThreadAsync { upd() }
        }
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (tracks) */

    inner class TrackAdapter : AsyncListDifferAdapter<Song, TrackAdapter.TrackHolder>() {
        override fun areItemsEqual(first: Song, second: Song) = first == second
        override val self: AsyncListDifferAdapter<Song, TrackHolder> get() = this

        /** [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter] */

        inner class TrackHolder(private val trackBinding: ListItemSongBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Song

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(
                    track,
                    binding!!.trackTitleChangeInput,
                    binding!!.trackArtistChangeInput,
                    binding!!.trackAlbumChangeInput
                )
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: Song) {
                track = _track.apply {
                    album
                        ?.takeIf { it.name == "null" }
                        ?.run { name = resources.getString(R.string.unknown_album) }
                }

                trackBinding.track = _track
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
                trackBinding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemSongBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit =
            holder.bind(differ.currentList[position])
    }

    /** [AsyncListDifferAdapter] for [TrackChangeFragment] (images) */

    inner class ImageAdapter : AsyncListDifferAdapter<String, ImageAdapter.ImageHolder>() {
        override fun areItemsEqual(first: String, second: String) = first == second
        override val self: AsyncListDifferAdapter<String, ImageHolder> get() = this

        /**
         * [androidx.recyclerview.widget.RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class ImageHolder(private val imageBinding: ListItemImageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(imageBinding.root),
            View.OnClickListener {
            private lateinit var image: String

            init {
                itemView.setOnClickListener(this)
                imageBinding.viewModel = ArtistListViewModel()
            }

            override fun onClick(v: View?) {
                viewModel.albumImagePathFlow.value = image
                viewModel.albumImageUriFlow.value = null

                when (image) {
                    ADD_IMAGE_FROM_STORAGE -> requireActivity().startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        ), ChangeImageFragment.PICK_IMAGE
                    )


                    else -> (callbacker as Callbacks?)?.onImageSelected(
                        (imageBinding.imageItem.drawable.current as BitmapDrawable).bitmap,
                        binding!!.currentImage
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder =
            ImageHolder(
                ListItemImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: ImageHolder, position: Int): Unit =
            holder.bind(differ.currentList[position])
    }
}