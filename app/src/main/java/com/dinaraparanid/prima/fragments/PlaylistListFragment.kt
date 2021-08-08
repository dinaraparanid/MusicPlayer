package com.dinaraparanid.prima.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import carbon.widget.ConstraintLayout
import carbon.widget.FloatingActionButton
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.*
import com.dinaraparanid.prima.utils.decorations.HorizontalSpaceItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.dialogs.NewPlaylistDialog
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.PlaylistListViewModel
import kotlinx.coroutines.*

/**
 * [ListFragment] for all albums and user's playlists
 */

class PlaylistListFragment :
    UpdatingListFragment<Playlist, PlaylistListFragment.PlaylistAdapter.PlaylistHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        /**
         * Calls new [TypicalTrackListFragment] with playlist's (album's) tracks
         * @param id id of playlist or 0 if it's album
         * @param title title of playlist or album
         */

        fun onPlaylistSelected(
            id: Long,
            title: String
        )
    }

    override var adapter: RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>? = null

    override val viewModel: ViewModel by lazy {
        ViewModelProvider(this)[PlaylistListViewModel::class.java]
    }

    override lateinit var emptyTextView: TextView
    override lateinit var updater: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        titleDefault = resources.getString(R.string.playlists)

        updater = view
            .findViewById<SwipeRefreshLayout>(R.id.playlist_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.instance.theme.rgb)
                setOnRefreshListener {
                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        loadAsync().await()
                        updateUI()
                        isRefreshing = false
                    }
                }
            }

        viewModel.viewModelScope.launch(Dispatchers.Main) {
            loadAsync().await()
            itemListSearch.addAll(itemList)
            adapter = PlaylistAdapter(itemList)

            val constraintLayout: ConstraintLayout =
                updater.findViewById(R.id.playlist_constraint_layout)

            constraintLayout.findViewById<FloatingActionButton>(R.id.add_playlist_button).run {
                setOnClickListener {
                    NewPlaylistDialog(this@PlaylistListFragment)
                        .show(parentFragmentManager, null)
                }
            }

            emptyTextView = constraintLayout.findViewById<TextView>(R.id.playlists_empty).apply {
                typeface = (requireActivity().application as MainApplication)
                    .getFontFromName(Params.instance.font)
            }
            setEmptyTextViewVisibility(itemList)

            recyclerView = constraintLayout
                .findViewById<RecyclerView>(R.id.playlist_recycler_view)
                .apply {
                    layoutManager = when (resources.configuration.orientation) {
                        Configuration.ORIENTATION_PORTRAIT ->
                            when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                                Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                    GridLayoutManager(context, 2)

                                Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                    GridLayoutManager(context, 3)

                                else -> GridLayoutManager(context, 2)
                            }

                        else -> when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
                            Configuration.SCREENLAYOUT_SIZE_NORMAL ->
                                GridLayoutManager(context, 3)

                            Configuration.SCREENLAYOUT_SIZE_LARGE ->
                                GridLayoutManager(context, 4)

                            else -> GridLayoutManager(context, 3)
                        }
                    }

                    adapter = this@PlaylistListFragment.adapter?.apply {
                        stateRestorationPolicy =
                            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    }

                    addItemDecoration(VerticalSpaceItemDecoration(30))
                    addItemDecoration(HorizontalSpaceItemDecoration(30))
                }

            if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        }

        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onStop() {
        (requireActivity() as MainActivity).selectButton.isVisible = false
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
    }

    override fun onResume() {
        val act = requireActivity() as MainActivity

        act.selectButton.isVisible = true

        if (act.needToUpdate) {
            loadContent()
            act.needToUpdate = false
        }

        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_playlists_menu, menu)
        (menu.findItem(R.id.playlist_search).actionView as SearchView).setOnQueryTextListener(this)
    }

    override fun updateUI(src: List<Playlist>) {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            adapter = PlaylistAdapter(src).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }
            recyclerView.adapter = adapter
            setEmptyTextViewVisibility(src)
        }
    }

    private fun loadContent(): Job = viewModel.viewModelScope.launch(Dispatchers.Main) {
        loadAsync().await()
        itemListSearch.addAll(itemList)
        adapter = PlaylistAdapter(itemListSearch).apply {
            stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        updateUI()
    }

    override fun filter(models: Collection<Playlist>?, query: String): List<Playlist> =
        query.lowercase().let { lowerCase ->
            models?.filter { lowerCase in it.title.lowercase() } ?: listOf()
        }

    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            when (mainLabelCurText) {
                resources.getString(R.string.playlists) -> itemList.run {
                    val task = CustomPlaylistsRepository.instance.getPlaylistsWithTracksAsync()

                    clear()
                    addAll(
                        task
                            .await()
                            .map { (p, t) ->
                                CustomPlaylist(p).apply {
                                    t.takeIf { it.isNotEmpty() }?.let { add(t.first()) }
                                }
                            }
                    )
                    Unit
                }

                else -> try {
                    requireActivity().contentResolver.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        arrayOf(MediaStore.Audio.Albums.ALBUM),
                        null,
                        null,
                        MediaStore.Audio.Media.ALBUM + " ASC"
                    ).use { cursor ->
                        itemList.clear()

                        if (cursor != null) {
                            val playlistList = mutableListOf<Playlist>()

                            while (cursor.moveToNext()) {
                                val albumTitle = cursor.getString(0)

                                (requireActivity().application as MainApplication).allTracks
                                    .firstOrNull { it.playlist == albumTitle }
                                    ?.let { track ->
                                        playlistList.add(
                                            DefaultPlaylist(
                                                albumTitle,
                                                tracks = mutableListOf(track) // album image
                                            )
                                        )
                                    }
                            }

                            itemList.addAll(playlistList.distinctBy { it.title })
                        }
                    }
                } catch (ignored: Exception) {
                    // Permission to storage not given
                }
            }
        }
    }

    /**
     * [RecyclerView.Adapter] for [PlaylistListFragment]
     * @param playlists items of fragment
     */

    inner class PlaylistAdapter(private val playlists: List<Playlist>) :
        RecyclerView.Adapter<PlaylistAdapter.PlaylistHolder>() {
        /**
         * [RecyclerView.ViewHolder] for tracks of [PlaylistAdapter]
         */

        inner class PlaylistHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var playlist: Playlist

            private val titleTextView = itemView
                .findViewById<TextView>(R.id.playlist_title)
                .apply {
                    typeface = (requireActivity().application as MainApplication)
                        .getFontFromName(Params.instance.font)
                }

            internal val playlistImage: carbon.widget.ImageView = itemView
                .findViewById<carbon.widget.ImageView>(R.id.playlist_image)
                .apply { if (!Params.instance.isRoundingPlaylistImage) setCornerRadius(0F) }

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?): Unit = (callbacks as Callbacks).onPlaylistSelected(
                when (mainLabelCurText) {
                    resources.getString(R.string.playlists) -> runBlocking {
                        CustomPlaylistsRepository.instance
                            .getPlaylistAsync(playlist.title)
                            .await()!!
                            .id
                    }

                    else -> 0
                },
                playlist.title
            )

            /**
             * Makes all GUI customizations for a playlist
             * @param _playlist playlist to bind
             */

            fun bind(_playlist: Playlist) {
                playlist = _playlist
                titleTextView.text = playlist.title

                viewModel.viewModelScope.launch {
                    playlist.takeIf { it.size > 0 }?.run {
                        launch((Dispatchers.Main)) {
                            val task = (requireActivity().application as MainApplication)
                                .getAlbumPictureAsync(
                                    currentTrack.path,
                                    Params.instance.showPlaylistsImages
                                )

                            Glide.with(this@PlaylistListFragment)
                                .load(task.await())
                                .placeholder(R.drawable.album_default)
                                .skipMemoryCache(true)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .override(playlistImage.width, playlistImage.height)
                                .into(playlistImage)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder =
            PlaylistHolder(layoutInflater.inflate(R.layout.list_item_playlist, parent, false))

        override fun getItemCount(): Int = playlists.size

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int): Unit =
            holder.bind(playlists[position])

        override fun onViewRecycled(holder: PlaylistHolder) {
            Glide.with(this@PlaylistListFragment).clear(holder.playlistImage)
            super.onViewRecycled(holder)
        }
    }
}