package com.dinaraparanid.prima.utils.extensions

/**
 * Converts given [CharSequence] to a correct android file name
 */

internal inline val CharSequence.correctFileName
    get() = this
        .replace("[|?*<>]".toRegex(), "_")
        .replace(":", " -")
        .replace("\"", "\'")

internal inline val CharSequence.fixedImageUrl
    get() = replaceFirst("rapgenius".toRegex(), "genius")