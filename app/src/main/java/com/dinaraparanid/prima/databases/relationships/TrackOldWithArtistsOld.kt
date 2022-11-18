package com.dinaraparanid.prima.databases.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.databases.entities.old.ArtistOld
import com.dinaraparanid.prima.databases.entities.old.TrackOld
import com.dinaraparanid.prima.utils.polymorphism.databases.CrossRefEntity

/**
 * Relationships between [TrackOld] and its [ArtistOld]
 * @deprecated Now using android MediaStore instead of database
 */

@Deprecated("Now using android MediaStore instead of database")
data class TrackOldWithArtistsOld(
    @Embedded val track: TrackOld,
    @Relation(
        parentColumn = "track_id",
        entityColumn = "artist_id",
        associateBy = Junction(ArtistOldTrackOldCrossRef::class)
    )
    val artists: List<ArtistOld>
) : CrossRefEntity {
    private companion object {
        /** UID required to serialize */
        private const val serialVersionUID = -8313441998470071379L
    }
}
