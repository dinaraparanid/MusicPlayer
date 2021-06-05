package com.dinaraparanid.prima.core

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "album", foreignKeys = [ForeignKey(
        entity = ArtistOld::class,
        parentColumns = arrayOf("artist_id"),
        childColumns = arrayOf("artist_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
@Deprecated("Now using android storage instead of database")
data class AlbumOld(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    @ColumnInfo(name = "artist_id", index = true) val artistId: UUID? = null,
    val title: String = "Unknown Album",
) {
    override fun toString(): String = title
}
