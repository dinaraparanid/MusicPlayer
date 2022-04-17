package com.dinaraparanid.prima.databases.entities.statistics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dinaraparanid.prima.core.Artist

/** Author's statistics */

@Entity(tableName = "statistics_artists")
data class StatisticsArtist(
    @PrimaryKey
    override val name: String,

    // How many times he's listened
    val count: Long = 1,
    @ColumnInfo(name = "count_daily") val countDaily: Long = 1,
    @ColumnInfo(name = "count_weekly") val countWeekly: Long = 1,
    @ColumnInfo(name = "count_monthly") val countMonthly: Long = 1,
    @ColumnInfo(name = "count_yearly") val countYearly: Long = 1
) : Artist(name) {
    constructor(artist: Artist) : this(artist.name)
}