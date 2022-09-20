package com.dinaraparanid.prima.fragments.main_menu

import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.utils.polymorphism.fragments.LoadAllArtistsFromStorageListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [LoadAllArtistsFromStorageListFragment] that loads all artists */

class DefaultArtistListFragment : LoadAllArtistsFromStorageListFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_search, menu)
                (menu.findItem(R.id.find).actionView as SearchView)
                    .setOnQueryTextListener(this@DefaultArtistListFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = true
        })
    }

    /** Loads all artists from [MediaStore] */
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            val allArtistsTask = loadAllArtistsFromStorage()
            val hiddenArtistsTask = HiddenRepository.getInstanceSynchronized().getArtistsAsync()

            val allArtists = allArtistsTask.await()
            val hiddenArtists = hiddenArtistsTask.await().toHashSet()

            itemList.clear()
            itemList.addAll(allArtists.filter { it !in hiddenArtists })
        }
    }
}