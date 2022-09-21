package com.dinaraparanid.prima.utils.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

/** Adds some horizontal space between items in recycler view */

class HorizontalSpaceItemDecoration(private val horizontalSpaceHeight: Int) :
    ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.right = horizontalSpaceHeight
    }
}