package com.dinaraparanid.prima.utils.polymorphism

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Some
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.databinding.ListItemTrackBinding
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.viewmodels.androidx.TrackListViewModel
import com.dinaraparanid.prima.viewmodels.mvvm.TrackItemViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ancestor for all tracks fragments
 */

abstract class AbstractTrackListFragment :
    TrackListSearchFragment<Track,
            AbstractTrackListFragment.TrackAdapter,
            AbstractTrackListFragment.TrackAdapter.TrackHolder>() {
    interface Callbacks : CallbacksFragment.Callbacks {
        /**
         * Plays track or just shows playing bar
         * @param track track to show in playing bar
         * @param tracks tracks from which current playlist' ll be constructed
         * @param needToPlay if true track' ll be played
         * else it' ll be just shown in playing bar
         */

        fun onTrackSelected(
            track: Track,
            tracks: Collection<Track>,
            needToPlay: Boolean = true
        )
    }

    public override var adapter: TrackAdapter? = null

    override val viewModel: TrackListViewModel by lazy {
        ViewModelProvider(this)[TrackListViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mainLabelOldText =
            requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY) ?: titleDefault
        mainLabelCurText =
            requireArguments().getString(MAIN_LABEL_CUR_TEXT_KEY) ?: titleDefault
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        val act = requireActivity() as MainActivity

        if (act.needToUpdate) {
            updateUIOnChangeTracks()
            act.needToUpdate = false
        }

        adapter?.highlight((requireActivity().application as MainApplication).curPath)
    }

    override fun updateUI(src: List<Track>) {
        try {
            viewModel.viewModelScope.launch(Dispatchers.Main) {
                adapter = TrackAdapter(src).apply {
                    stateRestorationPolicy =
                        RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                }

                recyclerView.adapter = adapter
                setEmptyTextViewVisibility(src)

                val text = "${resources.getString(R.string.tracks)}: ${src.size}"
                amountOfTracks.text = text
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onQueryTextChange(query: String?): Boolean {
        super.onQueryTextChange(query)
        val txt = "${resources.getString(R.string.tracks)}: ${itemListSearch.size}"
        amountOfTracks.text = txt
        return true
    }

    fun updateUIOnChangeTracks() {
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            val task = loadAsync()
            val progress = createAndShowAwaitDialog(requireContext())

            task.join()
            progress.dismiss()
            updateUI()
        }
    }

    /**
     * [RecyclerView.Adapter] for [TypicalTrackListFragment]
     * @param tracks tracks to use in adapter
     */

    inner class TrackAdapter(private val tracks: List<Track>) :
        RecyclerView.Adapter<TrackAdapter.TrackHolder>() {

        /**
         * [RecyclerView.ViewHolder] for tracks of [TrackAdapter]
         */

        inner class TrackHolder(internal val trackBinding: ListItemTrackBinding) :
            RecyclerView.ViewHolder(trackBinding.root),
            View.OnClickListener {
            private lateinit var track: Track

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                (callbacker as Callbacks?)?.onTrackSelected(track, tracks)
            }

            /**
             * Constructs GUI for track item
             * @param _track track to bind and use
             */

            fun bind(_track: Track) {
                trackBinding.viewModel = TrackItemViewModel(layoutPosition + 1)
                trackBinding.tracks = tracks.toTypedArray()
                trackBinding.track = _track
                trackBinding.executePendingBindings()
                track = _track
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder =
            TrackHolder(
                ListItemTrackBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun getItemCount(): Int = tracks.size

        override fun onBindViewHolder(holder: TrackHolder, position: Int): Unit = holder.run {
            bind(tracks[position])
            trackBinding.trackItemSettings.setOnClickListener {
                (requireActivity() as MainActivity)
                    .trackSettingsButtonAction(
                        it,
                        tracks[position],
                        BottomSheetBehavior.STATE_COLLAPSED
                    )
            }
        }

        /**
         * Highlight track in [RecyclerView]
         * @param path path of track to highlight
         */

        @Synchronized
        @SuppressLint("NotifyDataSetChanged")
        fun highlight(path: String): Unit = (requireActivity().application as MainApplication).run {
            highlightedRow = Some(path)
            notifyDataSetChanged()
        }
    }
}