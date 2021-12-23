package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.utils.extensions.fixedImageUrl
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Album(
    @Expose
    @JvmField
    @SerializedName("api_path")
    val apiPath: String,

    @Expose
    @JvmField
    @SerializedName("cover_art_url")
    val _coverArtUrl: String? = null,

    @Expose
    @JvmField
    @SerializedName("full_title")
    val fullTitle: String,

    @Expose
    @JvmField
    val id: String,

    @Expose
    @JvmField
    var name: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    val artist: Artist
) : Serializable {
    internal inline val coverArtUrl
        get() = _coverArtUrl?.fixedImageUrl
}