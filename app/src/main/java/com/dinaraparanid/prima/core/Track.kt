package com.dinaraparanid.prima.core

import java.io.Serializable

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long
) : Serializable