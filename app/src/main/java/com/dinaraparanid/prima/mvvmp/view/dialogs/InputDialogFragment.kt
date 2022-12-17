package com.dinaraparanid.prima.mvvmp.view.dialogs

import android.app.AlertDialog
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogInputBinding
import com.dinaraparanid.prima.dialogs.MessageDialog
import com.dinaraparanid.prima.mvvmp.StateChangedCallback
import com.dinaraparanid.prima.mvvmp.presenters.InputDialogPresenter
import com.dinaraparanid.prima.mvvmp.ui_handlers.InputDialogUIHandler
import com.dinaraparanid.prima.mvvmp.view_models.InputDialogViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * Base input dialog
 * @param message action message
 * (String param is input itself and how it can be used in okAction)
 * @param errorMessage message if okAction is failed
 * @param textType [InputType] of given text
 * @param maxInputLength maximum amount of characters to input
 */

abstract class InputDialogFragment<A : InputDialogUIHandler.Args, H : InputDialogUIHandler<A>>(
    @StringRes private val message: Int,
    @StringRes private val errorMessage: Int = R.string.unknown_error,
    private val textType: Int = InputType.TYPE_CLASS_TEXT,
    private val maxInputLength: Int = NO_LIMIT_LENGTH,
) : ObservableDialogFragment<InputDialogPresenter, InputDialogViewModel, H, DialogInputBinding>() {
    private companion object {
        private const val NO_LIMIT_LENGTH = -1
    }

    protected abstract val handlerOnOkArgs: A

    final override val stateChangesCallbacks = emptyArray<StateChangedCallback<H>>()

    final override val viewModel by viewModel<InputDialogViewModel> {
        parametersOf(textType, maxInputLength)
    }

    final override val dialogBinding: DialogInputBinding
        get() = DataBindingUtil
            .inflate<DialogInputBinding>(
                layoutInflater,
                R.layout.dialog_input,
                null, false
            )
            .apply {
                viewModel = this@InputDialogFragment.viewModel

                if (textType == InputType.TYPE_TEXT_VARIATION_PASSWORD)
                    inputDialogInputText.transformationMethod =
                        PasswordTransformationMethod.getInstance()

                executePendingBindings()
            }

    final override val dialogView
        get() = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setView(binding.root)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val input = binding.inputDialogInputText.text.toString()

                lifecycleScope.launch {
                    uiHandler.runCatching {
                        handlerOnOkArgs.onOkAsync(input, dialog)
                    }.getOrElse {
                        dialog.cancel()
                        MessageDialog(errorMessage).show(parentFragmentManager, null)
                        uiHandler.runCatching { handlerOnOkArgs.onErrorAsync(input) }
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()
}