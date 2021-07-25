package com.dinaraparanid.prima.fragments

import android.provider.MediaStore
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.utils.polymorphism.*
import kotlinx.coroutines.*

/**
 * [ArtistListFragment] for all artists on user's device
 */

class DefaultArtistListFragment : ArtistListFragment() {
    override suspend fun loadAsync(): Deferred<Unit> = coroutineScope {
        async(Dispatchers.IO) {
            try {
                requireActivity().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Artists.ARTIST),
                    null,
                    null,
                    MediaStore.Audio.Media.ARTIST + " ASC"
                ).use { cursor ->
                    itemList.clear()

                    if (cursor != null) {
                        val artistList = mutableListOf<Artist>()

                        while (cursor.moveToNext())
                            artistList.add(Artist(cursor.getString(0)))

                        itemList.addAll(artistList.distinctBy { it.name })
                    }
                }
            } catch (ignored: Exception) {
                // Permission to storage not given
            }
        }
    }
}