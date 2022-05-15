package com.dinaraparanid.prima.utils.polymorphism.fragments

import androidx.appcompat.widget.SearchView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.dinaraparanid.prima.dialogs.createAndShowAwaitDialog
import com.dinaraparanid.prima.utils.polymorphism.*
import com.dinaraparanid.prima.utils.polymorphism.Loader
import com.dinaraparanid.prima.utils.polymorphism.UIUpdatable
import kotlinx.coroutines.sync.Mutex
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
        where Act : AbstractActivity,
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

    final override val mutex = Mutex()

    private fun freeUIMemory() {
        Glide.get(requireContext()).run {
            runOnIOThread { clearDiskCache() }
            bitmapPool.clearMemory()
            clearMemory()
        }
    }

    /** Frees UI */
    override fun onStop() {
        super.onStop()
        freeUIMemory()
        updater!!.clearAnimation()
        updater!!.clearDisappearingChildren()
        updater!!.clearFocus()
        updater!!.isEnabled = false
    }

    override fun onDestroyView() {
        freeUIMemory()
        updater = null
        super.onDestroyView()
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
        filterAsync(query)
        return true
    }

    final override fun onLowMemory() {
        super.onLowMemory()
        itemListSearch.clear()
    }

    final override fun onQueryTextSubmit(query: String?) = false

    final override val loaderContent: List<T> get() = itemList

    /** Like [UIUpdatable.updateUIAsync] but src is [itemList] */
    internal suspend fun updateUIAsync(isLocking: Boolean) =
        updateUIAsync(itemList, isLocking)

    protected fun filterAsync(query: String?) = when {
        query != null && query.isNotEmpty() -> {
            val filteredModelList = filter(
                itemList,
                query
            )

            runOnUIThread {
                itemListSearch.clear()
                itemListSearch.addAll(filteredModelList)
                updateUIAsync(itemListSearch, isLocking = true)
            }
        }

        else -> null
    }

    /**
     * Loads content with [loadAsync]
     * and updates UI with [updateUIAsync]
     */

    internal fun updateUIOnChangeContentAsync() = runOnUIThread {
        val task = loadAsync()
        val progress = createAndShowAwaitDialog(requireContext(), false)

        task.join()
        progress.dismiss()
        updateUIAsync(isLocking = true)
    }
}