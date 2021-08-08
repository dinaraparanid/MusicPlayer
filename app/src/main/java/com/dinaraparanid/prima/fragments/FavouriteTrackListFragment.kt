package com.dinaraparanid.prima.fragments

import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.OnlySearchMenuTrackListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalTrackListFragment
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [OnlySearchMenuTrackListFragment] for user's favourite tracks
 */

class FavouriteTrackListFragment : TypicalTrackListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
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