package com.dinaraparanid.prima.utils.extensions

import android.content.res.ColorStateList
import androidx.databinding.BindingAdapter
import carbon.drawable.ripple.RippleDrawable
import carbon.view.View
import carbon.widget.*
import com.dinaraparanid.prima.utils.equalizer.AnalogController

class BindingAdapters {
    companion object {
        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: Button, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: Button, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: Button, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_tint")
        internal fun setTintColor(view: Button, color: Int) {
            view.setTintList(ColorStateList.valueOf(color))
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: Button, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: ConstraintLayout, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: ConstraintLayout, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: ConstraintLayout, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: ConstraintLayout, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: TextView, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: TextView, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: TextView, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_tint")
        internal fun setTintColor(view: TextView, color: Int) {
            view.setTintList(ColorStateList.valueOf(color))
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: TextView, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: ImageView, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: ImageView, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: ImageView, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_tint")
        internal fun setTintColor(view: ImageView, color: Int) {
            view.setTintList(ColorStateList.valueOf(color))
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: ImageView, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_cornerRadius")
        internal fun setCornerRadius(view: ImageView, color: Int) {
            view.setCornerRadius(color.toFloat())
        }

        @JvmStatic
        @BindingAdapter("app:carbon_tint")
        internal fun setColorTint(view: RecyclerView, color: Int) {
            view.setTintList(ColorStateList.valueOf(color))
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: Toolbar, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: Toolbar, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: Toolbar, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: Toolbar, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_backgroundTint")
        internal fun setBackgroundTint(view: FloatingActionButton, color: Int) {
            view.setBackgroundTint(color)
        }

        @JvmStatic
        @BindingAdapter("app:rippleColor")
        internal fun setRippleColor(view: FloatingActionButton, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationAmbientShadowColor")
        internal fun setAmbientShadowColor(view: View, color: Int) {
            view.outlineAmbientShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationShadowColor")
        internal fun setShadowColor(view: View, color: Int) {
            view.setElevationShadowColor(color)
        }

        @JvmStatic
        @BindingAdapter("app:carbon_elevationSpotShadowColor")
        internal fun setSpotShadowColor(view: View, color: Int) {
            view.outlineSpotShadowColor = color
        }

        @JvmStatic
        @BindingAdapter("app:carbon_tint")
        internal fun setTintColor(view: View, color: Int) {
            view.setTintList(ColorStateList.valueOf(color))
        }

        @JvmStatic
        @BindingAdapter("app:carbon_rippleColor")
        internal fun setRippleColor(view: View, color: Int) {
            view.rippleDrawable = RippleDrawable.create(
                ColorStateList.valueOf(color),
                RippleDrawable.Style.Over,
                view,
                false,
                10
            )
        }

        @JvmStatic
        @BindingAdapter("android:onProgressChanged")
        internal fun onProgressChanged(view: AnalogController, act: (Int) -> Unit) {
            view.setOnProgressChangedListener(act)
        }
    }
}