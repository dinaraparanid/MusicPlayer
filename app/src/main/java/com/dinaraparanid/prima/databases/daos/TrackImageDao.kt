package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.TrackImage
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * Dao for track - album image relationships
 */

@Dao
@Deprecated("Now changing track's cover's tag with JAudioTag")
interface TrackImageDao : EntityDao<TrackImage> {
    /**
     * Gets track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     * @return track with image or null if it isn't exists
     */

    @Deprecated("Now changing track's cover's tag with JAudioTag")
    @Query("SELECT * FROM image_tracks WHERE track_path = :path")
    suspend fun getTrackWithImage(path: String): TrackImage?

    /**
     * Removes track with its image asynchronously
     * @param path path of track (DATA column from MediaStore)
     */

    @Deprecated("Now changing track's cover's tag with JAudioTag")
    @Query("DELETE FROM image_tracks WHERE track_path = :path")
    suspend fun removeTrackWithImage(path: String)
}