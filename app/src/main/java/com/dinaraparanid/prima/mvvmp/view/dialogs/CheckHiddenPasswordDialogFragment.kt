package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.text.InputType
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.ui_handlers.CheckHiddenPasswordUIHandler
import kotlinx.coroutines.channels.Channel
import org.koin.core.component.inject

/**
 * Dialog that creates new password
 * when user opens hidden tracks for the first time
 */

class CheckHiddenPasswordDialogFragment(passwordHash: Int, showHiddenFragmentChannel: Channel<Unit>) :
    InputDialogFragment<CheckHiddenPasswordUIHandler.CheckHiddenPasswordUIHandlerArgs, CheckHiddenPasswordUIHandler>(
        message = R.string.check_password,
        textType = InputType.TYPE_TEXT_VARIATION_PASSWORD,
    ) {
    override val handlerOnOkArgs = CheckHiddenPasswordUIHandler.CheckHiddenPasswordUIHandlerArgs(
        passwordHash, showHiddenFragmentChannel
    )

    override val uiHandler by inject<CheckHiddenPasswordUIHandler>()
}