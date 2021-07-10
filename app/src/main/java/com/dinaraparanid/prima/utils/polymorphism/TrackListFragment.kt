package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack
import com.dinaraparanid.prima.fragments.DefaultTrackListFragment
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.ViewSetter
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.TrackListViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class TrackListFragment :
    ListFragment<Track, TrackListFragment.TrackAdapter.TrackHolder>() {
    interface Callbacks : ListFragment.Callbacks {
        fun onTrackSelected(
            track: Track,
            tracks: Collection<Track>,
            ind: Int,
            needToPlay: Boolean = true
        )
    }

    public override var adapter: RecyclerView.Adapter<TrackAdapter.TrackHolder>? = null

    override val viewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    protected companion object {
        const val START_KEY: String = "start"
        const val HIGHLIGHTED_START_KEY: String = "highlighted_start"
        const val NO_HIGHLIGHT: String = "______ЫЫЫЫЫЫЫЫ______"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault

        if (this is DefaultTrackListFragment) {
            try {
                runBlocking {
                    genFunc?.let {
                        coroutineScope {
                            launch {
                                itemList.addAll(it())
                            }
                        }
                    } ?: loadAsync()
                }
            } catch (e: Exception) {
                // permissions not given
            }
        }

        itemListSearch.addAll(itemList)
        adapter = TrackAdapter(itemList)

        viewModel.run {
            load(savedInstanceState?.getBoolean(HIGHLIGHTED_START_KEY))

            if (!highlightedStartLiveData.value!!)
                requireArguments().getString(START_KEY)
                    ?.takeIf { it != NO_HIGHLIGHT }
                    ?.let {
                        (requireActivity().application as MainApplication).run {
                            highlightedRows.add(it)
                            highlightedRows = highlightedRows.distinct().toMutableList()
                        }

                        highlightedStartLiveData.value = true
                    }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_track_list, container, false)
        titleDefault = resources.getString(R.string.tracks)

        val updater = view
            .findViewById<SwipeRefreshLayout>(R.id.track_swipe_refresh_layout)
            .apply {
                setColorSchemeColors(Params.getInstance().theme.rgb)
                setOnRefreshListener {

                    try {
                        runBlocking {
                            genFunc?.let {
                                coroutineScope {
                                    launch {
                                        itemList.clear()
                                        itemList.addAll(it())
                                    }
                                }
                            } ?: loadAsync()
                        }
                    } catch (e: Exception) {
                        // permissions not given
                    }

                    updateContent(itemList)
                    isRefreshing = false
                }
            }

        recyclerView = updater
            .findViewById<ConstraintLayout>(R.id.track_constraint_layout)
            .findViewById<RecyclerView>(R.id.track_recycler_view).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@TrackListFragment.adapter
                addItemDecoration(VerticalSpaceItemDecoration(30))
                addItemDecoration(DividerItemDecoration(requireActivity()))
            }

        if ((requireActivity().application as MainApplication).playingBarIsVisible) up()
        (requireActivity() as MainActivity).mainLabel.text = mainLabelCurText
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            HIGHLIGHTED_START_KEY,
            viewModel.highlightedStartLiveData.value!!
        )

        super.onSaveInstanceState(outState)
    }

    override fun updateUI(src: List<Track>) {
        adapter = TrackAdapter(src)
        recyclerView.adapter = adapter
    }

    override fun filter(models: Collection<Track>?, query: String): List<Track> =
        query.lowercase().let { lowerCase ->
            models?.filter {
                lowerCase in it.title.lowercase()
                        || lowerCase in it.artist.lowercase()
                        || lowerCase in it.playlist.lowercase()
            } ?: listOf()
        }

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {
        inner class TrackHolder(view: View) :
            RecyclerView.ViewHolder(view),
            View.OnClickListener {
            private lateinit var track: Track
            private var ind: Int = 0

            val titleTextView: TextView = itemView.findViewById(R.id.track_title)
            val settingsButton: ImageButton = itemView.findViewById(R.id.track_item_settings)
            val artistsAlbumTextView: TextView =
                itemView.findViewById(R.id.track_author_album)
            private val trackNumberTextView: TextView = itemView.findViewById(R.id.track_number)

            init {
                itemView.setOnClickListener(this)
                titleTextView.setTextColor(ViewSetter.textColor)
                artistsAlbumTextView.setTextColor(ViewSetter.textColor)
                settingsButton.setImageResource(ViewSetter.settingsButtonImage)
            }

            override fun onClick(v: View?) {
                (callbacks as Callbacks?)?.onTrackSelected(track, tracks, ind)
            }

            fun bind(_track: Track, _ind: Int) {
                track = _track
                ind = _ind

                val artistAlbum =
                    "${
                        track.artist
                            .let { if (it == "<unknown>") resources.getString(R.string.unknown_artist) else it }
                    } / ${if (track is CustomPlaylistTrack) (track as CustomPlaylistTrack).playlist else track.playlist}"

                titleTextView.text =
                    track.title.let { if (it == "<unknown>") resources.getString(R.string.unknown_track) else it }
                artistsAlbumTextView.text = artistAlbum
                trackNumberTextView.text = (layoutPosition + 1).toString()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(layoutInflater.inflate(R.layout.list_item_track, parent, false))

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int) {
            holder.bind(tracks[position], position)

            val trackTitle = holder.titleTextView
            val trackAlbumArtist = holder.artistsAlbumTextView
            val settingsButton = holder.settingsButton
            val highlightedRows = (requireActivity().application as MainApplication).highlightedRows

            settingsButton.setOnClickListener {
                (requireActivity() as MainActivity)
                    .trackSettingsButtonAction(
                        it,
                        tracks[position],
                        BottomSheetBehavior.STATE_COLLAPSED
                    )
            }

            when (tracks[position].path) {
                in highlightedRows -> {
                    val color = Params.getInstance().theme.rgb
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)
                }

                else -> {
                    val color = ViewSetter.textColor
                    trackTitle.setTextColor(color)
                    trackAlbumArtist.setTextColor(color)
                }
            }
        }

        fun highlight(track: Track): Unit =
            (requireActivity().application as MainApplication).run {
                highlightedRows.clear()
                highlightedRows.add(track.path)
                highlightedRows = highlightedRows.distinct().toMutableList()
                notifyDataSetChanged()
            }
    }
}