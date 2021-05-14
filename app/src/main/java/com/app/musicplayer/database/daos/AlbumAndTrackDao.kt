package com.app.musicplayer.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.app.musicplayer.core.Album
import com.app.musicplayer.core.Track
import com.app.musicplayer.database.relationships.AlbumAndTrack
import java.util.UUID

@Dao
interface AlbumAndTrackDao {
    @Transaction
    @Query("SELECT * FROM album")
    fun getAlbumsWithTracks(): LiveData<List<AlbumAndTrack>>

    @Query("SELECT * FROM album WHERE id = (:trackAlbumId)")
    fun getAlbumByTrack(trackAlbumId: UUID): LiveData<Album?>

    @Query("SELECT * FROM track WHERE album_id = (:albumId)")
    fun getTracksFromAlbum(albumId: UUID): LiveData<List<Track>>
}