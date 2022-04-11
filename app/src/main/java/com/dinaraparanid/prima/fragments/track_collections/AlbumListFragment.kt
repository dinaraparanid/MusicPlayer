package com.dinaraparanid.prima.fragments.track_collections

import com.dinaraparanid.prima.core.DefaultPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylistListFragment
import com.dinaraparanid.prima.utils.polymorphism.TypicalViewPlaylistListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [AbstractPlaylistListFragment] for all albums */

class AlbumListFragment : TypicalViewPlaylistListFragment() {
    override suspend fun loadAsync() = coroutineScope {
        launch(Dispatchers.IO) {
            itemList.clear()
            itemList.addAll(
                application
                    .allTracks
                    .map { it.album to it }
                    .distinctBy(Pair<*, *>::first)
                    .sortedBy(Pair<String, *>::first)
                    .map { (albumTitle, track) ->
                        DefaultPlaylist(
                            albumTitle,
                            AbstractPlaylist.PlaylistType.ALBUM,
                            track
                        )
                    }
            )
        }
    }
}