package com.dinaraparanid.prima

import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import arrow.core.None
import arrow.core.Some
import com.dinaraparanid.prima.core.Artist
import com.dinaraparanid.prima.core.Playlist
import com.dinaraparanid.prima.core.Track
import com.dinaraparanid.prima.fragments.*
import com.dinaraparanid.prima.utils.Colors
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.unwrap
import com.dinaraparanid.prima.viewmodels.MainActivityViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity :
    AppCompatActivity(),
    TrackListFragment.Callbacks,
    ArtistListFragment.Callbacks,
    NavigationView.OnNavigationItemSelectedListener {
    private lateinit var playingPart: ConstraintLayout
    internal lateinit var mainLabel: TextView

    private lateinit var trackLayout: ConstraintLayout
    private lateinit var settingsButton: ImageButton
    private lateinit var albumImage: ImageView
    private lateinit var trackPlayingBar: SeekBar
    private lateinit var curTime: TextView
    private lateinit var trackLength: TextView
    private lateinit var trackTitle: TextView
    private lateinit var artistsAlbum: TextView
    private lateinit var playButton: ImageButton
    private lateinit var prevTrackButton: ImageButton
    private lateinit var nextTrackButton: ImageButton
    private lateinit var likeButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var playlistButton: ImageButton
    private lateinit var trackLyricsButton: ImageButton
    private lateinit var returnButton: ImageButton

    private lateinit var appBarLayout: AppBarLayout
    private lateinit var playingToolbar: Toolbar
    private lateinit var albumImageSmall: CircleImageView
    private lateinit var trackTitleSmall: TextView
    private lateinit var trackArtists: TextView
    private lateinit var playButtonSmall: ImageButton
    private lateinit var prevTrackButtonSmall: ImageButton
    private lateinit var nextTrackButtonSmall: ImageButton

    internal val mainActivityViewModel: MainActivityViewModel by lazy {
        ViewModelProvider(this)[MainActivityViewModel::class.java]
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var fragmentContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        Params.initialize()
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityViewModel.load(
            savedInstanceState?.getSerializable("track_id") as Long?,
            savedInstanceState?.getBoolean("is_playing"),
            savedInstanceState?.getSerializable("cur_playlist") as Playlist?,
            savedInstanceState?.getInt("sheet_behavior_state"),
            savedInstanceState?.getSerializable("all_tracks") as Playlist?
        )

        if (mainActivityViewModel.allTracksLiveData.value!!.isEmpty())
            loadStorageData()

        appBarLayout = findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)
            .findViewById(R.id.appbar)

        val toolbar = appBarLayout.findViewById<Toolbar>(R.id.toolbar)
        mainLabel = toolbar.findViewById(R.id.main_label)
        setSupportActionBar(toolbar)

        /*MusicRepository.getInstance().apply {
            (1..100).forEach { addTrack(Track(title = "Track $it")) }
        }*/

        /*MusicRepository.getInstance().apply {
            (1..100).forEach { addArtist(Artist(name = "Artist $it")) }
        }*/

        drawerLayout = findViewById(R.id.drawer_layout)

        val mainCoordinatorLayout = drawerLayout
            .findViewById<CoordinatorLayout>(R.id.main_coordinator_layout)

        playingPart = mainCoordinatorLayout
            .findViewById<ConstraintLayout>(R.id.playing)
            .apply { isVisible = false }

        playingToolbar = playingPart.findViewById(R.id.playing_toolbar)
        val playingLayout = playingToolbar.findViewById<ConstraintLayout>(R.id.playing_layout)

        albumImageSmall = playingLayout.findViewById(R.id.playing_album_image)
        trackTitleSmall = playingLayout.findViewById(R.id.playing_track_title)
        trackArtists = playingLayout.findViewById(R.id.playing_track_artists)
        playButtonSmall = playingLayout.findViewById(R.id.playing_play_button)
        prevTrackButtonSmall = playingLayout.findViewById(R.id.playing_prev_track)
        nextTrackButtonSmall = playingLayout.findViewById(R.id.playing_next_track)

        trackLayout = playingPart.findViewById(R.id.track_layout)
        val primaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.primary_buttons)
        val secondaryButtons = trackLayout.findViewById<ConstraintLayout>(R.id.secondary_buttons)

        settingsButton = trackLayout.findViewById(R.id.track_settings_button)
        albumImage = trackLayout.findViewById(R.id.album_picture)
        trackPlayingBar = trackLayout.findViewById(R.id.track_playing_bar)
        curTime = trackLayout.findViewById(R.id.current_time)
        trackLength = trackLayout.findViewById(R.id.track_length)
        trackTitle = trackLayout.findViewById(R.id.track_title_big)
        artistsAlbum = trackLayout.findViewById(R.id.artists_album)
        playButton = primaryButtons.findViewById(R.id.play_button)
        prevTrackButton = primaryButtons.findViewById(R.id.previous_track_button)
        nextTrackButton = primaryButtons.findViewById(R.id.next_track_button)
        likeButton = secondaryButtons.findViewById(R.id.like_button)
        repeatButton = secondaryButtons.findViewById(R.id.repeat_button)
        playlistButton = secondaryButtons.findViewById(R.id.playlist_button)
        trackLyricsButton = secondaryButtons.findViewById(R.id.track_lyrics)
        returnButton = trackLayout.findViewById(R.id.return_button)

        curTime.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        trackLength.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        trackTitle.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)
        artistsAlbum.setTextColor(if (Params.getInstance().theme.isNight) Color.WHITE else Color.BLACK)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            0
        )

        returnButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.arrow_blue
                is Colors.BlueNight -> R.drawable.arrow_blue
                is Colors.Green -> R.drawable.arrow_green
                is Colors.GreenNight -> R.drawable.arrow_green
                is Colors.GreenTurquoise -> R.drawable.arrow_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.arrow_green_turquoise
                is Colors.Lemon -> R.drawable.arrow_lemon
                is Colors.LemonNight -> R.drawable.arrow_lemon
                is Colors.Orange -> R.drawable.arrow_orange
                is Colors.OrangeNight -> R.drawable.arrow_orange
                is Colors.Pink -> R.drawable.arrow_pink
                is Colors.PinkNight -> R.drawable.arrow_pink
                is Colors.Purple -> R.drawable.arrow_purple
                is Colors.PurpleNight -> R.drawable.arrow_purple
                is Colors.Red -> R.drawable.arrow_red
                is Colors.RedNight -> R.drawable.arrow_red
                is Colors.Sea -> R.drawable.arrow_sea
                is Colors.SeaNight -> R.drawable.arrow_sea
                is Colors.Turquoise -> R.drawable.arrow_turquoise
                is Colors.TurquoiseNight -> R.drawable.arrow_turquoise
                else -> R.drawable.arrow
            }
        )

        nextTrackButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.next_track_blue
                is Colors.BlueNight -> R.drawable.next_track_blue
                is Colors.Green -> R.drawable.next_track_green
                is Colors.GreenNight -> R.drawable.next_track_green
                is Colors.GreenTurquoise -> R.drawable.next_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.next_track_green_turquoise
                is Colors.Lemon -> R.drawable.next_track_lemon
                is Colors.LemonNight -> R.drawable.next_track_lemon
                is Colors.Orange -> R.drawable.next_track_orange
                is Colors.OrangeNight -> R.drawable.next_track_orange
                is Colors.Pink -> R.drawable.next_track_pink
                is Colors.PinkNight -> R.drawable.next_track_pink
                is Colors.Purple -> R.drawable.next_track_purple
                is Colors.PurpleNight -> R.drawable.next_track_purple
                is Colors.Red -> R.drawable.next_track_red
                is Colors.RedNight -> R.drawable.next_track_red
                is Colors.Sea -> R.drawable.next_track_sea
                is Colors.SeaNight -> R.drawable.next_track_sea
                is Colors.Turquoise -> R.drawable.next_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.next_track_turquoise
                else -> R.drawable.next_track
            }
        )

        prevTrackButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.prev_track_blue
                is Colors.BlueNight -> R.drawable.prev_track_blue
                is Colors.Green -> R.drawable.prev_track_green
                is Colors.GreenNight -> R.drawable.prev_track_green
                is Colors.GreenTurquoise -> R.drawable.prev_track_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.prev_track_green_turquoise
                is Colors.Lemon -> R.drawable.prev_track_lemon
                is Colors.LemonNight -> R.drawable.prev_track_lemon
                is Colors.Orange -> R.drawable.prev_track_orange
                is Colors.OrangeNight -> R.drawable.prev_track_orange
                is Colors.Pink -> R.drawable.prev_track_pink
                is Colors.PinkNight -> R.drawable.prev_track_pink
                is Colors.Purple -> R.drawable.prev_track_purple
                is Colors.PurpleNight -> R.drawable.prev_track_purple
                is Colors.Red -> R.drawable.prev_track_red
                is Colors.RedNight -> R.drawable.prev_track_red
                is Colors.Sea -> R.drawable.prev_track_sea
                is Colors.SeaNight -> R.drawable.prev_track_sea
                is Colors.Turquoise -> R.drawable.prev_track_turquoise
                is Colors.TurquoiseNight -> R.drawable.prev_track_turquoise
                else -> R.drawable.prev_track
            }
        )

        playlistButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.playlist_blue
                is Colors.BlueNight -> R.drawable.playlist_blue
                is Colors.Green -> R.drawable.playlist_green
                is Colors.GreenNight -> R.drawable.playlist_green
                is Colors.GreenTurquoise -> R.drawable.playlist_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.playlist_green_turquoise
                is Colors.Lemon -> R.drawable.playlist_lemon
                is Colors.LemonNight -> R.drawable.playlist_lemon
                is Colors.Orange -> R.drawable.playlist_orange
                is Colors.OrangeNight -> R.drawable.playlist_orange
                is Colors.Pink -> R.drawable.playlist_pink
                is Colors.PinkNight -> R.drawable.playlist_pink
                is Colors.Purple -> R.drawable.playlist_purple
                is Colors.PurpleNight -> R.drawable.playlist_purple
                is Colors.Red -> R.drawable.playlist_red
                is Colors.RedNight -> R.drawable.playlist_red
                is Colors.Sea -> R.drawable.playlist_sea
                is Colors.SeaNight -> R.drawable.playlist_sea
                is Colors.Turquoise -> R.drawable.playlist_turquoise
                is Colors.TurquoiseNight -> R.drawable.playlist_turquoise
                else -> R.drawable.playlist
            }
        )

        trackLyricsButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.text_blue
                is Colors.BlueNight -> R.drawable.text_blue
                is Colors.Green -> R.drawable.text_green
                is Colors.GreenNight -> R.drawable.text_green
                is Colors.GreenTurquoise -> R.drawable.text_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.text_green_turquoise
                is Colors.Lemon -> R.drawable.text_lemon
                is Colors.LemonNight -> R.drawable.text_lemon
                is Colors.Orange -> R.drawable.text_orange
                is Colors.OrangeNight -> R.drawable.text_orange
                is Colors.Pink -> R.drawable.text_pink
                is Colors.PinkNight -> R.drawable.text_pink
                is Colors.Purple -> R.drawable.text_purple
                is Colors.PurpleNight -> R.drawable.text_purple
                is Colors.Red -> R.drawable.text_red
                is Colors.RedNight -> R.drawable.text_red
                is Colors.Sea -> R.drawable.text_sea
                is Colors.SeaNight -> R.drawable.text_sea
                is Colors.Turquoise -> R.drawable.text_turquoise
                is Colors.TurquoiseNight -> R.drawable.text_turquoise
                else -> R.drawable.text
            }
        )

        settingsButton.setImageResource(
            when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.three_dots_blue
                is Colors.BlueNight -> R.drawable.three_dots_blue
                is Colors.Green -> R.drawable.three_dots_green
                is Colors.GreenNight -> R.drawable.three_dots_green
                is Colors.GreenTurquoise -> R.drawable.three_dots_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.three_dots_green_turquoise
                is Colors.Lemon -> R.drawable.three_dots_lemon
                is Colors.LemonNight -> R.drawable.three_dots_lemon
                is Colors.Orange -> R.drawable.three_dots_orange
                is Colors.OrangeNight -> R.drawable.three_dots_orange
                is Colors.Pink -> R.drawable.three_dots_pink
                is Colors.PinkNight -> R.drawable.three_dots_pink
                is Colors.Purple -> R.drawable.three_dots_purple
                is Colors.PurpleNight -> R.drawable.three_dots_purple
                is Colors.Red -> R.drawable.three_dots_red
                is Colors.RedNight -> R.drawable.three_dots_red
                is Colors.Sea -> R.drawable.three_dots_sea
                is Colors.SeaNight -> R.drawable.three_dots_sea
                is Colors.Turquoise -> R.drawable.three_dots_turquoise
                is Colors.TurquoiseNight -> R.drawable.three_dots_turquoise
                else -> R.drawable.three_dots
            }
        )

        setPlayButtonSmallImage()
        setLikeButtonImage()
        setPlayButtonImage()
        setRepeatButtonImage()

        playingToolbar.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        playButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                mainActivityViewModel.isPlayingLiveData.value =
                    !mainActivityViewModel.isPlayingLiveData.value!!
                setPlayButtonSmallImage()
                // TODO: Track playing
            }
        }

        prevTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                mainActivityViewModel.curPlaylistLiveData.value!!.goToPrevTrack()
                updateUI()
                mainActivityViewModel.isPlayingLiveData.value = true
                setPlayButtonSmallImage()
            }
        }

        nextTrackButtonSmall.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                mainActivityViewModel.curPlaylistLiveData.value!!.goToNextTrack()
                mainActivityViewModel.isPlayingLiveData.value = true
                updateUI()
                setPlayButtonImage()
            }
        }

        albumImageSmall.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        trackTitleSmall.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        trackArtists.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        playButton.setOnClickListener {
            mainActivityViewModel.isPlayingLiveData.value =
                !mainActivityViewModel.isPlayingLiveData.value!!
            setPlayButtonImage()
            // TODO: Track playing
        }

        nextTrackButton.setOnClickListener {
            mainActivityViewModel.curPlaylistLiveData.value!!.goToNextTrack()
            mainActivityViewModel.isPlayingLiveData.value = true
            updateUI()
            setPlayButtonImage()
        }

        prevTrackButton.setOnClickListener {
            mainActivityViewModel.curPlaylistLiveData.value!!.goToPrevTrack()
            updateUI()
            mainActivityViewModel.isPlayingLiveData.value = true
            setPlayButtonImage()
        }

        likeButton.setOnClickListener {
            mainActivityViewModel.like = !mainActivityViewModel.like
            setLikeButtonImage()
            // TODO: favourites
        }

        repeatButton.setOnClickListener {
            mainActivityViewModel.repeat1 = !mainActivityViewModel.repeat1
            setRepeatButtonImage()
            // TODO: repeat playlist / song
        }

        playlistButton.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(
                    R.id.fragment_container,
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        mainActivityViewModel.curPlaylistLiveData.value!!,
                        false
                    )
                )
                .addToBackStack(null)
                .apply { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
                .commit()
        }

        returnButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        settingsButton.setOnClickListener {
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                PopupMenu(this, it).apply {
                    menuInflater.inflate(R.menu.menu_track_settings, menu)

                    /*setOnMenuItemClickListener {
                        when (it.itemId) {
                            // TODO: Track settings menu functionality
                        }
                    }*/

                    show()
                }
        }

        sheetBehavior = BottomSheetBehavior.from(playingPart)

        if (mainActivityViewModel.playingIdLiveData.value!! != None) {
            onTrackSelected(
                mainActivityViewModel
                    .tracks
                    .find { it.id == mainActivityViewModel.playingIdLiveData.value!!.unwrap() }!!,
            )

            mainActivityViewModel.isPlayingLiveData.value =
                !mainActivityViewModel.isPlayingLiveData.value!!
            setPlayButtonSmallImage()

            if (mainActivityViewModel.sheetBehaviorPositionLiveData.value!! == BottomSheetBehavior.STATE_EXPANDED) {
                toolbar.isVisible = false
                setPlayButtonImage()

                returnButton.alpha = 1.0F
                settingsButton.alpha = 1.0F
                albumImage.alpha = 1.0F
                appBarLayout.alpha = 0.0F
                playingToolbar.alpha = 0.0F
                trackTitleSmall.isSelected = true
                trackArtists.isSelected = true

                if (mainActivityViewModel.sheetBehaviorPositionLiveData.value!! == BottomSheetBehavior.STATE_EXPANDED)
                    sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        sheetBehavior.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) =
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> toolbar.isVisible = false
                        else -> Unit
                    }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (!toolbar.isVisible)
                        toolbar.isVisible = true

                    setPlayButtonSmallImage()
                    setPlayButtonImage()

                    returnButton.alpha = slideOffset
                    settingsButton.alpha = slideOffset
                    albumImage.alpha = slideOffset
                    appBarLayout.alpha = 1 - slideOffset
                    playingToolbar.alpha = 1 - slideOffset
                    trackTitleSmall.isSelected = true
                    trackArtists.isSelected = true
                }
            }
        )

        fragmentContainer = mainCoordinatorLayout.findViewById(R.id.fragment_container)

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
            setNavigationItemSelectedListener(this@MainActivity)

            itemIconTintList = null
            setBackgroundColor(if (Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE)
            itemTextColor =
                ColorStateList.valueOf(if (!Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE)

            menu.apply {
                get(0).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.tracks_blue
                        is Colors.BlueNight -> R.drawable.tracks_blue
                        is Colors.Green -> R.drawable.tracks_green
                        is Colors.GreenNight -> R.drawable.tracks_green
                        is Colors.GreenTurquoise -> R.drawable.tracks_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.tracks_green_turquoise
                        is Colors.Lemon -> R.drawable.tracks_lemon
                        is Colors.LemonNight -> R.drawable.tracks_lemon
                        is Colors.Orange -> R.drawable.tracks_orange
                        is Colors.OrangeNight -> R.drawable.tracks_orange
                        is Colors.Pink -> R.drawable.tracks_pink
                        is Colors.PinkNight -> R.drawable.tracks_pink
                        is Colors.Purple -> R.drawable.tracks_purple
                        is Colors.PurpleNight -> R.drawable.tracks_purple
                        is Colors.Red -> R.drawable.tracks_red
                        is Colors.RedNight -> R.drawable.tracks_red
                        is Colors.Sea -> R.drawable.tracks_sea
                        is Colors.SeaNight -> R.drawable.tracks_sea
                        is Colors.Turquoise -> R.drawable.tracks_turquoise
                        is Colors.TurquoiseNight -> R.drawable.tracks_turquoise
                        else -> R.drawable.tracks_blue
                    }
                )

                get(1).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.playlist_blue
                        is Colors.BlueNight -> R.drawable.playlist_blue
                        is Colors.Green -> R.drawable.playlist_green
                        is Colors.GreenNight -> R.drawable.playlist_green
                        is Colors.GreenTurquoise -> R.drawable.playlist_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.playlist_green_turquoise
                        is Colors.Lemon -> R.drawable.playlist_lemon
                        is Colors.LemonNight -> R.drawable.playlist_lemon
                        is Colors.Orange -> R.drawable.playlist_orange
                        is Colors.OrangeNight -> R.drawable.playlist_orange
                        is Colors.Pink -> R.drawable.playlist_pink
                        is Colors.PinkNight -> R.drawable.playlist_pink
                        is Colors.Purple -> R.drawable.playlist_purple
                        is Colors.PurpleNight -> R.drawable.playlist_purple
                        is Colors.Red -> R.drawable.playlist_red
                        is Colors.RedNight -> R.drawable.playlist_red
                        is Colors.Sea -> R.drawable.playlist_sea
                        is Colors.SeaNight -> R.drawable.playlist_sea
                        is Colors.Turquoise -> R.drawable.playlist_turquoise
                        is Colors.TurquoiseNight -> R.drawable.playlist_turquoise
                        else -> R.drawable.playlist
                    }
                )

                get(2).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.human_blue
                        is Colors.BlueNight -> R.drawable.human_blue
                        is Colors.Green -> R.drawable.human_green
                        is Colors.GreenNight -> R.drawable.human_green
                        is Colors.GreenTurquoise -> R.drawable.human_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.human_green_turquoise
                        is Colors.Lemon -> R.drawable.human_lemon
                        is Colors.LemonNight -> R.drawable.human_lemon
                        is Colors.Orange -> R.drawable.human_orange
                        is Colors.OrangeNight -> R.drawable.human_orange
                        is Colors.Pink -> R.drawable.human_pink
                        is Colors.PinkNight -> R.drawable.human_pink
                        is Colors.Purple -> R.drawable.human_purple
                        is Colors.PurpleNight -> R.drawable.human_purple
                        is Colors.Red -> R.drawable.human_red
                        is Colors.RedNight -> R.drawable.human_red
                        is Colors.Sea -> R.drawable.human_sea
                        is Colors.SeaNight -> R.drawable.human_sea
                        is Colors.Turquoise -> R.drawable.human_turquoise
                        is Colors.TurquoiseNight -> R.drawable.human_turquoise
                        else -> R.drawable.human
                    }
                )

                get(3).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.favourite_track_blue
                        is Colors.BlueNight -> R.drawable.favourite_track_blue
                        is Colors.Green -> R.drawable.favourite_track_green
                        is Colors.GreenNight -> R.drawable.favourite_track_green
                        is Colors.GreenTurquoise -> R.drawable.favourite_track_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.favourite_track_green_turquoise
                        is Colors.Lemon -> R.drawable.favourite_track_lemon
                        is Colors.LemonNight -> R.drawable.favourite_track_lemon
                        is Colors.Orange -> R.drawable.favourite_track_orange
                        is Colors.OrangeNight -> R.drawable.favourite_track_orange
                        is Colors.Pink -> R.drawable.favourite_track_pink
                        is Colors.PinkNight -> R.drawable.favourite_track_pink
                        is Colors.Purple -> R.drawable.favourite_track_purple
                        is Colors.PurpleNight -> R.drawable.favourite_track_purple
                        is Colors.Red -> R.drawable.favourite_track_red
                        is Colors.RedNight -> R.drawable.favourite_track_red
                        is Colors.Sea -> R.drawable.favourite_track_sea
                        is Colors.SeaNight -> R.drawable.favourite_track_sea
                        is Colors.Turquoise -> R.drawable.favourite_track_turquoise
                        is Colors.TurquoiseNight -> R.drawable.favourite_track_turquoise
                        else -> R.drawable.favourite_track
                    }
                )

                get(4).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.favourite_artist_blue
                        is Colors.BlueNight -> R.drawable.favourite_artist_blue
                        is Colors.Green -> R.drawable.favourite_artist_green
                        is Colors.GreenNight -> R.drawable.favourite_artist_green
                        is Colors.GreenTurquoise -> R.drawable.favourite_artist_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.favourite_artist_green_turquoise
                        is Colors.Lemon -> R.drawable.favourite_artist_lemon
                        is Colors.LemonNight -> R.drawable.favourite_artist_lemon
                        is Colors.Orange -> R.drawable.favourite_artist_orange
                        is Colors.OrangeNight -> R.drawable.favourite_artist_orange
                        is Colors.Pink -> R.drawable.favourite_artist_pink
                        is Colors.PinkNight -> R.drawable.favourite_artist_pink
                        is Colors.Purple -> R.drawable.favourite_artist_purple
                        is Colors.PurpleNight -> R.drawable.favourite_artist_purple
                        is Colors.Red -> R.drawable.favourite_artist_red
                        is Colors.RedNight -> R.drawable.favourite_artist_red
                        is Colors.Sea -> R.drawable.favourite_artist_sea
                        is Colors.SeaNight -> R.drawable.favourite_artist_sea
                        is Colors.Turquoise -> R.drawable.favourite_artist_turquoise
                        is Colors.TurquoiseNight -> R.drawable.favourite_artist_turquoise
                        else -> R.drawable.favourite_artist
                    }
                )

                get(5).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.recommendation_blue
                        is Colors.BlueNight -> R.drawable.recommendation_blue
                        is Colors.Green -> R.drawable.recommendation_green
                        is Colors.GreenNight -> R.drawable.recommendation_green
                        is Colors.GreenTurquoise -> R.drawable.recommendation_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.recommendation_green_turquoise
                        is Colors.Lemon -> R.drawable.recommendation_lemon
                        is Colors.LemonNight -> R.drawable.recommendation_lemon
                        is Colors.Orange -> R.drawable.recommendation_orange
                        is Colors.OrangeNight -> R.drawable.recommendation_orange
                        is Colors.Pink -> R.drawable.recommendation_pink
                        is Colors.PinkNight -> R.drawable.recommendation_pink
                        is Colors.Purple -> R.drawable.recommendation_purple
                        is Colors.PurpleNight -> R.drawable.recommendation_purple
                        is Colors.Red -> R.drawable.recommendation_red
                        is Colors.RedNight -> R.drawable.recommendation_red
                        is Colors.Sea -> R.drawable.recommendation_sea
                        is Colors.SeaNight -> R.drawable.recommendation_sea
                        is Colors.Turquoise -> R.drawable.recommendation_turquoise
                        is Colors.TurquoiseNight -> R.drawable.recommendation_turquoise
                        else -> R.drawable.recommendation
                    }
                )

                get(6).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.compilation_blue
                        is Colors.BlueNight -> R.drawable.compilation_blue
                        is Colors.Green -> R.drawable.compilation_green
                        is Colors.GreenNight -> R.drawable.compilation_green
                        is Colors.GreenTurquoise -> R.drawable.compilation_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.compilation_green_turquoise
                        is Colors.Lemon -> R.drawable.compilation_lemon
                        is Colors.LemonNight -> R.drawable.compilation_lemon
                        is Colors.Orange -> R.drawable.compilation_orange
                        is Colors.OrangeNight -> R.drawable.compilation_orange
                        is Colors.Pink -> R.drawable.compilation_pink
                        is Colors.PinkNight -> R.drawable.compilation_pink
                        is Colors.Purple -> R.drawable.compilation_purple
                        is Colors.PurpleNight -> R.drawable.compilation_purple
                        is Colors.Red -> R.drawable.compilation_red
                        is Colors.RedNight -> R.drawable.compilation_red
                        is Colors.Sea -> R.drawable.compilation_sea
                        is Colors.SeaNight -> R.drawable.compilation_sea
                        is Colors.Turquoise -> R.drawable.compilation_turquoise
                        is Colors.TurquoiseNight -> R.drawable.compilation_turquoise
                        else -> R.drawable.compilation
                    }
                )

                get(7).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.settings_blue
                        is Colors.BlueNight -> R.drawable.settings_blue
                        is Colors.Green -> R.drawable.settings_green
                        is Colors.GreenNight -> R.drawable.settings_green
                        is Colors.GreenTurquoise -> R.drawable.settings_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.settings_green_turquoise
                        is Colors.Lemon -> R.drawable.settings_lemon
                        is Colors.LemonNight -> R.drawable.settings_lemon
                        is Colors.Orange -> R.drawable.settings_orange
                        is Colors.OrangeNight -> R.drawable.settings_orange
                        is Colors.Pink -> R.drawable.settings_pink
                        is Colors.PinkNight -> R.drawable.settings_pink
                        is Colors.Purple -> R.drawable.settings_purple
                        is Colors.PurpleNight -> R.drawable.settings_purple
                        is Colors.Red -> R.drawable.settings_red
                        is Colors.RedNight -> R.drawable.settings_red
                        is Colors.Sea -> R.drawable.settings_sea
                        is Colors.SeaNight -> R.drawable.settings_sea
                        is Colors.Turquoise -> R.drawable.settings_turquoise
                        is Colors.TurquoiseNight -> R.drawable.settings_turquoise
                        else -> R.drawable.settings
                    }
                )

                get(8).setIcon(
                    when (Params.getInstance().theme) {
                        is Colors.Blue -> R.drawable.about_app_blue
                        is Colors.BlueNight -> R.drawable.about_app_blue
                        is Colors.Green -> R.drawable.about_app_green
                        is Colors.GreenNight -> R.drawable.about_app_green
                        is Colors.GreenTurquoise -> R.drawable.about_app_green_turquoise
                        is Colors.GreenTurquoiseNight -> R.drawable.about_app_green_turquoise
                        is Colors.Lemon -> R.drawable.about_app_lemon
                        is Colors.LemonNight -> R.drawable.about_app_lemon
                        is Colors.Orange -> R.drawable.about_app_orange
                        is Colors.OrangeNight -> R.drawable.about_app_orange
                        is Colors.Pink -> R.drawable.about_app_pink
                        is Colors.PinkNight -> R.drawable.about_app_pink
                        is Colors.Purple -> R.drawable.about_app_purple
                        is Colors.PurpleNight -> R.drawable.about_app_purple
                        is Colors.Red -> R.drawable.about_app_red
                        is Colors.RedNight -> R.drawable.about_app_red
                        is Colors.Sea -> R.drawable.about_app_sea
                        is Colors.SeaNight -> R.drawable.about_app_sea
                        is Colors.Turquoise -> R.drawable.about_app_turquoise
                        is Colors.TurquoiseNight -> R.drawable.about_app_turquoise
                        else -> R.drawable.about_app
                    }
                )
            }
        }

        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null)
            supportFragmentManager
                .beginTransaction()
                .add(
                    R.id.fragment_container,
                    TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        mainActivityViewModel.allTracksLiveData.value!!
                    )
                )
                .commit()

        val tv = TypedValue()
        if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            mainActivityViewModel.actionBarSize = TypedValue
                .complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(
            "track_id",
            mainActivityViewModel.playingIdLiveData.value!!.orNull()
        )

        outState.putBoolean(
            "is_playing",
            mainActivityViewModel.isPlayingLiveData.value!!
        )

        outState.putSerializable(
            "cur_playlist",
            mainActivityViewModel.curPlaylistLiveData.value
        )

        outState.putInt(
            "sheet_behavior_state",
            sheetBehavior.state
        )

        outState.putSerializable(
            "all_tracks",
            mainActivityViewModel.allTracksLiveData.value
        )
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in,
                R.anim.slide_out,
                R.anim.slide_in,
                R.anim.slide_out
            )
            .replace(
                R.id.fragment_container,
                when (item.itemId) {
                    R.id.nav_tracks -> TrackListFragment.newInstance(
                        mainLabel.text.toString(),
                        mainActivityViewModel.allTracksLiveData.value!!
                    ).apply { mainLabel.setText(R.string.tracks) }

                    R.id.nav_playlists -> PlaylistListFragment.newInstance()
                        .apply { mainLabel.setText(R.string.playlists) }

                    R.id.nav_artists -> ArtistListFragment.newInstance()
                        .apply { mainLabel.setText(R.string.artists) }

                    R.id.nav_favourite_artists -> FavouriteArtistsFragment.newInstance()
                        .apply { mainLabel.setText(R.string.favourite_artists) }

                    R.id.nav_favourite_tracks -> FavouriteTracksFragment.newInstance()
                        .apply { mainLabel.setText(R.string.favourite_tracks) }

                    R.id.nav_recommendations -> RecommendationsFragment.newInstance()
                        .apply { mainLabel.setText(R.string.recommendations) }

                    R.id.nav_compilation -> CompilationFragment.newInstance()
                        .apply { mainLabel.setText(R.string.compilation) }

                    R.id.nav_settings -> SettingsFragment.newInstance()
                        .apply { mainLabel.setText(R.string.settings) }

                    else -> AboutAppFragment.newInstance()
                        .apply { mainLabel.setText(R.string.about_app) }
                }
            )
            .addToBackStack(null)
            .apply {
                if (mainActivityViewModel.isPlayingLiveData.value!!)
                    playingPart.isVisible = true
            }
            .commit()

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed(): Unit = when {
        drawerLayout.isDrawerOpen(GravityCompat.START) -> drawerLayout.closeDrawer(GravityCompat.START)
        else -> super.onBackPressed()
    }

    override fun onTrackSelected(track: Track) {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            mainActivityViewModel.isPlayingLiveData.value =
                mainActivityViewModel.playingIdLiveData.value!!.let {
                    when (it) {
                        None -> true

                        is Some -> when (it.value) {
                            track.id -> !mainActivityViewModel.isPlayingLiveData.value!!
                            else -> true
                        }
                    }
                }

            mainActivityViewModel.playingIdLiveData.value = Some(track.id)

            setPlayButtonSmallImage()

            val sortedTracks = mainActivityViewModel.tracks.sortedBy { it.title }
            val end = sortedTracks.takeWhile { it.id != track.id }

            mainActivityViewModel.curPlaylistLiveData.value!!.apply {
                clear()
                addAll(sortedTracks.dropWhile { it.id != track.id })
                addAll(end)
            }

            updateUI()

            returnButton.alpha = 0.0F
            settingsButton.alpha = 0.0F
            albumImage.alpha = 0.0F
            trackTitleSmall.isSelected = true
            trackArtists.isSelected = true

            if (!playingPart.isVisible)
                playingPart.isVisible = true
        }
    }

    override fun onArtistSelected(artist: Artist) {
        // TODO: Artist Selection
        Toast.makeText(this, "Coming Soon", Toast.LENGTH_LONG).show()
    }

    private fun setTheme() = setTheme(
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

    private fun updateUI() {
        playingPart.setBackgroundColor(if (Params.getInstance().theme.isNight) Color.BLACK else Color.WHITE)

        val track = mainActivityViewModel.curPlaylistLiveData.value!!.currentTrack
        val trackArtistAlbum = "${track.artist} / ${track.album}"

        trackTitleSmall.text = track.title
        trackArtists.text = track.artist

        trackTitle.text = track.title
        artistsAlbum.text = trackArtistAlbum
    }

    internal fun setPlayButtonSmallImage() = playButtonSmall.setImageResource(
        when {
            mainActivityViewModel.isPlayingLiveData.value!! -> android.R.drawable.ic_media_pause
            else -> android.R.drawable.ic_media_play
        }
    )

    private fun setLikeButtonImage() = likeButton.setImageResource(
        when {
            mainActivityViewModel.like -> R.drawable.heart_like
            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.heart_blue
                is Colors.BlueNight -> R.drawable.heart_blue
                is Colors.Green -> R.drawable.heart_green
                is Colors.GreenNight -> R.drawable.heart_green
                is Colors.GreenTurquoise -> R.drawable.heart_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.heart_green_turquoise
                is Colors.Lemon -> R.drawable.heart_lemon
                is Colors.LemonNight -> R.drawable.heart_lemon
                is Colors.Orange -> R.drawable.heart_orange
                is Colors.OrangeNight -> R.drawable.heart_orange
                is Colors.Pink -> R.drawable.heart_pink
                is Colors.PinkNight -> R.drawable.heart_pink
                is Colors.Purple -> R.drawable.heart_purple
                is Colors.PurpleNight -> R.drawable.heart_purple
                is Colors.Red -> R.drawable.heart_red
                is Colors.RedNight -> R.drawable.heart_red
                is Colors.Sea -> R.drawable.heart_sea
                is Colors.SeaNight -> R.drawable.heart_sea
                is Colors.Turquoise -> R.drawable.heart_turquoise
                is Colors.TurquoiseNight -> R.drawable.heart_turquoise
                else -> R.drawable.heart
            }
        }
    )

    internal fun setPlayButtonImage() = playButton.setImageResource(
        when {
            !mainActivityViewModel.isPlayingLiveData.value!! -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.play_blue
                is Colors.BlueNight -> R.drawable.play_blue
                is Colors.Green -> R.drawable.play_green
                is Colors.GreenNight -> R.drawable.play_green
                is Colors.GreenTurquoise -> R.drawable.play_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.play_green_turquoise
                is Colors.Lemon -> R.drawable.play_lemon
                is Colors.LemonNight -> R.drawable.play_lemon
                is Colors.Orange -> R.drawable.play_orange
                is Colors.OrangeNight -> R.drawable.play_orange
                is Colors.Pink -> R.drawable.play_pink
                is Colors.PinkNight -> R.drawable.play_pink
                is Colors.Purple -> R.drawable.play_purple
                is Colors.PurpleNight -> R.drawable.play_purple
                is Colors.Red -> R.drawable.play_red
                is Colors.RedNight -> R.drawable.play_red
                is Colors.Sea -> R.drawable.play_sea
                is Colors.SeaNight -> R.drawable.play_sea
                is Colors.Turquoise -> R.drawable.play_turquoise
                is Colors.TurquoiseNight -> R.drawable.play_turquoise
                else -> R.drawable.play
            }

            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.pause_blue
                is Colors.BlueNight -> R.drawable.pause_blue
                is Colors.Green -> R.drawable.pause_green
                is Colors.GreenNight -> R.drawable.pause_green
                is Colors.GreenTurquoise -> R.drawable.pause_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.pause_green_turquoise
                is Colors.Lemon -> R.drawable.pause_lemon
                is Colors.LemonNight -> R.drawable.pause_lemon
                is Colors.Orange -> R.drawable.pause_orange
                is Colors.OrangeNight -> R.drawable.pause_orange
                is Colors.Pink -> R.drawable.pause_pink
                is Colors.PinkNight -> R.drawable.pause_pink
                is Colors.Purple -> R.drawable.pause_purple
                is Colors.PurpleNight -> R.drawable.pause_purple
                is Colors.Red -> R.drawable.pause_red
                is Colors.RedNight -> R.drawable.pause_red
                is Colors.Sea -> R.drawable.pause_sea
                is Colors.SeaNight -> R.drawable.pause_sea
                is Colors.Turquoise -> R.drawable.pause_turquoise
                is Colors.TurquoiseNight -> R.drawable.pause_turquoise
                else -> R.drawable.pause
            }
        }
    )

    private fun setRepeatButtonImage() = repeatButton.setImageResource(
        when {
            mainActivityViewModel.repeat1 -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.repeat1_blue
                is Colors.BlueNight -> R.drawable.repeat1_blue
                is Colors.Green -> R.drawable.repeat1_green
                is Colors.GreenNight -> R.drawable.repeat1_green
                is Colors.GreenTurquoise -> R.drawable.repeat1_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat1_green_turquoise
                is Colors.Lemon -> R.drawable.repeat1_lemon
                is Colors.LemonNight -> R.drawable.repeat1_lemon
                is Colors.Orange -> R.drawable.repeat1_orange
                is Colors.OrangeNight -> R.drawable.repeat1_orange
                is Colors.Pink -> R.drawable.repeat1_pink
                is Colors.PinkNight -> R.drawable.repeat1_pink
                is Colors.Purple -> R.drawable.repeat1_purple
                is Colors.PurpleNight -> R.drawable.repeat1_purple
                is Colors.Red -> R.drawable.repeat1_red
                is Colors.RedNight -> R.drawable.repeat1_red
                is Colors.Sea -> R.drawable.repeat1_sea
                is Colors.SeaNight -> R.drawable.repeat1_sea
                is Colors.Turquoise -> R.drawable.repeat1_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat1_turquoise
                else -> R.drawable.repeat_1
            }

            else -> when (Params.getInstance().theme) {
                is Colors.Blue -> R.drawable.repeat_blue
                is Colors.BlueNight -> R.drawable.repeat_blue
                is Colors.Green -> R.drawable.repeat_green
                is Colors.GreenNight -> R.drawable.repeat_green
                is Colors.GreenTurquoise -> R.drawable.repeat_green_turquoise
                is Colors.GreenTurquoiseNight -> R.drawable.repeat_green_turquoise
                is Colors.Lemon -> R.drawable.repeat_lemon
                is Colors.LemonNight -> R.drawable.repeat_lemon
                is Colors.Orange -> R.drawable.repeat_orange
                is Colors.OrangeNight -> R.drawable.repeat_orange
                is Colors.Pink -> R.drawable.repeat_pink
                is Colors.PinkNight -> R.drawable.repeat_pink
                is Colors.Purple -> R.drawable.repeat_purple
                is Colors.PurpleNight -> R.drawable.repeat_purple
                is Colors.Red -> R.drawable.repeat_red
                is Colors.RedNight -> R.drawable.repeat_red
                is Colors.Sea -> R.drawable.repeat_sea
                is Colors.SeaNight -> R.drawable.repeat_sea
                is Colors.Turquoise -> R.drawable.repeat_turquoise
                is Colors.TurquoiseNight -> R.drawable.repeat_turquoise
                else -> R.drawable.repeat
            }
        }
    )

    private fun loadStorageData() {
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        ).use { cursor ->
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    mainActivityViewModel.allTracksLiveData.value!!.add(
                        Track(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getLong(5)
                        )
                    )
                }
            }
        }
    }
}