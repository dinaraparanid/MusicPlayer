package com.dinaraparanid.prima.utils.extensions

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.Track

fun Collection<Track>.toPlaylist(): Playlist = DefaultPlaylist(tracks = toMutableList())