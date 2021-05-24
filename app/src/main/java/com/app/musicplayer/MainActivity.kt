package com.app.musicplayer

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.app.musicplayer.core.Playlist
import com.app.musicplayer.core.Track
import com.app.musicplayer.fragments.PlayingMenuFragment
import com.app.musicplayer.fragments.TrackDetailFragment
import com.app.musicplayer.fragments.TrackListFragment
import com.app.musicplayer.utils.Colors
import com.app.musicplayer.utils.Params
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import java.util.UUID

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    TrackDetailFragment.Callbacks,
    PlayingMenuFragment.Callbacks {
    private lateinit var drawerLayout: DrawerLayout
    internal lateinit var fragmentContainer: FrameLayout
    internal var actionBarSize = 0
    private var player: MediaPlayer? = MediaPlayer()
    private var playingId: UUID? = null
    var tracks: MutableList<Track> = mutableListOf()
    val curPlaylist = Playlist()
    var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Params.initialize()
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)
            .findViewById<AppBarLayout>(R.id.appbar)
            .findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        /*MusicRepository.getInstance().apply {
            (1..100).forEach { addTrack(Track(title = "Track $it")) }
        }*/

        drawerLayout = findViewById(R.id.drawer_layout)

        fragmentContainer = drawerLayout
            .findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)
            .findViewById(R.id.fragment_container)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        drawerLayout.findViewById<NavigationView>(R.id.nav_view).apply {
            setBackgroundColor(if (Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE)
            itemTextColor = ColorStateList.valueOf(Params.getInstance().theme.rgb)
        }

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, TrackListFragment.newInstance())
                .commit()

        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun onBackPressed() = when {
        drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
        else -> super.onBackPressed()
    }

    override fun onTrackSelected(track: Track, ret: Boolean) {
        if (Params.getInstance().menuPressed)
            return

        isPlaying = when (playingId) {
            null -> true
            track.trackId -> ret
            else -> true
        }

        playingId = track.trackId

        supportFragmentManager.findFragmentByTag("PlayingMenuFragment")?.let {
            val playingMenuFragment = it as PlayingMenuFragment

            when {
                playingMenuFragment.isVisible -> {
                    playingMenuFragment.track = track
                    playingMenuFragment.updateUI()
                }
                else -> {
                    supportFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_up,
                            R.anim.slide_down,
                            R.anim.slide_up,
                            R.anim.slide_down
                        )
                        .add(
                            R.id.fragment_container,
                            PlayingMenuFragment.newInstance(track.trackId, isPlaying),
                            "PlayingMenuFragment"
                        )
                        .commit()
                }
            }
        } ?: run {
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    PlayingMenuFragment.newInstance(track.trackId, isPlaying),
                    "PlayingMenuFragment"
                )
                .commit()
        }

        val sortedTracks = tracks.sortedBy { it.title }
        val end = sortedTracks.takeWhile { it.trackId != track.trackId }

        curPlaylist.apply {
            clear()
            addAll(sortedTracks.dropWhile { it.trackId != track.trackId })
            addAll(end)
        }
    }

    override fun onReturnSelected(track: Track) {
        (fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams)
            .setMargins(0, actionBarSize, 0, 0)

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_down,
                R.anim.slide_up,
                R.anim.slide_down,
                R.anim.slide_up
            )
            .replace(R.id.fragment_container, TrackListFragment.newInstance())
            .commit()

        onTrackSelected(track, true)
    }

    override fun onPlayingToolbarClicked(trackId: UUID) {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_down,
                R.anim.slide_up,
                R.anim.slide_down,
                R.anim.slide_up
            )
            .replace(R.id.fragment_container, TrackDetailFragment.newInstance(trackId, isPlaying))
            .addToBackStack(null)
            .commit()

        supportActionBar!!.hide()

        (fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams)
            .setMargins(0, 0, 0, 0)
    }

    fun setTheme() = setTheme(
        when (Params.getInstance().theme) {
            is Colors.Blue -> R.style.Theme_MusicPlayerBlue
            is Colors.Green -> R.style.Theme_MusicPlayerGreen
            is Colors.GreenTurquoise -> R.style.Theme_MusicPlayerGreenTurquoise
            is Colors.Lemon -> R.style.Theme_MusicPlayerLemon
            is Colors.Orange -> R.style.Theme_MusicPlayerOrange
            is Colors.Pink -> R.style.Theme_MusicPlayerPink
            is Colors.Purple -> R.style.Theme_MusicPlayerPurple
            is Colors.Red -> R.style.Theme_MusicPlayerRed
            is Colors.Sea -> R.style.Theme_MusicPlayerSea
            is Colors.Turquoise -> R.style.Theme_MusicPlayerTurquoise
            is Colors.BlueNight -> R.style.Theme_MusicPlayerBlueNight
            is Colors.GreenNight -> R.style.Theme_MusicPlayerGreenNight
            is Colors.GreenTurquoiseNight -> R.style.Theme_MusicPlayerGreenTurquoiseNight
            is Colors.LemonNight -> R.style.Theme_MusicPlayerLemonNight
            is Colors.OrangeNight -> R.style.Theme_MusicPlayerOrangeNight
            is Colors.PinkNight -> R.style.Theme_MusicPlayerPinkNight
            is Colors.PurpleNight -> R.style.Theme_MusicPlayerPurpleNight
            is Colors.RedNight -> R.style.Theme_MusicPlayerRedNight
            is Colors.SeaNight -> R.style.Theme_MusicPlayerSeaNight
            is Colors.TurquoiseNight -> R.style.Theme_MusicPlayerTurquoiseNight
            else -> throw IllegalStateException("Wrong theme")
        }
    )

    /* TODO: Not yet ready for usage

    private fun playTrack(rootToTrack: String) {
        if (player != null && player!!.isPlaying) {
            clearMediaPlayer()
            trackBar.progress = 0
            isPlaying = true
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pause
                )
            )
        }

        if (!isPlaying) {
            playButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.play
                )
            )

            val descriptor = assets.openFd(rootToTrack)

            player!!.setDataSource(
                descriptor.fileDescriptor,
                descriptor.startOffset,
                descriptor.length
            )

            descriptor.close()

            player!!.apply {
                setVolume(0.5f, 0.5f)
                isLooping = false
            }

            player!!.prepare()
            trackBar.max = player!!.duration
            player!!.start()
            Thread(this).start()
        }

        isPlaying = false
    }

    override fun run() {
        var currentPosition = player!!.currentPosition
        val total = player!!.duration

        while (player != null && player!!.isPlaying && currentPosition < total) {
            currentPosition = try {
                Thread.sleep(1000)
                player!!.currentPosition
            } catch (e: Exception) {
                return
            }

            trackBar.progress = currentPosition
        }
    }

    private fun clearMediaPlayer() {
        player!!.stop()
        player!!.release()
        player = null
    }*/
}