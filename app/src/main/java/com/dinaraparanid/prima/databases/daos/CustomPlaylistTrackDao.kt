package com.dinaraparanid.prima.databases.daos

import androidx.room.*
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack

@Dao
interface CustomPlaylistTrackDao {
    @Query("SELECT * FROM CustomTracks")
    suspend fun getTracks(): List<CustomPlaylistTrack>

    @Query("SELECT * FROM CustomTracks WHERE path = :path")
    suspend fun getTrack(path: String): CustomPlaylistTrack?

    @Update
    suspend fun updateTrack(track: CustomPlaylistTrack)

    @Insert
    suspend fun addTrack(track: CustomPlaylistTrack)

    @Query("DELETE FROM CustomTracks WHERE path = :path AND playlist_id = :playlistId")
    suspend fun removeTrack(path: String, playlistId: Long)

    @Query("DELETE FROM CustomTracks WHERE playlist_title = :title")
    suspend fun removeTracksOfPlaylist(title: String)
}