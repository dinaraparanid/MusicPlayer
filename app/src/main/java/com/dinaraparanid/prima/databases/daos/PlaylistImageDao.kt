package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.PlaylistImage
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/** [Dao] for playlists' images */

@Dao
interface PlaylistImageDao : EntityDao<PlaylistImage> {
    /**
     * Gets playlist with its image asynchronously
     * @param title playlist's title
     * @return playlist with image or null if it isn't exists
     */

    @Query("SELECT * FROM image_playlists WHERE title = :title")
    suspend fun getPlaylistWithImage(title: String): PlaylistImage?
}