package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.fragments.PlaylistListFragment
import com.dinaraparanid.prima.utils.dialogs.NewPlaylistDialog

/** 
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.PlaylistListFragment]
 */

class PlaylistListViewModel(private val fragment: PlaylistListFragment) : ViewModel() {

    /** Shows dialog to add user's playlist */
    @JvmName("onAddPlaylistButtonPressed")
    internal fun onAddPlaylistButtonPressed() = NewPlaylistDialog(fragment)
        .show(fragment.parentFragmentManager, null)
}