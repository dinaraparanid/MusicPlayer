package com.dinaraparanid.prima.utils.polymorphism.fragments

import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface MainActivityFragment {
    var isMainLabelInitialized: Boolean
    val awaitMainLabelInitCondition: Channel<Unit>
    var mainLabelCurText: String
}

/**
 * Should be called immediately after
 * initializing fragment's main label in AbstractActivity.onCreate
 */

internal suspend fun MainActivityFragment.setMainLabelInitialized() {
    isMainLabelInitialized = true
    awaitMainLabelInitCondition.send(Unit)
}

internal fun <T> T.setMainActivityMainLabel()
        where T : MainActivityFragment,
              T : AbstractFragment<*, MainActivity> = fragmentActivity.runOnIOThread {
    while (!fragmentActivity.isBindingInitialized)
        fragmentActivity.awaitBindingInitCondition.block()

    launch(Dispatchers.Main) {
        fragmentActivity.mainLabelCurText = mainLabelCurText
    }
}