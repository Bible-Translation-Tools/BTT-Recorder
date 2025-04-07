package org.wycliffeassociates.translationrecorder.login.fragments

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wycliffeassociates.translationrecorder.AudioVisualization.ActiveRecordingRenderer
import org.wycliffeassociates.translationrecorder.Recording.RecordingQueues
import org.wycliffeassociates.translationrecorder.Recording.WavFileWriter
import org.wycliffeassociates.translationrecorder.Recording.WavRecorder
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingWaveform
import org.wycliffeassociates.translationrecorder.databinding.FragmentCreateProfileBinding
import org.wycliffeassociates.translationrecorder.login.utils.ConvertAudio
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Created by sarabiaj on 3/10/2018.
 */
@AndroidEntryPoint
class FragmentCreateProfile : Fragment() {

    @Inject lateinit var directoryProvider: IDirectoryProvider

    interface OnProfileCreatedListener {
        fun onProfileCreated(wav: WavFile, audio: File, hash: String)
    }

    companion object {
        fun newInstance(directoryProvider: IDirectoryProvider): FragmentCreateProfile {
            val args = Bundle()
            val fragment = FragmentCreateProfile()
            fragment.arguments = args
            fragment.userAudio = File(directoryProvider.profilesDir, UUID.randomUUID().toString())
            fragment.userAudio.createNewFile()
            return fragment
        }
    }

    private lateinit var userAudio: File
    private lateinit var hash: String

    private lateinit var recordingWaveform: FragmentRecordingWaveform
    private lateinit var renderer: ActiveRecordingRenderer

    private var profileCreatedCallback: OnProfileCreatedListener? = null

    private var _binding: FragmentCreateProfileBinding? = null
    private val binding get() = _binding!!

    private var isRecording = false
    private var recording: File? = null
    private var newRecording: WavFile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentCreateProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        profileCreatedCallback = context as OnProfileCreatedListener
    }

    override fun onDestroy() {
        super.onDestroy()
        profileCreatedCallback = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recordingWaveform = FragmentRecordingWaveform.newInstance()

        with(binding) {
            btnRecord.setOnClickListener {
                (btnRecord.background as Animatable).start()
                if (btnRecord.isActivated.not()) {
                    btnRecord.isActivated = true
                    startRecording()
                }
            }
        }

        parentFragmentManager.beginTransaction()
            .add(binding.waveformView.id, recordingWaveform).commit()
        renderer = ActiveRecordingRenderer(null, null, recordingWaveform)
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            requireActivity().stopService(Intent(activity, WavRecorder::class.java))
            RecordingQueues.clearQueues()
            recording = directoryProvider.createTempFile("recording", ".raw")
            newRecording = WavFile(recording, null)
            requireActivity().startService(Intent(activity, WavRecorder::class.java))
            requireActivity().startService(WavFileWriter.getIntent(activity, newRecording))
            renderer.listenForRecording(false)
            Handler(Looper.getMainLooper()).postDelayed({ stopRecording() }, 3000)
        }
    }

    private fun stopRecording() {
        //Stop recording, load the recorded file, and draw
        requireActivity().stopService(Intent(activity, WavRecorder::class.java))
        RecordingQueues.pauseQueues()
        RecordingQueues.stopQueues(activity)
        isRecording = false
        binding.btnRecord.isActivated = true
        convertAudio()
    }

    private fun convertAudio() {
        lifecycleScope.launch(Dispatchers.Main) {
            hash = ConvertAudio.convertWavToMp4(recording!!, userAudio)
            profileCreatedCallback?.onProfileCreated(newRecording!!, userAudio, hash)
        }
    }
}
