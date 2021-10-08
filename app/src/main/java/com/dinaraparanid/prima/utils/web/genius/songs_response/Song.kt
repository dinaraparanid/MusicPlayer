package com.dinaraparanid.prima.utils.web.genius.songs_response

import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.web.genius.Artist
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Song(
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
    @SerializedName("apple_music_id")
    val appleMusicId: Long? = null,

    @Expose
    @JvmField
    @SerializedName("apple_music_player_url")
    val appleMusicPlayerUrl: String,

    @Expose
    @JvmField
    val description: Description,

    @Expose
    @JvmField
    @SerializedName("embed_content")
    val embedContent: String,

    @Expose
    @JvmField
    @SerializedName("featured_video")
    val featuredVideo: Boolean,

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
    @SerializedName("lyrics_placeholder_reason")
    val lyricsPlaceholderReason: String? = null,

    @Expose
    @JvmField
    @SerializedName("lyrics_state")
    val lyricsState: String,

    @Expose
    @JvmField
    val path: String,

    @Expose
    @JvmField
    @SerializedName("pyongs_count")
    val pyongsCount: Int? = null,

    @Expose
    @JvmField
    @SerializedName("recording_location")
    val recordingLocation: String? = null,

    @Expose
    @JvmField
    @SerializedName("release_date")
    val releaseDate: String? = null,

    @Expose
    @JvmField
    @SerializedName("release_date_for_display")
    val releaseDateForDisplay: String,

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
    val title: String,

    @Expose
    @JvmField
    @SerializedName("title_with_featured")
    val titleWithFeatured: String,

    @Expose
    @JvmField
    val url: String,

    @Expose
    @JvmField
    @SerializedName("current_user_metadata")
    val currentUserMetadata: CurrentUserMetadata,

    @Expose
    @JvmField
    val album: Album? = null,

    @Expose
    @JvmField
    @SerializedName("custom_performances")
    val customPerformances: Array<CustomPerformance>,

    @Expose
    @JvmField
    @SerializedName("description_annotation")
    val descriptionAnnotation: DescriptionAnnotation,

    @Expose
    @JvmField
    @SerializedName("featured_artists")
    val featuredArtists: Array<Artist>,

    @Expose
    @JvmField
    @SerializedName("lyrics_marked_complete_by")
    val lyricsMarkedCompleteBy: Any? = null,

    @Expose
    @JvmField
    @SerializedName("media")
    val media: Array<MediaData>,

    @Expose
    @JvmField
    @SerializedName("primary_artist")
    val primaryArtist: Artist,

    @Expose
    @JvmField
    @SerializedName("producer_artists")
    val producerArtists: Array<Artist>,

    @Expose
    @JvmField
    @SerializedName("song_relationships")
    val songRelationships: Array<SongRelationship>,

    @Expose
    @JvmField
    @SerializedName("verified_annotations_by")
    val verifiedAnnotationsBy: Array<*>,

    @Expose
    @JvmField
    @SerializedName("verified_contributors")
    val verifiedContributors: Array<*>,

    @Expose
    @JvmField
    @SerializedName("verified_lyrics_by")
    val verifiedLyricsBy: Array<*>,

    @Expose
    @JvmField
    @SerializedName("writer_artists")
    val writerArtists: Array<Artist>
) : Serializable {
    internal inline val youTubeUrl
        get() = media.find { it.provider == "youtube" }

    @JvmField
    internal val hasYouTubeUrl = youTubeUrl != null

    private inline val unknown
        get() = Params.instance.application.unchecked.resources.getString(R.string.unknown)

    private inline val none
        get() = Params.instance.application.unchecked.resources.getString(R.string.none)

    private inline val Array<Artist>.concatenatedOrUnknown
        get() = this
            .takeIf(Array<*>::isNotEmpty)
            ?.fold("") { acc, artist -> if (acc.isEmpty()) artist.name else "$acc, ${artist.name}" }
            ?: none

    internal inline val artistAlbumFormatted
        @JvmName("getArtistAlbumFormatted")
        get() = "${primaryArtist.name}${album?.name.let { " / $it" }}"

    internal inline val albumOrUnknown
        @JvmName("getAlbumOrUnknown")
        get() = album?.name ?: unknown

    internal inline val releaseDateOrUnknown
        @JvmName("getReleaseDateOrUnknown")
        get() = releaseDate ?: unknown

    internal inline val recordingLocationOrUnknown
        @JvmName("getRecordingLocationOrUnknown")
        get() = recordingLocation ?: unknown

    internal inline val producerArtistsOrUnknown
        @JvmName("getProducerArtistOrUnknown")
        get() = producerArtists.concatenatedOrUnknown

    internal inline val featuredArtistsOrUnknown
        @JvmName("getFeaturedArtistOrUnknown")
        get() = featuredArtists.concatenatedOrUnknown

    internal inline val writerArtistsOrUnknown
        @JvmName("getWriterArtistOrUnknown")
        get() = writerArtists.concatenatedOrUnknown

    internal inline val youTubeUrlOrNone
        @JvmName("getYouTubeUrlOrNone")
        get() = youTubeUrl?.let(MediaData::url) ?: none
}