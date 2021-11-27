package com.dinaraparanid.prima.utils.polymorphism

import android.widget.TextView
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import java.io.Serializable

/**
 * Ancestor for all fragments with [RecyclerView]
 */

abstract class ListFragment<Act, T, A, VH, B> :
    CallbacksFragment<B, Act>(),
    AsyncContext
        where Act: AbstractActivity,
              T : Serializable,
              VH : RecyclerView.ViewHolder,
              A : RecyclerView.Adapter<VH>,
              B : ViewDataBinding {
    /**
     * [RecyclerView.Adapter] for every fragment
     */

    protected abstract val adapter: A

    /**
     * [ViewModel] for every fragment.
     * Mainly used to call coroutines and remember some data
     */

    abstract val viewModel: ViewModel

    /**
     * [TextView] that shows when there are no entities
     */

    protected abstract var emptyTextView: TextView?

    /** [RecyclerView] for every fragment */

    protected var recyclerView: RecyclerView? = null

    /** Default title if there weren't any in params */

    protected lateinit var titleDefault: String

    final override val coroutineScope: CoroutineScope
        get() = lifecycleScope

    override fun onDestroyView() {
        super.onDestroyView()
        emptyTextView = null
        recyclerView = null
    }

    /**
     * Sets [emptyTextView] visibility.
     * If [src] is empty [TextView.VISIBLE],
     * else [TextView.INVISIBLE]
     *
     * @param src entities to show in fragment (if there are any)
     */

    protected fun setEmptyTextViewVisibility(src: List<T>) {
        emptyTextView!!.visibility = if (src.isEmpty()) TextView.VISIBLE else TextView.INVISIBLE
    }
}