package com.dinaraparanid.prima.utils.web.genius

import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.web.genius.search_response.Stats
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/** Track's data itself */

data class GeniusTrack(
    @Expose
    @JvmField
    @SerializedName("annotation_count")
    val annotationCount: Int,

    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    @SerializedName("full_title")
    val fullTitle: String,

    @Expose
    @JvmField
    @SerializedName("header_image_thumbnail_url")
    val headerImageThumbnailUrl: String,

    @Expose
    @JvmField
    @SerializedName("header_image_url")
    val headerImageUrl: String,

    @Expose
    @JvmField
    val id: Long,

    @Expose
    @JvmField
    @SerializedName("lyrics_owner_id")
    val lyricsOwnerId: Long,

    @Expose
    @JvmField
    @SerializedName("lyrics_state")
    val lyricsState: String,

    @Expose
    @JvmField
    @SerializedName("path")
    val pathToLyrics: String,

    @Expose
    @JvmField
    @SerializedName("pyongs_count")
    val pyongsCount: Long,

    @Expose
    @JvmField
    @SerializedName("song_art_image_thumbnail_url")
    val songArtImageThumbnailUrl: String,

    @Expose
    @JvmField
    @SerializedName("song_art_image_url")
    val songArtImageUrl: String,

    @Expose
    @JvmField
    val stats: Stats,

    @Expose
    @JvmField
    @SerializedName("title")
    val geniusTitle: String,

    @Expose
    @JvmField
    @SerializedName("title_with_featured")
    val titleWithFeatured: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("primary_artist")
    val primaryArtist: Artist
) : AbstractTrack(
    0,
    geniusTitle,
    primaryArtist.name,
    "",
    Params.NO_PATH,
    0, null, null, 0
) {
    override val title: String
        get() = geniusTitle

    override val artist: String
        get() = primaryArtist.name
}