package com.dinaraparanid.prima.fragments.main_menu.favourites

import android.os.Bundle
import com.dinaraparanid.prima.databases.repositories.FavouriteRepository
import com.dinaraparanid.prima.utils.polymorphism.AbstractArtistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractArtistListFragment] for user's favourite artists */

class FavouriteArtistListFragment : AbstractArtistListFragment() {
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val task = FavouriteRepository
                    .getInstanceSynchronized()
                    .getArtistsAsync()

                itemList.run {
                    clear()
                    addAll(task.await())
                }
            } catch (ignored: Exception) {
            }
        }
    }
}