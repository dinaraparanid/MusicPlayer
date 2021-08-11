package com.dinaraparanid.prima.databases.daos

import androidx.room.Dao
import androidx.room.Query
import com.dinaraparanid.prima.databases.entities.AlbumOld
import com.dinaraparanid.prima.utils.polymorphism.EntityDao
import java.util.*

@Dao
@Deprecated("Now using android storage instead of database")
interface AlbumDao : EntityDao<AlbumOld> {
    @Query("SELECT * FROM album")
    suspend fun getAlbums(): List<AlbumOld>

    @Query("SELECT * FROM album WHERE id = (:id)")
    suspend fun getAlbum(id: UUID): AlbumOld?

    @Query("SELECT * FROM album WHERE title = (:title)")
    suspend fun getAlbum(title: String): AlbumOld?
}