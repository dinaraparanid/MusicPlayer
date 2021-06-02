package com.dinaraparanid.prima.database.relationships

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.UUID

@Entity(tableName = "artist_track", primaryKeys = ["artist_id", "track_id"])
data class ArtistTrackCrossRef(
    @ColumnInfo(name = "artist_id", index = true) val artistId: UUID,
    @ColumnInfo(name = "track_id", index = true) val trackId: UUID
)
