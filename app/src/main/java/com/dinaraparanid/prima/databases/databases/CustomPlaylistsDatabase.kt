package com.dinaraparanid.prima.databases.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dinaraparanid.prima.databases.daos.CustomPlaylistDao
import com.dinaraparanid.prima.databases.daos.CustomPlaylistTrackDao
import com.dinaraparanid.prima.databases.daos.CustomPlaylistAndTrackDao
import com.dinaraparanid.prima.databases.entities.CustomPlaylist
import com.dinaraparanid.prima.databases.entities.CustomPlaylistTrack

@Database(
    entities = [
        CustomPlaylist.Entity::class,
        CustomPlaylistTrack::class
    ],
    version = 2
)
abstract class CustomPlaylistsDatabase : RoomDatabase() {
    abstract fun customPlaylistDao(): CustomPlaylistDao
    abstract fun customPlaylistTrackDao(): CustomPlaylistTrackDao
    abstract fun customPlaylistAndTrackDao(): CustomPlaylistAndTrackDao
}