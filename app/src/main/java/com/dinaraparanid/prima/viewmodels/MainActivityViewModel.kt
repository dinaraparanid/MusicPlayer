package com.dinaraparanid.prima.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MainActivityViewModel : ViewModel() {
    internal val sheetBehaviorPositionLiveData = MutableLiveData<Int>()
    internal val progressLiveData = MutableLiveData<Int>()
    internal val curIndexLiveData = MutableLiveData<Int>()
    internal val trackSelectedLiveData = MutableLiveData<Boolean>()

    fun load(
        sheetBehaviorPosition: Int?,
        progress: Int?,
        curIndex: Int?,
        trackSelected: Boolean?,
    ) {
        sheetBehaviorPositionLiveData.value =
            sheetBehaviorPosition ?: BottomSheetBehavior.STATE_COLLAPSED
        progressLiveData.value = progress ?: 0
        curIndexLiveData.value = curIndex ?: -1
        trackSelectedLiveData.value = trackSelected ?: false
    }
}