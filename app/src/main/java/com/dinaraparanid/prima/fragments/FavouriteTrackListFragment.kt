package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalTrackListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [OnlySearchMenuTrackListFragment] for user's favourite tracks
 */

class FavouriteTrackListFragment : TypicalTrackListFragment() {
    override suspend fun loadAsync(): Job = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                itemList.run {
                    val task = FavouriteRepository.instance.getTracksAsync()
                    clear()
                    addAll(Params.sortedTrackList(task.await()))
                    Unit
                }
            } catch (ignored: Exception) {
            }
        }
    }
}