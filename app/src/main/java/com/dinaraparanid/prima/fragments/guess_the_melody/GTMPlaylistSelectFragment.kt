package com.dinaraparanid.prima.fragments.guess_the_melody

import android.os.Bundle
import android.os.ConditionVariable
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databases.repositories.ImageRepository
import com.dinaraparanid.prima.databinding.FragmentSelectPlaylistBinding
import com.dinaraparanid.prima.databinding.ListItemGtmSelectPlaylistBinding
import com.dinaraparanid.prima.fragments.track_collections.PlaylistSelectFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.extensions.toBitmap
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.FilterFragment
import com.dinaraparanid.prima.viewmodels.androidx.DefaultViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/**
 * Fragment which chooses playlists
 * to start "Guess the Melody" game
 */

class GTMPlaylistSelectFragment : MainActivityUpdatingListFragment<
        AbstractPlaylist,
        GTMPlaylistSelectFragment.PlaylistAdapter,
        GTMPlaylistSelectFragment.PlaylistAdapter.PlaylistHolder,
        FragmentSelectPlaylistBinding>(),
    FilterFragment<AbstractPlaylist> {
    internal interface Callbacks : CallbacksFragment.Callbacks {
        fun onPlaylistSelected(playlist: AbstractPlaylist, fragment: GTMPlaylistSelectFragment)
    }

    @Volatile
    private var isAdapterInit = false
    private val awaitAdapterInitCondition = ConditionVariable()

    private var awaitDialog: Deferred<KProgressHUD>? = null
    override var binding: FragmentSelectPlaylistBinding? = null
    override var emptyTextView: TextView? = null
    override var updater: SwipeRefreshLayout? = null
    override var _adapter: PlaylistAdapter? = null

    override val viewModel by lazy {
        ViewModelProvider(this)[DefaultViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mainLabelCurText = resources.getString(R.string.playlists)
        setMainLabelInitialized()
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        runOnIOThread {
            val task = loadAsync()

            awaitDialog = async(Dispatchers.Main) {
                createAndShowAwaitDialog(requireContext(), false)
            }

            task.join()

            launch(Dispatchers.Main) {
                awaitDialog?.await()?.dismiss()
                setEmptyTextViewVisibility(itemList)
                initAdapter()
                adapter.setCurrentList(itemList)
            }

            itemListSearch.addAll(itemList)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate<FragmentSelectPlaylistBinding>(
            inflater,
            R.layout.fragment_select_playlist,
            container,
            false
        ).apply {
            viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()

            updater = selectPlaylistSwipeRefreshLayout.apply {
                setColorSchemeColors(Params.instance.primaryColor)
                setOnRefreshListener {
                    runOnIOThread {
                        val task = loadAsync()
                        awaitDialog = async(Dispatchers.Main) {
                            createAndShowAwaitDialog(requireContext(), false)
                        }

                        task.join()

                        launch(Dispatchers.Main) {
                            awaitDialog?.await()?.dismiss()
                            updateUIAsync(isLocking = true)
                            isRefreshing = false
                        }
                    }
                }
            }

            emptyTextView = selectPlaylistEmpty
            setEmptyTextViewVisibility(itemList)

            runOnIOThread {
                recyclerView = selectPlaylistRecyclerView.apply {
                    while (!isAdapterInit)
                        awaitAdapterInitCondition.block()

                    launch(Dispatchers.Main) {
                        layoutManager = LinearLayoutManager(context)
                        adapter = this@GTMPlaylistSelectFragment.adapter
                        addItemDecoration(VerticalSpaceItemDecoration(30))
                    }
                }
            }
        }

        if (application.playingBarIsVisible) up()
        return binding!!.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_search, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        runOnUIThread {
            awaitDialog?.await()?.dismiss()
            awaitDialog = null
        }
    }

    override fun filter(models: Collection<AbstractPlaylist>?, query: String) =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()

            // New playlist

            itemList.add(
                DefaultPlaylist(
                    resources.getString(R.string.create_playlist),
                    AbstractPlaylist.PlaylistType.GTM
                )
            )

            // Albums

            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Albums.ALBUM),
                    null,
                    null,
                    MediaStore.Audio.Media.ALBUM + " ASC"
                ).use { cursor ->
                    if (cursor != null) {
                        val playlistList = mutableListOf<AbstractPlaylist>()

                        while (cursor.moveToNext()) {
                            val albumTitle = cursor.getString(0)

                            application.allTracksWithoutHidden
                                .firstOrNull { it.album == albumTitle }
                                ?.let { playlistList.add(DefaultPlaylist(albumTitle)) }
                        }

                        itemList.addAll(playlistList.distinctBy(AbstractPlaylist::title))
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }

            // User's playlists

            if (application.checkAndRequestPermissions())
                itemList.addAll(
                    CustomPlaylistsRepository
                        .getInstanceSynchronized()
                        .getPlaylistsAsync()
                        .await()
                        .map { CustomPlaylist(it) }
                )

            itemList.run {
                val distinctedList = distinctBy { "${it.title}${it.type.name}" }
                clear()
                addAll(distinctedList)
            }
        }
    }

    override suspend fun updateUIAsyncNoLock(src: List<AbstractPlaylist>) {
        adapter.setCurrentList(src)
        recyclerView!!.adapter = adapter
        setEmptyTextViewVisibility(src)
    }

    override fun initAdapter() {
        _adapter = PlaylistAdapter().apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        isAdapterInit = true
        awaitAdapterInitCondition.open()
    }

    /** [RecyclerView.Adapter] for [PlaylistSelectFragment] */

    inner class PlaylistAdapter : AsyncListDifferAdapter<AbstractPlaylist, PlaylistAdapter.PlaylistHolder>() {
        override fun areItemsEqual(
            first: AbstractPlaylist,
            second: AbstractPlaylist
        ) = first == second

        /**
         * [RecyclerView.ViewHolder] for playlists of [PlaylistAdapter]
         */

        inner class PlaylistHolder(private val playlistBinding: ListItemGtmSelectPlaylistBinding) :
            RecyclerView.ViewHolder(playlistBinding.root),
            View.OnClickListener {
            private lateinit var playlist: AbstractPlaylist

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) = (callbacker as Callbacks)
                .onPlaylistSelected(playlist, this@GTMPlaylistSelectFragment)

            private suspend fun getTrackPathFromAlbum() =
                application.getAlbumTracksAsync(playlist.title).await().first().path

            private suspend fun getTrackPathFromCustomPlaylist() =
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .getFirstTrackOfPlaylistAsync(playlist.title)
                    .await()
                    ?.path ?: Params.NO_PATH

            /**
             * Constructs GUI for playlist item
             * @param playlist playlist itself
             */

            internal fun bind(playlist: AbstractPlaylist) = playlistBinding.run {
                this@PlaylistHolder.playlist = playlist
                viewModel = com.dinaraparanid.prima.viewmodels.mvvm.ViewModel()
                this.title = playlist.title

                if (Params.instance.areCoversDisplayed)
                    runOnIOThread {
                        try {
                            val taskDB = when (playlist.type) {
                                AbstractPlaylist.PlaylistType.CUSTOM -> ImageRepository
                                    .getInstanceSynchronized()
                                    .getPlaylistWithImageAsync(playlist.title)
                                    .await()

                                AbstractPlaylist.PlaylistType.ALBUM -> ImageRepository
                                    .getInstanceSynchronized()
                                    .getAlbumWithImageAsync(playlist.title)
                                    .await()

                                AbstractPlaylist.PlaylistType.GTM -> null
                            }

                            when {
                                taskDB != null -> launch(Dispatchers.Main) {
                                    Glide.with(this@GTMPlaylistSelectFragment)
                                        .load(taskDB.image.toBitmap())
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(playlistImage.width, playlistImage.height)
                                        .into(playlistImage)
                                }

                                else -> launch(Dispatchers.Main) {
                                    Glide.with(this@GTMPlaylistSelectFragment)
                                        .run {
                                            when (playlist.type) {
                                                AbstractPlaylist.PlaylistType.GTM -> load(R.drawable.album_default)

                                                else -> load(
                                                    application
                                                        .getAlbumPictureAsync(
                                                            when (playlist.type) {
                                                                AbstractPlaylist.PlaylistType.ALBUM ->
                                                                    getTrackPathFromAlbum()

                                                                else -> getTrackPathFromCustomPlaylist()
                                                            }
                                                        )
                                                        .await()
                                                )
                                            }
                                        }
                                        .placeholder(R.drawable.album_default)
                                        .skipMemoryCache(true)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .override(
                                            playlistImage.width,
                                            playlistImage.height
                                        )
                                        .into(playlistImage)
                                }
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.image_too_big,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlaylistHolder(
            ListItemGtmSelectPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) =
            holder.bind(differ.currentList[position])
    }
}