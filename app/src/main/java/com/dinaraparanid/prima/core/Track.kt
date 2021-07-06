package com.dinaraparanid.prima.core

import com.dinaraparanid.prima.databases.entities.FavouriteTrack
import java.io.Serializable

open class Track(
    open val title: String,
    open val artist: String,
    open val playlist: String,
    open val path: String,
    open val duration: Long,
) : Serializable, Favourable<FavouriteTrack> {
    override fun asFavourite(): FavouriteTrack = FavouriteTrack(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}