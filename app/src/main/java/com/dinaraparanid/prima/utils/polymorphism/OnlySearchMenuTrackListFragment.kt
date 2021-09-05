package com.dinaraparanid.prima.utils.polymorphism

import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.widget.SearchView
import androidx.databinding.ViewDataBinding
import com.dinaraparanid.prima.R

/**
 * [TypicalTrackListFragment] with only search option in menu
 */

abstract class OnlySearchMenuTrackListFragment<B : ViewDataBinding> :
    AbstractTrackListFragment<B>() {
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_track_search, menu)

        (menu.findItem(R.id.find).actionView as SearchView)
            .setOnQueryTextListener(this@OnlySearchMenuTrackListFragment)

        menu.findItem(R.id.find_by).setOnMenuItemClickListener { selectSearch() }
    }
}