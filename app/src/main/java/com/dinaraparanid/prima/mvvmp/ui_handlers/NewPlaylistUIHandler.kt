package com.dinaraparanid.prima.mvvmp.ui_handlers

import android.content.DialogInterface
import com.dinaraparanid.prima.databases.entities.custom.CustomPlaylist
import com.dinaraparanid.prima.databases.repositories.CustomPlaylistsRepository
import com.dinaraparanid.prima.utils.Statistics
import com.dinaraparanid.prima.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** [InputDialogUIHandler] for NewPlaylistDialog */

class NewPlaylistUIHandler : InputDialogUIHandler<NewPlaylistUIHandler.Args> {
    @JvmInline
    value class Args(val updateFragmentChannel: Channel<Unit>) :
        InputDialogUIHandler.Args

    override suspend fun Args.onOkAsync(
        input: String,
        dialog: DialogInterface
    ) {
        coroutineScope {
            launch(Dispatchers.IO) {
                StorageUtil.runAsyncSynchronized {
                    storeStatistics(
                        loadStatistics()
                            ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                            ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                    )

                    storeStatisticsDaily(
                        loadStatisticsDaily()
                            ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                            ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                    )

                    storeStatisticsWeekly(
                        loadStatisticsWeekly()
                            ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                            ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                    )

                    storeStatisticsMonthly(
                        loadStatisticsMonthly()
                            ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                            ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                    )

                    storeStatisticsYearly(
                        loadStatisticsYearly()
                            ?.let(Statistics::withIncrementedNumberOfCreatedPlaylists)
                            ?: Statistics.empty.withIncrementedNumberOfCreatedPlaylists
                    )
                }
            }

            launch(Dispatchers.IO) {
                CustomPlaylistsRepository
                    .getInstanceSynchronized()
                    .addPlaylistsAsync(CustomPlaylist.Entity(0, input))
                    .join()

                updateFragmentChannel.send(Unit)
            }
        }
    }
}