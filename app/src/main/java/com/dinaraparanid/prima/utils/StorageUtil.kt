package com.dinaraparanid.prima.utils

import android.content.Context
import android.content.SharedPreferences
import com.dinaraparanid.prima.utils.polymorphism.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.utils.extensions.toPlaylist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manipulates [SharedPreferences] data for app
 */

internal class StorageUtil(private val context: Context) {
    companion object {
        private const val STORAGE = "com.dinaraparanid.prima.STORAGE"
        private const val TRACK_LIST_KEY = "track_list"
        private const val TRACK_PATH_KEY = "track_path"
        private const val PAUSE_TIME_KEY = "pause_time"
        private const val LOOPING_KEY = "looping"
        private const val CURRENT_PLAYLIST_KEY = "current_p"
        private const val CHANGED_TRACKS_KEY = "changed_tracks"
        private const val LANGUAGE_KEY = "language"
        private const val THEME_KEY = "theme"
        private const val SAVE_PROGRESS_KEY = "save_progress"
        private const val ROUNDED_PLAYLIST_KEY = "round"
    }

    private var preferences: SharedPreferences? = null

    /**
     * Saves tracks in [SharedPreferences]
     * @param trackList track list to save
     */

    fun storeTracks(trackList: List<Track?>?) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_LIST_KEY, Gson().toJson(trackList))
            apply()
        }

    /**
     * Loads tracks from [SharedPreferences]
     * @return loaded tracks
     */

    fun loadTracks(): List<Track> = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(TRACK_LIST_KEY, null),
        (object : TypeToken<List<Track?>?>() {}).type
    )

    /**
     * Saves current track's path in [SharedPreferences]
     * @param path path to track (DATA column from MediaStore)
     */

    fun storeTrackPath(path: String) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(TRACK_PATH_KEY, path)
            apply()
        }

    /**
     * Loads current track's path from [SharedPreferences]
     * @return current track's path or [com.dinaraparanid.prima.MainActivity.NO_PATH]
     */

    fun loadTrackPath() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getString(TRACK_PATH_KEY, "_____ЫЫЫЫЫЫЫЫ_____")!!

    /**
     * Saves current track's pause time in [SharedPreferences]
     * @param pause pause time
     */

    fun storeTrackPauseTime(pause: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(PAUSE_TIME_KEY, pause)
            apply()
        }

    /**
     * Loads current track's pause time from [SharedPreferences]
     * @return current track's pause time or -1 if it wasn't saved
     */

    fun loadTrackPauseTime() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getInt(PAUSE_TIME_KEY, -1)

    /**
     * Saves looping in [SharedPreferences]
     * @param isLooping looping when playing track
     */

    fun storeLooping(isLooping: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(LOOPING_KEY, isLooping)
            apply()
        }

    /**
     * Loads looping from [SharedPreferences]
     * @return looping when playing track or false if it wasn't saved
     */

    fun loadLooping() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(LOOPING_KEY, false)

    /**
     * Saves current playlist in [SharedPreferences]
     * @param curPlaylist current playlist to save
     */

    fun storeCurPlaylist(curPlaylist: Playlist) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CURRENT_PLAYLIST_KEY, Gson().toJson(curPlaylist))
            apply()
        }

    /**
     * Loads current playlist from [SharedPreferences]
     * @return current playlist or null if it wasn't save or even created
     */

    fun loadCurPlaylist() = Gson().fromJson<List<Track>>(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CURRENT_PLAYLIST_KEY, null),
        (object : TypeToken<List<Track?>?>() {}).type
    )?.toPlaylist()

    /**
     * Saves current playlist in [SharedPreferences] (Android 11+)
     * @since Android 11
     * @param changedTracks dictionary with changed tracks
     */

    fun storeChangedTracks(changedTracks: MutableMap<String, Track>) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putString(CHANGED_TRACKS_KEY, Gson().toJson(changedTracks))
            apply()
        }

    /**
     * Loads current playlist from [SharedPreferences] (Android 11+)
     * @since Android 11
     * @return dictionary with changed tracks or null if it wasn't save or even created
     */

    fun loadChangedTracks(): MutableMap<String, Track>? = Gson().fromJson(
        context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getString(CHANGED_TRACKS_KEY, null),
        (object : TypeToken<MutableMap<String, Track>?>() {}).type
    )

    /**
     * Saves current language in [SharedPreferences]
     * @param language [Params.Companion.Language] to save
     */

    fun storeLanguage(language: Params.Companion.Language) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(LANGUAGE_KEY, language.ordinal)
            apply()
        }

    /**
     * Loads current language from [SharedPreferences]
     * @return [Params.Companion.Language] that was chosen before
     * or [Params.Companion.Language.EN] as a default language if it wasn't
     */

    fun loadLanguage() = Params.Companion.Language.values().getOrNull(
        context
            .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getInt(LANGUAGE_KEY, -1)
    )

    /**
     * Saves current theme in [SharedPreferences]
     * @param theme number of theme to save
     * @see Params.chooseTheme
     */

    fun storeTheme(theme: Int) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putInt(THEME_KEY, theme)
            apply()
        }

    /**
     * Loads theme from [SharedPreferences]
     * @return [Colors] of theme that was chosen before
     * or [Colors.PurpleNight] as a default theme if it wasn't
     */

    fun loadTheme() = Params.chooseTheme(
        context
            .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
            .getInt(THEME_KEY, 1)
    )

    /**
     * Saves flag about saving playing progress in [SharedPreferences]
     * @param isSavingProgress saving playing progress flag to save
     */

    fun storeSaveProgress(isSavingProgress: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(SAVE_PROGRESS_KEY, isSavingProgress)
            apply()
        }

    /**
     * Loads flag about saving playing progress from [SharedPreferences]
     * @return saving playing progress flag or false if it's wasn't saved
     */

    fun loadSaveProgress() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(SAVE_PROGRESS_KEY, true)

    /**
     * Saves flag about rounding playlists' images in [SharedPreferences]
     * @param isRounded saving rounding playlists' images flag to save
     */

    fun storeRounded(isRounded: Boolean) = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!.edit().run {
            putBoolean(ROUNDED_PLAYLIST_KEY, isRounded)
            apply()
        }

    /**
     * Loads flag about rounding playlists' images from [SharedPreferences]
     * @return saving rounding playlists' images flag or true if it's wasn't saved
     */

    fun loadRounded() = context
        .getSharedPreferences(STORAGE, Context.MODE_PRIVATE)!!
        .getBoolean(ROUNDED_PLAYLIST_KEY, true)

    /**
     * Clears playlist data in [SharedPreferences]
     */

    fun clearCachedPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(TRACK_LIST_KEY)
            apply()
        }
    }

    /**
     * Clears playing progress in [SharedPreferences]
     */

    fun clearProgress() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE)
        preferences!!.edit().apply {
            remove(TRACK_PATH_KEY)
            remove(PAUSE_TIME_KEY)
            remove(LOOPING_KEY)
            remove(CURRENT_PLAYLIST_KEY)
            apply()
        }
    }
}