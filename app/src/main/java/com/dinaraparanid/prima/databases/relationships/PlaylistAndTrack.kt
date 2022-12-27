package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylistTrack
import com.dinaraparanid.prima.databases.entities.CrossRefEntity

/**
 * Relationships between [CustomPlaylist.Entity]
 * and its [CustomPlaylistTrack]
 */

data class PlaylistAndTrack(
    @Embedded val playlist: CustomPlaylist.Entity,
    @Relation(
        parentColumn = "id",
        entityColumn = "playlist_id",
        entity = CustomPlaylistTrack::class
    )
    val track: CustomPlaylistTrack?
) : CrossRefEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -1910752515869966129L
    }
}