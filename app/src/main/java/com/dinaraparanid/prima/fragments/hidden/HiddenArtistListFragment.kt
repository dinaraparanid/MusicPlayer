package com.dinaraparanid.prima.fragments.hidden

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.HiddenRepository
import com.dinaraparanid.prima.dialogs.CreateHiddenPasswordDialog
import com.dinaraparanid.prima.utils.polymorphism.fragments.LoadAllArtistsFromStorageListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Artist list with artists hidden by user */

class HiddenArtistListFragment : LoadAllArtistsFromStorageListFragment() {
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.run {
                clear()
                addAll(HiddenRepository.getInstanceSynchronized().getArtistsAsync().await())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_hidden, menu)
        (menu.findItem(R.id.find).actionView as SearchView).setOnQueryTextListener(this)
        menu.findItem(R.id.change_password).setOnMenuItemClickListener {
            CreateHiddenPasswordDialog(CreateHiddenPasswordDialog.Target.CREATE, fragmentActivity)
                .show(requireActivity().supportFragmentManager, null)
            true
        }
    }
}