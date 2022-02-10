package com.dinaraparanid.prima.viewmodels.mvvm

import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.extensions.unchecked

/** MVVM View Model for track item */

open class TrackItemViewModel(
    private val _pos: Int? = null,
    private val _track: AbstractTrack? = null
) : ViewModel() {
    internal val pos
        @JvmName("getPos")
        get() = _pos!!

    internal val track get() = _track!!

    /** Formats track title */
    internal inline val title
        @JvmName("getTitle")
        get() = track.title.let {
            when (it) {
                "<unknown>" -> params
                    .application
                    .unchecked
                    .resources
                    .getString(R.string.unknown_track)

                else -> it
            }
        }

    /** Formats track's artist and album */
    internal inline val artistAndAlbum
        @JvmName("getArtistAndAlbum")
        get() = track.artistAndAlbumFormatted

    /** Gets track number as string */
    internal inline val number
        @JvmName("getNumber")
        get() = pos.toString()

    /** Gets text color depending on what track is currently playing */
    @JvmName("getTextColor")
    internal fun getTextColor(tracks: Array<AbstractTrack>, position: Int) = when {
        (params.application.unchecked as MainApplication).highlightedPath.isEmpty() -> params.fontColor

        tracks[position].path ==
                (params.application.unchecked as MainApplication).highlightedPath.orNull() ->
            params.primaryColor

        else -> params.fontColor
    }
}