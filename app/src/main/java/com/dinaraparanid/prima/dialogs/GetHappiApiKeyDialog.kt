package com.dinaraparanid.prima.dialogs

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.mvvmp.view.dialogs.InputDialog

/** Dialog to get user's Happi API key */

@Deprecated("Switched to Genius API")
internal class GetHappiApiKeyDialog(callFragment: (String) -> Unit) : InputDialog(
    R.string.api_key,
    { apiKey, _ -> callFragment(apiKey) },
    null
)