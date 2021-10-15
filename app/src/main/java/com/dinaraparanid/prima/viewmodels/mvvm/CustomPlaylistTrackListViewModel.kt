package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.core.AbstractTrack
import com.dinaraparanid.prima.databinding.FragmentCustomPlaylistTrackListBinding
import com.dinaraparanid.prima.fragments.TrackSelectFragment
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrackListFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.CustomPlaylistTrackListFragment]
 */

class CustomPlaylistTrackListViewModel(
    fragment: AbstractTrackListFragment<FragmentCustomPlaylistTrackListBinding>,
    private val mainLabelCurText: String,
    private val playlistId: Long,
    private val itemList: MutableList<AbstractTrack>
) : PlaylistTrackListViewModel<FragmentCustomPlaylistTrackListBinding>(fragment) {

    /** shows [com.dinaraparanid.prima.fragments.TrackSelectFragment] */
    @JvmName("onAddTrackButtonClicked")
    internal fun onAddTrackButtonClicked() {
        fragment.unchecked.requireActivity().supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                TrackSelectFragment.newInstance(
                    mainLabelCurText,
                    fragment.unchecked.resources.getString(R.string.tracks),
                    playlistId,
                    itemList.toPlaylist()
                )
            )
            .addToBackStack(null)
            .apply {
                (fragment.unchecked.requireActivity() as MainActivity).sheetBehavior.state =
                    BottomSheetBehavior.STATE_COLLAPSED
            }
            .commit()
    }
}