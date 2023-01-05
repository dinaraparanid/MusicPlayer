package com.dinaraparanid.prima.utils.polymorphism.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentTrackListBinding
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.decorations.DividerItemDecoration
import com.dinaraparanid.prima.utils.decorations.VerticalSpaceItemDecoration
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.polymorphism.runOnUIThread
import com.dinaraparanid.prima.mvvmp.old_shit.TrackListViewModel
import com.dinaraparanid.prima.mvvmp.view.fragments.AbstractTrackListFragment
import com.kaopiz.kprogresshud.KProgressHUD

/**
 * Typical track list fragment ancestor
 * without no special view features
 */

abstract class TypicalViewTrackListFragment : AbstractTrackListFragment<FragmentTrackListBinding>() {
    final override var binding: FragmentTrackListBinding? = null
    final override var updater: SwipeRefreshLayout? = null
    final override var emptyTextView: TextView? = null
    final override var amountOfTracks: carbon.widget.TextView? = null
    final override var trackOrderTitle: carbon.widget.TextView? = null
    private var awaitDialog: KProgressHUD? = null

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate<FragmentTrackListBinding>(
                inflater,
                R.layout.fragment_track_list,
                container,
                false
            )
            .apply {
                viewModel = TrackListViewModel(this@TypicalViewTrackListFragment)

                updater = trackSwipeRefreshLayout.apply {
                    setColorSchemeColors(Params.instance.primaryColor)
                    setOnRefreshListener {
                        try {
                            runOnUIThread {
                                loadAsync().join()
                                updateUIAsync(isLocking = true)
                                isRefreshing = false
                            }
                        } catch (ignored: Exception) {
                            // permissions not given
                        }
                    }
                }

                this@TypicalViewTrackListFragment.amountOfTracks = amountOfTracks
                this@TypicalViewTrackListFragment.trackOrderTitle = trackOrderTitle
                emptyTextView = trackListEmpty

                try {
                    runOnUIThread {
                        val task = loadAsync()
                        awaitDialog = createAndShowAwaitDialog(requireContext(), false)
                        task.join()
                        awaitDialog?.dismiss()
                        initAdapter()

                        setEmptyTextViewVisibility(itemList)
                        itemListSearch.addAll(itemList)
                        adapter.setCurrentList(itemList)

                        amountOfTracks.apply {
                            isSelected = true
                            val txt = "${resources.getString(R.string.tracks)}: ${itemList.size}"
                            text = txt
                        }

                        recyclerView = trackRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = this@TypicalViewTrackListFragment.adapter
                            addItemDecoration(VerticalSpaceItemDecoration(30))

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                                Params.getInstanceSynchronized().areDividersShown
                            ) addItemDecoration(
                                DividerItemDecoration(requireContext(), Divider.instance)
                            )
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

    final override fun onDestroyView() {
        super.onDestroyView()
        awaitDialog?.dismiss()
        awaitDialog = null
    }
}