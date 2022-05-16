package com.dinaraparanid.prima.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.dinaraparanid.prima.GuessTheMelodyActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.databinding.DialogGtmStartParamsBinding
import com.dinaraparanid.prima.fragments.guess_the_melody.GTMPlaylistSelectFragment
import com.dinaraparanid.prima.utils.extensions.unchecked
import com.dinaraparanid.prima.utils.polymorphism.AbstractPlaylist
import com.dinaraparanid.prima.utils.polymorphism.AbstractTrack
import com.dinaraparanid.prima.viewmodels.mvvm.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Dialog to set params for game (amount of tracks and maximum playback time).
 * Tracks amount must be smaller than 9999, playback limit is 99 seconds.
 *
 * @param playlist playlist in which tracks will be guessed
 * @param fragment current [GTMPlaylistSelectFragment]
 */

class GuessTheMelodyStartParamsDialog(
    private val playlist: AbstractPlaylist,
    private val fragment: WeakReference<GTMPlaylistSelectFragment>
) : DialogFragment(), CoroutineScope by MainScope() {
    private var dialogBinding: DialogGtmStartParamsBinding? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogBinding = DataBindingUtil.inflate<DialogGtmStartParamsBinding>(
            layoutInflater,
            R.layout.dialog_gtm_start_params,
            null, false
        ).apply { viewModel = ViewModel() }

        return AlertDialog.Builder(requireContext())
            .setView(dialogBinding!!.root)
            .setCancelable(true)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                launch(Dispatchers.IO) {
                    val gamePlaylist = when (playlist.type) {
                        AbstractPlaylist.PlaylistType.ALBUM -> {
                            mutableListOf<AbstractTrack>().apply {
                                val app = requireActivity().application as MainApplication
                                val task1 = app.getAlbumTracksAsync(playlist.title)
                                val task2 = app.getAlbumTracksAsync(playlist.title.lowercase())
                                val task3 = app.getAlbumTracksAsync("${playlist.title} ")
                                val task4 = app.getAlbumTracksAsync("${playlist.title} ".lowercase())

                                addAll(task1.await())
                                addAll(task2.await())
                                addAll(task3.await())
                                addAll(task4.await())
                            }
                        }

                        AbstractPlaylist.PlaylistType.CUSTOM ->
                            CustomPlaylistsRepository
                                .getInstanceSynchronized()
                                .getTracksOfPlaylistAsync(playlist.title)
                                .await()

                        else -> throw IllegalArgumentException(
                            "GTM Playlist should not be used with GuessTheMelodyStartParamsDialog"
                        )
                    }

                    when {
                        dialogBinding!!
                            .gtmTracksAmount.text.toString()
                            .toIntOrNull()
                            ?.let { it > 3 } != true -> launch(Dispatchers.Main) {
                            Toast.makeText(
                                fragment.unchecked.requireContext().applicationContext,
                                R.string.track_number_error,
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                            dialogBinding = null
                        }

                        dialogBinding!!
                            .gtmPlaybackLen.text.toString()
                            .toByteOrNull()
                            ?.let { it > 0 } != true -> launch(Dispatchers.Main) {
                            Toast.makeText(
                                fragment.unchecked.requireContext().applicationContext,
                                R.string.playback_time_error,
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                            dialogBinding = null
                        }

                        gamePlaylist.size < 4 -> launch(Dispatchers.Main) {
                            Toast.makeText(
                                fragment.unchecked.requireContext().applicationContext,
                                R.string.game_playlist_small,
                                Toast.LENGTH_LONG
                            ).show()

                            dialog.dismiss()
                            dialogBinding = null
                        }

                        else -> {
                            fragment.unchecked.startActivity(
                                Intent(
                                    fragment.unchecked.requireContext().applicationContext,
                                    GuessTheMelodyActivity::class.java
                                ).apply {
                                    putExtra(
                                        GuessTheMelodyActivity.PLAYLIST_KEY,
                                        gamePlaylist
                                            .shuffled()
                                            .take(dialogBinding!!.gtmTracksAmount.text.toString().toInt())
                                            .toTypedArray()
                                    )

                                    putExtra(
                                        GuessTheMelodyActivity.MAX_PLAYBACK_LENGTH_KEY,
                                        dialogBinding!!.gtmPlaybackLen.text.toString().toByte()
                                    )
                                }
                            )

                            dialog.dismiss()
                            dialogBinding = null

                            fragment.unchecked.requireActivity()
                                .supportFragmentManager
                                .popBackStack()
                        }
                    }

                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
    }
}