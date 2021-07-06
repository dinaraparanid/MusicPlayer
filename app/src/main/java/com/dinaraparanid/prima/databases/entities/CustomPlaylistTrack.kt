package com.dinaraparanid.prima.databases.entities

import androidx.room.*
import com.dinaraparanid.prima.core.Track

@Entity(
    tableName = "CustomTracks", foreignKeys = [ForeignKey(
        entity = CustomPlaylist.Entity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("playlist_id"),
        onDelete = ForeignKey.CASCADE
    )]
)
data class CustomPlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Long,
    override val title: String,
    @ColumnInfo(name = "artist_name") override val artist: String,
    @ColumnInfo(name = "playlist_title", index = true) override val playlist: String,
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    override val path: String,
    override val duration: Long,
) : Track(title, artist, playlist, path, duration) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        if (other is CustomPlaylistTrack) return id == other.id
        return path == other.path
    }

    override fun hashCode(): Int = super.hashCode() * 31 + id.hashCode()
}
