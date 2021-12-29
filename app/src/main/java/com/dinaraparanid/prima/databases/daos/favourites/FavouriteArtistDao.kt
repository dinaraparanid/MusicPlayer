package com.dinaraparanid.prima.databases.daos.favourites

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.favourites.FavouriteArtist
import com.dinaraparanid.prima.utils.polymorphism.EntityDao

/**
 * DAO for user's favourite artists
 * (singers, compositors and etc.)
 */

@Dao
interface FavouriteArtistDao : EntityDao<FavouriteArtist> {
    /**
     * Gets all favourite artists asynchronously
     * @return all favourite artists
     */

    @Query("SELECT * FROM favourite_artists")
    suspend fun getArtistsAsync(): List<FavouriteArtist>

    /**
     * Gets artist by his name asynchronously
     * @param name artist's name
     * @return artist or null if it doesn't exist
     */

    @Query("SELECT * FROM favourite_artists WHERE name = :name")
    suspend fun getArtistAsync(name: String): FavouriteArtist?
}