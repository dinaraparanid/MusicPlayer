package com.dinaraparanid.prima.viewmodels.mvvm

import android.media.PlaybackParams
import androidx.fragment.app.FragmentActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings

/**
 * MVVM View Model for [com.dinaraparanid.prima.fragments.EqualizerFragment]
 */

class EqualizerViewModel(private val activity: FragmentActivity) : ViewModel() {
    /** Clears equalizer fragment */

    @JvmName("onBackButtonPressed")
    internal fun onBackButtonPressed() = activity.supportFragmentManager.popBackStack()

    /** Enables or disables equalizer */

    @JvmName("onSwitchCheckedChange")
    internal fun onSwitchCheckedChange(isChecked: Boolean) {
        val app = activity.application as MainApplication
        app.equalizer.enabled = isChecked
        app.bassBoost.enabled = isChecked
        app.presetReverb.enabled = isChecked
        EqualizerSettings.instance.isEqualizerEnabled = isChecked
        EqualizerSettings.instance.equalizerModel!!.isEqualizerEnabled = isChecked

        val loader = StorageUtil(activity)
        app.musicPlayer!!.playbackParams = PlaybackParams()
            .setPitch(if (isChecked) loader.loadPitch() else 1F)
            .setSpeed(if (isChecked) loader.loadSpeed() else 1F)
    }

    /** Changes bass amount */

    @JvmName("onControllerBassProgressChanged")
    internal fun onControllerBassProgressChanged(progress: Int) {
        EqualizerSettings.instance.bassStrength = (1000F / 19 * progress).toInt().toShort()
        (activity.application as MainApplication).bassBoost.setStrength(EqualizerSettings.instance.bassStrength)
        EqualizerSettings.instance.equalizerModel!!.bassStrength =
            EqualizerSettings.instance.bassStrength

        if (Params.instance.saveEqualizerSettings)
            StorageUtil(activity)
                .storeBassStrength(EqualizerSettings.instance.bassStrength)
    }
}