package com.dinaraparanid.prima.database.relationships

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.dinaraparanid.prima.core.ArtistOld
import com.dinaraparanid.prima.core.TrackOld

@Deprecated("Now using android storage instead of database")
data class TrackWithArtists(
    @Embedded val track: TrackOld,
    @Relation(
        parentColumn = "track_id",
        entityColumn = "artist_id",
        associateBy = Junction(ArtistTrackCrossRef::class)
    )
    val artists: List<ArtistOld>
)
