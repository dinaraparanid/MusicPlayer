package com.dinaraparanid.prima.utils

import android.content.Context
import com.dinaraparanid.prima.R
import com.kaopiz.kprogresshud.KProgressHUD

/**
 * Creates dialog that shows that
 * user needs to wait until some event is finished
 *
 * @param context [Context] in which dialog will be shown
 * @return dialog itself
 */

internal fun createAndShowAwaitDialog(context: Context, cancellable: Boolean = true): KProgressHUD =
    KProgressHUD.create(context)
        .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
        .setLabel(context.resources.getString(R.string.please_wait))
        .setCancellable(cancellable)
        .setAnimationSpeed(2)
        .setDimAmount(0.5F)
        .show()