package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.core.AbstractTrack

/** Constructs new playlist from collection of tracks */
internal fun Collection<AbstractTrack>.toPlaylist() = DefaultPlaylist(tracks = toTypedArray())