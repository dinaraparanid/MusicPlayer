package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for user's favourite tracks
 */

class FavouriteTrackListFragment : OnlySearchMenuTrackListFragment() {
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                itemList.apply {
                    val task = FavouriteRepository.instance.getTracksAsync()
                    clear()
                    addAll(Params.sortedTrackList(task.await()))
                    Unit
                }
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fragmentActivity.setSelectButtonVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        fragmentActivity.setSelectButtonVisibility(false)
    }
}