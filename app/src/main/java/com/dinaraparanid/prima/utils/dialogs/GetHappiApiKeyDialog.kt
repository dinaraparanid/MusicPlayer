package com.dinaraparanid.prima.utils.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.InputDialog

/**
 * Dialog to get user's api key
 */

@Deprecated("Switched to Genius API")
internal class GetHappiApiKeyDialog(callFragment: (String) -> Unit) : InputDialog(
    R.string.api_key,
    callFragment,
    null
)