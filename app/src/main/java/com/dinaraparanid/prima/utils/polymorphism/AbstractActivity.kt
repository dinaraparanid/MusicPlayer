package com.dinaraparanid.prima.utils.polymorphism

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.ViewSetter
import java.lang.ref.WeakReference

/** Ancestor for all [AppCompatActivity] */

abstract class AbstractActivity : AppCompatActivity(), AsyncContext {
    internal var currentFragment = WeakReference<Fragment>(null)
    abstract val viewModel: ViewModel

    override val coroutineScope get() = lifecycleScope

    /** Initializes activity's view */
    protected abstract fun initView(savedInstanceState: Bundle?)

    /** Initializes activity's first fragment */
    protected abstract fun initFirstFragment()

    /**
     * Sets theme for app.
     * If user selected custom theme, it' ll show it.
     * Else it will show one of standard themes (default is Purple Night)
     */

    protected fun setTheme() = setTheme(
        when (Params.instance.themeColor.second) {
            -1 -> ViewSetter.appTheme
            else -> when (Params.instance.themeColor.second) {
                0 -> R.style.Theme_MusicPlayerWhite
                else -> R.style.Theme_MusicPlayerBlack
            }
        }
    )
}