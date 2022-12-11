package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.CreateHiddenPasswordUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

class CreateHiddenPasswordDialog(target: Target, showHiddenFragmentChannel: Channel<Unit>) :
    InputDialog<CreateHiddenPasswordUIHandler>(
        message = R.string.new_password,
        textType = InputType.TYPE_TEXT_VARIATION_PASSWORD
    ) {
    enum class Target { CREATE, UPDATE }

    override val uiHandler by inject<CreateHiddenPasswordUIHandler> {
        parametersOf(target, showHiddenFragmentChannel)
    }
}