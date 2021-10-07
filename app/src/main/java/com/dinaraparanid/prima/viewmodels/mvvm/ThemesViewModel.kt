package com.dinaraparanid.prima.viewmodels.mvvm

import android.app.AlertDialog
import android.content.Intent
import android.provider.MediaStore
import com.dinaraparanid.prima.BR
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.ColorPickerDialog
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.drawables.Divider
import com.dinaraparanid.prima.utils.drawables.FontDivider
import com.dinaraparanid.prima.utils.drawables.Marker
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.ChangeImageFragment
import java.lang.ref.WeakReference

/**
 * MVVM View Model for
 * [com.dinaraparanid.prima.fragments.ThemesFragment]
 */

class ThemesViewModel(private val activity: WeakReference<MainActivity>) : ViewModel() {

    /**
     * 1. Shows color picker dialog to choose primary color
     * 2. Shows dialog with night or day theme selection
     * 3. Recreates activity
     */
    @JvmName("onCustomThemeClicked")
    internal fun onCustomThemeClicked() {
        ColorPickerDialog(activity.unchecked, this).show(object : ColorPickerDialog.ColorPickerObserver() {
            override fun onColorPicked(color: Int) {
                AlertDialog.Builder(activity.unchecked)
                    .setTitle(R.string.select_color)
                    .setSingleChoiceItems(
                        arrayOf(
                            activity.unchecked.resources.getString(R.string.day),
                            activity.unchecked.resources.getString(R.string.night)
                        ),
                        -1
                    ) { _, item ->
                        val themeColors = color to item
                        Params.instance.themeColor = themeColors
                        StorageUtil(activity.unchecked).storeCustomThemeColors(themeColors)

                        Divider.update()
                        FontDivider.update()
                        Marker.update()

                        activity.unchecked.startActivity(Intent(activity.unchecked, MainActivity::class.java))
                    }
                    .show()
            }
        })
    }

    /**
     * Sends intent to set picture from user's gallery
     * as app's background image
     */

    @JvmName("onSetBackgroundPictureClicked")
    internal fun onSetBackgroundPictureClicked(): AlertDialog = AlertDialog.Builder(activity.unchecked)
        .setSingleChoiceItems(
            arrayOf(
                activity.unchecked.resources.getString(R.string.set_background_picture),
                activity.unchecked.resources.getString(R.string.remove_background_picture)
            ),
            -1
        ) { dialog, item ->

            when (item) {
                0 -> activity.unchecked.startActivityForResult(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    ), ChangeImageFragment.PICK_IMAGE
                )

                else -> {
                    StorageUtil(activity.unchecked).clearBackgroundImage()
                    Params.instance.backgroundImage = null

                    activity.unchecked.binding!!.run {
                        drawerLayout.setBackgroundColor(params.secondaryColor)
                        appbar.setBackgroundColor(params.primaryColor)
                        switchToolbar.setBackgroundColor(params.primaryColor)
                    }

                    notifyPropertyChanged(BR._all)
                }
            }

            dialog.dismiss()
        }
        .show()
}