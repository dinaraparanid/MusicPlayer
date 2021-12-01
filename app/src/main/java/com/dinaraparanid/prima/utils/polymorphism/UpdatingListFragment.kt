package com.dinaraparanid.prima.utils.polymorphism

import androidx.appcompat.widget.SearchView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dinaraparanid.prima.utils.createAndShowAwaitDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable
import java.util.Collections

/**
 * [ListFragment] with swipe fresh layout
 * (to update its [itemList])
 */

abstract class UpdatingListFragment<Act, T, A, VH, B> :
    ListFragment<Act, T, A, VH, B>(),
    SearchView.OnQueryTextListener,
    UIUpdatable<List<T>>,
    FilterFragment<T>,
    Loader<List<T>>
        where Act: AbstractActivity,
              T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : AsyncListDifferAdapter<T, VH>,
              B : ViewDataBinding {

    /** Item list for every fragment */
    protected val itemList: MutableList<T> = Collections.synchronizedList(mutableListOf())

    /** Item list to use in search operations */
    protected val itemListSearch: MutableList<T> = Collections.synchronizedList(mutableListOf())

    /** Swipe refresh layout to update [itemList] */
    protected abstract var updater: SwipeRefreshLayout?

    private val mutex = Mutex()

    override fun onPause() {
        super.onPause()
        updater!!.clearAnimation()
        updater!!.clearDisappearingChildren()
        updater!!.clearFocus()
        updater!!.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updater = null
    }

    override fun onDestroy() {
        super.onDestroy()
        itemList.clear()
        itemListSearch.clear()
    }

    override fun onResume() {
        super.onResume()
        updater!!.isEnabled = true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null && query.isNotEmpty()) {
            val filteredModelList = filter(
                itemList,
                query
            )

            runOnUIThread {
                mutex.withLock {
                    itemListSearch.clear()
                    itemListSearch.addAll(filteredModelList)
                    updateUIAsync(itemListSearch).join()
                }
            }
        }
        return true
    }

    override fun onLowMemory() {
        super.onLowMemory()
        itemListSearch.clear()
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override val loaderContent: List<T> get() = itemList

    /** Like [UIUpdatable.updateUIAsync] but src is [itemList] */
    internal suspend fun updateUIAsync() = updateUIAsync(itemList)

    /**
     * Loads content with [loadAsync]
     * and updates UI with [updateUIAsync]
     */

    internal suspend fun updateUIOnChangeContentAsync() = coroutineScope {
        launch(Dispatchers.Main) {
            val task = loadAsync()
            val progress = createAndShowAwaitDialog(requireContext(), false)

            task.join()
            progress.dismiss()
            updateUIAsync()
        }
    }
}