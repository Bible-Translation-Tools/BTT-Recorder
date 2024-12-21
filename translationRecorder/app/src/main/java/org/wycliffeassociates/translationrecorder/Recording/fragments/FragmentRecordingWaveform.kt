package org.wycliffeassociates.translationrecorder.Recording.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.databinding.FragmentRecordingWaveformBinding

/**
 * Created by sarabiaj on 2/20/2017.
 */
class FragmentRecordingWaveform : Fragment() {
    private var _binding: FragmentRecordingWaveformBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingWaveformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateWaveform(buffer: FloatArray?) {
        binding.mainCanvas.setBuffer(buffer)
        binding.mainCanvas.postInvalidate()
    }

    fun setDrawingFromBuffer(drawFromBuffer: Boolean) {
        binding.mainCanvas.setDrawingFromBuffer(drawFromBuffer)
    }

    val width: Int
        get() {
            val view = view
            return view?.width ?: 0
        }

    val height: Int
        get() {
            val view = view
            return view?.height ?: 0
        }

    companion object {
        @JvmStatic
        fun newInstance(): FragmentRecordingWaveform {
            return FragmentRecordingWaveform()
        }
    }
}
