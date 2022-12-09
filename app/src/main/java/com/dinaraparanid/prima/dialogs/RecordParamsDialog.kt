package com.dinaraparanid.prima.dialogs

import android.app.Dialog
import android.os.Build
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.dinaraparanid.prima.MainActivity
import com.dinaraparanid.prima.MainApplication
import com.dinaraparanid.prima.R
import com.dinaraparanid.prima.databinding.DialogRecordBinding
import com.dinaraparanid.prima.services.MicRecordService
import com.dinaraparanid.prima.utils.Params
import com.dinaraparanid.prima.utils.polymorphism.runOnIOThread
import com.dinaraparanid.prima.mvvmp.presenters.BasePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/** [Dialog] to start recording */

class RecordParamsDialog(activity: MainActivity) : Dialog(activity) {
    private val binding = DataBindingUtil
        .inflate<DialogRecordBinding>(layoutInflater, R.layout.dialog_record, null, false)
        .apply { viewModel = BasePresenter() }

    private val sourceArray = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> listOf(
            activity.resources.getString(R.string.source_mic),
            activity.resources.getString(R.string.source_playback),
        )

        else -> listOf(activity.resources.getString(R.string.source_mic))
    }

    init {
        setContentView(binding!!.root)
        setTitle(R.string.record_audio)
        setCancelable(true)

        binding.recordSourceSpinner.run {
            adapter = ArrayAdapter(activity, R.layout.dialog_text_view, sourceArray)
            setSelection(0)
        }

        binding.recordSourceSpinnerDropdownIcon.setOnClickListener {
            binding.recordSourceSpinner.performClick()
        }

        binding.startRecording.setOnClickListener {
            var name = binding.recordFilename.text.toString()
            activity.runOnIOThread {
                if (File("${Params.getInstanceSynchronized().pathToSave}/$name.mp3").exists()) {
                    var ind = 1

                    while (File("${Params.getInstanceSynchronized().pathToSave}/$name($ind).mp3").exists())
                        ind++

                    name = "$name($ind)"
                }

                when (binding.recordSourceSpinner.selectedItemPosition) {
                    0 -> {
                        launch(Dispatchers.Main) {
                            activity.setRecordButtonImage(true, isLocking = true)
                        }

                        MicRecordService.Caller(WeakReference(activity.application as MainApplication))
                            .setFileName(name)
                            .call()
                    }

                    else -> {
                        activity.setRecordFilename(name)
                        activity.startMediaProjectionRequest()
                    }
                }

                launch(Dispatchers.Main) { dismiss() }
            }
        }
    }
}