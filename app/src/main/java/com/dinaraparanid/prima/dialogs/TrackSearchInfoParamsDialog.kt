package com.dinaraparanid.prima.dialogs

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainActivity.Companion.setSheetBehaviourFromExpandedToCollapsed
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.fragments.track_lists.TrackListFoundFragment

/**
 * Dialog to input title and artist
 * for track of which info should be found
 */

class TrackSearchInfoParamsDialog(curTrack: AbstractTrack) : TrackSearchParamsDialog(curTrack) {
    init {
        action = { dialog ->
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in,
                    R.anim.slide_out,
                    R.anim.slide_in,
                    R.anim.slide_out
                )
                .replace(
                    R.id.fragment_container,
                    TrackListFoundFragment.newInstance(
                        dialogBinding!!.searchTitle.text.toString(),
                        dialogBinding!!.searchArtist.text.toString(),
                        TrackListFoundFragment.Target.INFO
                    )
                )
                .addToBackStack(null)
                .commit()

            dialog.dismiss()
            (requireActivity() as MainActivity).setSheetBehaviourFromExpandedToCollapsed()
        }
    }
}