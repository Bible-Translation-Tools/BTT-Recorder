package org.wycliffeassociates.translationrecorder.login.fragments

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wycliffeassociates.translationrecorder.AudioVisualization.ActiveRecordingRenderer
import org.wycliffeassociates.translationrecorder.Recording.RecordingQueues
import org.wycliffeassociates.translationrecorder.Recording.WavFileWriter
import org.wycliffeassociates.translationrecorder.Recording.WavRecorder
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingWaveform
import org.wycliffeassociates.translationrecorder.databinding.FragmentCreateProfileBinding
import org.wycliffeassociates.translationrecorder.login.interfaces.OnProfileCreatedListener
import org.wycliffeassociates.translationrecorder.login.utils.convertWavToMp4
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.util.*

/**
 * Created by sarabiaj on 3/10/2018.
 */

class FragmentCreateProfile : Fragment() {

    companion object {
        fun newInstance(uploadDir: File, profileCreatedCallback: OnProfileCreatedListener): FragmentCreateProfile {
            val args = Bundle()
            val fragment = FragmentCreateProfile()
            fragment.arguments = args
            fragment.uploadDir = uploadDir
            uploadDir.mkdirs()
            fragment.userAudio = File(uploadDir, UUID.randomUUID().toString())
            fragment.userAudio.createNewFile()
            fragment.profileCreatedCallback = profileCreatedCallback
            return fragment
        }
    }

    private lateinit var uploadDir: File
    private lateinit var userAudio: File
    private lateinit var hash: String
    private var profileCreatedCallback: OnProfileCreatedListener? = null

    private lateinit var recording: File
    private lateinit var recordingWaveform: FragmentRecordingWaveform
    private lateinit var renderer: ActiveRecordingRenderer

    private var _binding: FragmentCreateProfileBinding? = null
    private val binding get() = _binding!!

    private var isRecording = false
    private var newRecording: WavFile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentCreateProfileBinding.inflate(inflater, container, false)
        return binding.root
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("user_audio", userAudio)
    }

    private fun startRecording() {
        if (!isRecording) {
            isRecording = true
            requireActivity().stopService(Intent(activity, WavRecorder::class.java))
            RecordingQueues.clearQueues()
            recording = File.createTempFile(UUID.randomUUID().toString(), ".raw")
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
            if (userAudio.exists()) {
                withContext(Dispatchers.IO) {
                    userAudio.delete()
                    userAudio.createNewFile()
                }
            }
            hash = convertWavToMp4(recording, userAudio)
            profileCreatedCallback?.onProfileCreated(newRecording!!, userAudio, hash)
        }
    }
}
