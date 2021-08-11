package com.dinaraparanid.prima.databases.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.utils.polymorphism.ImageEntity

/**
 * Entity for albums' images
 */

@Entity(tableName = "image_albums")
data class AlbumImage(
    @PrimaryKey val title: String,
    override val image: ByteArray
) : ImageEntity {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistImage) return false

        if (title != other.title) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}