package org.wycliffeassociates.translationrecorder.Recording.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.databinding.FragmentSourceAudioBinding
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project
import javax.inject.Inject

/**
 * Created by sarabiaj on 2/20/2017.
 */
@AndroidEntryPoint
class FragmentSourceAudio : Fragment() {

    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var prefs: IPreferenceRepository

    private var _binding: FragmentSourceAudioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentSourceAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun loadAudio(project: Project, filename: String, chapter: Int) {
        binding.sourceAudioPlayer.initSrcAudio(project, filename, chapter, directoryProvider, prefs)
    }

    fun disableSourceAudio() {
        binding.sourceAudioPlayer.cleanup()
        binding.sourceAudioPlayer.isEnabled = false
    }

    fun resetSourceAudio(project: Project, filename: String, chapter: Int) {
        binding.sourceAudioPlayer.reset(project, filename, chapter, directoryProvider, prefs)
    }

    fun initialize(project: Project, filename: String, chapter: Int) {
        binding.sourceAudioPlayer.initSrcAudio(project, filename, chapter, directoryProvider, prefs)
    }

    override fun onPause() {
        super.onPause()
        binding.sourceAudioPlayer.pauseSource()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): FragmentSourceAudio {
            val f = FragmentSourceAudio()
            return f
        }
    }
}
