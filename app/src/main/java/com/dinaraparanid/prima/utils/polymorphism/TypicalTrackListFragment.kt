package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTrackListBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.viewmodels.mvvm.TrackListViewModel
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.coroutines.*

/**
 * Typical track list fragment ancestor
 * without no special view features
 */

abstract class TypicalTrackListFragment : AbstractTrackListFragment<FragmentTrackListBinding>() {
    override var binding: FragmentTrackListBinding? = null
    override var updater: SwipeRefreshLayout? = null
    override var emptyTextView: TextView? = null
    override var amountOfTracks: carbon.widget.TextView? = null
    override var trackOrderTitle: carbon.widget.TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        titleDefault = resources.getString(R.string.tracks)

        binding = DataBindingUtil
            .inflate<FragmentTrackListBinding>(
                inflater,
                R.layout.fragment_track_list,
                container,
                false
            )
            .apply {
                viewModel = TrackListViewModel(this@TypicalTrackListFragment)

                updater = trackSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        try {
                            runOnUIThread {
                                loadAsync().join()
                                updateUIAsync()
                                isRefreshing = false
                            }
                        } catch (ignored: Exception) {
                            // permissions not given
                        }
                    }
                }

                this@TypicalTrackListFragment.amountOfTracks = amountOfTracks
                this@TypicalTrackListFragment.trackOrderTitle = trackOrderTitle
                emptyTextView = trackListEmpty

                try {
                    runOnUIThread {
                        val task = loadAsync()
                        var progress: KProgressHUD

                        while (true) {
                            try {
                                progress = createAndShowAwaitDialog(requireContext(), false)
                                break
                            } catch (ignored: Exception) {
                            }
                        }

                        task.join()
                        progress.dismiss()

                        setEmptyTextViewVisibility(itemList)
                        itemListSearch.addAll(itemList)

                        adapter = TrackAdapter(itemList).apply {
                            stateRestorationPolicy =
                                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                        }

                        amountOfTracks.apply {
                            isSelected = true
                            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                            text = txt
                        }

                        recyclerView = trackRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = this@TypicalTrackListFragment.adapter?.apply {
                                stateRestorationPolicy =
                                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                            }
                            addItemDecoration(VerticalSpaceItemDecoration(30))
                        }

                        if (application.playingBarIsVisible) up()
                    }
                } catch (ignored: Exception) {
                    // permissions not given
                }

                amountOfTracks.run {
                    isSelected = true
                    val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                    text = txt
                }

                updateOrderTitle()
            }

        return binding!!.root
    }
}