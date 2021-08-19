package com.dinaraparanid.prima.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.databinding.DataBindingUtil
import com.db.chart.model.LineSet
import com.db.chart.view.AxisController
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.FragmentEqualizerBinding
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.StorageUtil
import com.dinaraparanid.prima.utils.equalizer.EqualizerModel
import com.dinaraparanid.prima.utils.equalizer.EqualizerSettings
import com.dinaraparanid.prima.utils.polymorphism.AbstractFragment
import com.dinaraparanid.prima.viewmodels.mvvm.EqualizerViewModel

/**
 * Equalizer Fragment to modify audio.
 */

internal class EqualizerFragment : AbstractFragment() {
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var paint: Paint
    private lateinit var chart: LineChartView
    private lateinit var dataset: LineSet
    private lateinit var points: FloatArray
    internal lateinit var context: Context

    private lateinit var binding: FragmentEqualizerBinding

    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var numberOfFrequencyBands: Short = 0
    private var audioSessionId = 0
    private var y = 0

    internal class Builder(private val mainLabelOldText: String) {
        private var id = -1

        internal fun setAudioSessionId(id: Int): Builder {
            this.id = id
            return this
        }

        internal fun build() = newInstance(mainLabelOldText, id)
    }

    private companion object {
        private const val ARG_AUDIO_SESSION_ID = "audio_session_id"

        fun newInstance(mainLabelOldText: String, audioSessionId: Int): EqualizerFragment {
            val args = Bundle()
            args.putInt(ARG_AUDIO_SESSION_ID, audioSessionId)
            args.putString(MAIN_LABEL_OLD_TEXT_KEY, mainLabelOldText)
            val fragment = EqualizerFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainLabelOldText = requireArguments().getString(MAIN_LABEL_OLD_TEXT_KEY)
            ?: resources.getString(R.string.equalizer)
        mainLabelCurText = resources.getString(R.string.equalizer)

        val loader = StorageUtil(requireContext())
        (requireActivity().application as MainApplication).musicPlayer!!.playbackParams =
            PlaybackParams().setPitch(loader.loadPitch()).setSpeed(loader.loadSpeed())

        EqualizerSettings.instance.isEqualizerEnabled = true

        EqualizerSettings.instance.isEditing = true
        audioSessionId = requireArguments().getInt(ARG_AUDIO_SESSION_ID)

        if (EqualizerSettings.instance.equalizerModel == null) {
            EqualizerSettings.instance.equalizerModel = EqualizerModel(context).apply {
                reverbPreset = PresetReverb.PRESET_NONE
                bassStrength = (1000 / 19).toShort()
            }
        }

        val app = requireActivity().application as MainApplication

        app.equalizer = Equalizer(0, audioSessionId)
        app.bassBoost = BassBoost(0, audioSessionId).apply {
            enabled = EqualizerSettings.instance.isEqualizerEnabled
            properties = BassBoost.Settings(properties.toString()).apply {
                strength = StorageUtil(requireContext()).loadBassStrength()
            }
        }

        app.presetReverb = PresetReverb(0, audioSessionId).apply {
            try {
                preset = StorageUtil(requireContext()).loadReverbPreset()
            } catch (ignored: Exception) {
                // not supported
            }
            enabled = EqualizerSettings.instance.isEqualizerEnabled
        }

        app.equalizer.enabled = EqualizerSettings.instance.isEqualizerEnabled

        val seekBarPoses = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        when (EqualizerSettings.instance.presetPos) {
            0 -> (0 until app.equalizer.numberOfBands).forEach {
                app.equalizer.setBandLevel(
                    it.toShort(),
                    seekBarPoses[it].toShort()
                )
            }

            else -> app.equalizer.usePreset(EqualizerSettings.instance.presetPos.toShort())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val app = requireActivity().application as MainApplication
        val themeColor = Params.instance.primaryColor

        binding = DataBindingUtil
            .inflate<FragmentEqualizerBinding>(
                inflater,
                R.layout.fragment_equalizer,
                container,
                false
            )
            .apply {
                viewModel = EqualizerViewModel(requireActivity())

                spinnerDropdownIcon.setOnClickListener { equalizerPresetSpinner.performClick() }

                paint = Paint()
                dataset = LineSet()

                val pit = StorageUtil(requireContext()).loadPitch()
                pitchStatus.text = pit.toString().take(4)

                pitchSeekBar.run {
                    progress = ((pit - 0.5F) * 100).toInt()
                    var newPitch = 0F

                    setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            val ap = requireActivity().application as MainApplication

                            if (EqualizerSettings.instance.isEqualizerEnabled) {
                                val speed = ap.musicPlayer!!.playbackParams.speed
                                newPitch = 0.5F + progress * 0.01F

                                try {
                                    val isPlaying = ap.musicPlayer!!.isPlaying

                                    ap.musicPlayer!!.playbackParams = PlaybackParams()
                                        .setSpeed(speed)
                                        .setPitch(newPitch)

                                    if (!isPlaying)
                                        (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                } catch (ignored: Exception) {
                                    // old or weak phone
                                }

                                pitchStatus.text = newPitch.toString().take(4)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) =
                            (requireActivity() as MainActivity).run { if (isPlaying != true) resumePlaying() }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            try {
                                app.musicPlayer!!.playbackParams = PlaybackParams()
                                    .setSpeed(app.musicPlayer!!.playbackParams.speed)
                                    .setPitch(newPitch)
                            } catch (e: Exception) {
                                progress = 50

                                Toast.makeText(
                                    requireContext(),
                                    R.string.not_supported,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            if (Params.instance.saveEqualizerSettings)
                                StorageUtil(requireContext()).storePitch(newPitch)
                        }
                    })
                }

                val speed = StorageUtil(requireContext()).loadSpeed()
                speedStatus.text = speed.toString().take(4)

                speedSeekBar.run {
                    progress = ((StorageUtil(requireContext()).loadSpeed() - 0.5F) * 100).toInt()
                    var newSpeed = 0F

                    setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            val ap = requireActivity().application as MainApplication

                            if (EqualizerSettings.instance.isEqualizerEnabled) {
                                val pitch = ap.musicPlayer!!.playbackParams.pitch
                                newSpeed = 0.5F + progress * 0.01F

                                try {
                                    val isPlaying = ap.musicPlayer!!.isPlaying

                                    ap.musicPlayer!!.playbackParams = PlaybackParams()
                                        .setPitch(pitch)
                                        .setSpeed(newSpeed)

                                    if (!isPlaying)
                                        (requireActivity() as MainActivity).reinitializePlayingCoroutine()
                                } catch (ignored: Exception) {
                                    // old or weak phone
                                }

                                speedStatus.text = newSpeed.toString().take(4)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) =
                            (requireActivity() as MainActivity).run { if (isPlaying != true) resumePlaying() }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            try {
                                app.musicPlayer!!.playbackParams = PlaybackParams()
                                    .setSpeed(newSpeed)
                                    .setPitch(app.musicPlayer!!.playbackParams.pitch)
                            } catch (e: Exception) {
                                progress = 50

                                Toast.makeText(
                                    requireContext(),
                                    R.string.not_supported,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            if (Params.instance.saveEqualizerSettings)
                                StorageUtil(requireContext())
                                    .storeSpeed(app.musicPlayer!!.playbackParams.speed)
                        }
                    })
                }

                controllerBass.run {
                    label = resources.getString(R.string.bass)
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()
                    setOnProgressChangedListener {
                        viewModel!!.onControllerBassProgressChanged(it)
                    }
                }

                controller3D.run {
                    label = "3D"
                    circlePaint2.color = themeColor
                    linePaint.color = themeColor
                    invalidate()
                    setOnProgressChangedListener {
                        viewModel!!.onControllerBassProgressChanged(it)
                    }
                }

                when {
                    !EqualizerSettings.instance.isEqualizerReloaded -> {
                        val x = app.bassBoost.roundedStrength * 19 / 1000
                        y = app.presetReverb.preset * 19 / 6
                        controllerBass.progress = if (x == 0) 1 else x
                        controller3D.progress = if (y == 0) 1 else y
                    }

                    else -> {
                        val x = EqualizerSettings.instance.bassStrength * 19 / 1000
                        y = EqualizerSettings.instance.reverbPreset * 19 / 6
                        controllerBass.progress = if (x == 0) 1 else x
                        controller3D.progress = if (y == 0) 1 else y
                    }
                }

                numberOfFrequencyBands = 5
                points = FloatArray(numberOfFrequencyBands.toInt())

                val lowerEqualizerBandLevel = app.equalizer.bandLevelRange[0]
                val upperEqualizerBandLevel = app.equalizer.bandLevelRange[1]

                (0 until numberOfFrequencyBands).forEach {
                    val equalizerBandIndex = it.toShort()

                    val frequencyHeader = TextView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )

                        gravity = Gravity.CENTER_HORIZONTAL
                        text = "${app.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                    }

                    val seekBar: SeekBar
                    val textView: TextView

                    when (it) {
                        0 -> {
                            seekBar = seekBar1
                            textView = textView1
                        }

                        1 -> {
                            seekBar = seekBar2
                            textView = textView2
                        }

                        2 -> {
                            seekBar = seekBar3
                            textView = textView3
                        }

                        3 -> {
                            seekBar = seekBar4
                            textView = textView4
                        }

                        else -> {
                            seekBar = seekBar5
                            textView = textView5
                        }
                    }

                    seekBarFinal[it] = seekBar.apply {
                        id = it
                        max = upperEqualizerBandLevel - lowerEqualizerBandLevel
                    }

                    textView.run {
                        text = "${app.equalizer.getCenterFreq(equalizerBandIndex) / 1000} Hz"
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }

                    val seekBarPoses = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
                        ?: EqualizerSettings.instance.seekbarPos

                    when {
                        EqualizerSettings.instance.isEqualizerReloaded -> {
                            points[it] = (seekBarPoses[it] - lowerEqualizerBandLevel).toFloat()
                            dataset.addPoint(frequencyHeader.text.toString(), points[it])
                            seekBar.progress = seekBarPoses[it] - lowerEqualizerBandLevel
                        }

                        else -> {
                            points[it] =
                                (app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                            dataset.addPoint(frequencyHeader.text.toString(), points[it])

                            seekBar.progress =
                                app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel

                            EqualizerSettings.instance.seekbarPos[it] =
                                app.equalizer.getBandLevel(equalizerBandIndex).toInt()

                            EqualizerSettings.instance.isEqualizerReloaded = true
                        }
                    }

                    seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            app.equalizer.setBandLevel(
                                equalizerBandIndex,
                                (progress + lowerEqualizerBandLevel).toShort()
                            )

                            points[seekBar.id] =
                                (app.equalizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()

                            EqualizerSettings.instance.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel
                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[seekBar.id] =
                                progress + lowerEqualizerBandLevel

                            dataset.updateValues(points)
                            chart.notifyDataUpdate()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            equalizerPresetSpinner.setSelection(0)
                            EqualizerSettings.instance.presetPos = 0
                            EqualizerSettings.instance.equalizerModel!!.presetPos = 0
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (Params.instance.saveEqualizerSettings)
                                StorageUtil(context)
                                    .storeEqualizerSeekbarsPos(EqualizerSettings.instance.seekbarPos)
                        }
                    })
                }
            }

        binding.controller3D.setOnProgressChangedListener {
            EqualizerSettings.instance.reverbPreset = (it * 6 / 19).toShort()
            EqualizerSettings.instance.equalizerModel!!.reverbPreset =
                EqualizerSettings.instance.reverbPreset
            app.presetReverb.preset = EqualizerSettings.instance.reverbPreset

            if (Params.instance.saveEqualizerSettings)
                StorageUtil(context)
                    .storeReverbPreset(EqualizerSettings.instance.reverbPreset)

            y = it
        }

        equalizeSound()

        paint.run {
            color = themeColor
            strokeWidth = (1.10 * EqualizerSettings.instance.ratio).toFloat()
        }

        dataset.run {
            color = themeColor
            isSmooth = true
            thickness = 5F
        }

        chart = binding.lineChart.apply {

            setXAxis(false)
            setYAxis(false)
            setYLabels(AxisController.LabelPosition.NONE)
            setXLabels(AxisController.LabelPosition.NONE)
            setGrid(ChartView.GridType.NONE, 7, 10, paint)
            setAxisBorderValues(-300, 3300)
            addData(dataset)
            show()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val pos = StorageUtil(requireContext()).loadEqualizerSeekbarsPos()
            ?: EqualizerSettings.instance.seekbarPos

        val lowerEqualizerBandLevel = (requireActivity().application as MainApplication)
            .equalizer.bandLevelRange[0]

        seekBarFinal.forEachIndexed { i, sb -> sb?.progress = pos[i] - lowerEqualizerBandLevel }

        val pit = StorageUtil(requireContext()).loadPitch()
        binding.pitchStatus.text = pit.toString().take(4)
        binding.pitchSeekBar.progress = ((pit - 0.5F) * 100).toInt()

        val speed = StorageUtil(requireContext()).loadSpeed()
        binding.speedStatus.text = speed.toString().take(4)
        binding.speedSeekBar.progress = ((speed - 0.5F) * 100).toInt()
    }

    private fun equalizeSound() {
        val equalizerPresetNames = mutableListOf<String>().apply {
            add(resources.getString(R.string.custom))
            addAll(
                listOf(
                    R.string.normal,
                    R.string.classic,
                    R.string.dance,
                    R.string.flat,
                    R.string.folk,
                    R.string.heavy_metal,
                    R.string.hip_hop,
                    R.string.jazz,
                    R.string.pop,
                    R.string.rock
                ).map(resources::getString)
            )
        }

        binding.equalizerPresetSpinner.adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            equalizerPresetNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (EqualizerSettings.instance.isEqualizerReloaded && EqualizerSettings.instance.presetPos != 0)
            binding.equalizerPresetSpinner.setSelection(EqualizerSettings.instance.presetPos)

        binding.equalizerPresetSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val app = requireActivity().application as MainApplication

                    if (Params.instance.saveEqualizerSettings)
                        StorageUtil(context).storePresetPos(position)

                    if (position != 0) {
                        app.equalizer.usePreset((position - 1).toShort())
                        EqualizerSettings.instance.presetPos = position

                        val numberOfFreqBands: Short = 5
                        val lowerEqualizerBandLevel = app.equalizer.bandLevelRange[0]

                        (0 until numberOfFreqBands).forEach {
                            seekBarFinal[it]!!.progress =
                                app.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel

                            points[it] =
                                (app.equalizer.getBandLevel(it.toShort()) - lowerEqualizerBandLevel).toFloat()

                            EqualizerSettings.instance.seekbarPos[it] =
                                app.equalizer.getBandLevel(it.toShort()).toInt()

                            EqualizerSettings.instance.equalizerModel!!.seekbarPos[it] =
                                app.equalizer.getBandLevel(it.toShort()).toInt()
                        }

                        dataset.updateValues(points)
                        chart.notifyDataUpdate()
                    }

                    EqualizerSettings.instance.equalizerModel!!.presetPos = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        EqualizerSettings.instance.isEditing = false
    }
}