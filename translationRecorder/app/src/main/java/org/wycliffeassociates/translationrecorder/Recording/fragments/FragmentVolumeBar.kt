package org.wycliffeassociates.translationrecorder.Recording.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.databinding.FragmentVolumeBarBinding

/**
 * Created by sarabiaj on 2/20/2017.
 */
class FragmentVolumeBar : Fragment() {

    private var _binding: FragmentVolumeBarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentVolumeBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateDb(db: Int) {
        binding.volumeBar1.setDb(db)
        binding.volumeBar1.postInvalidate()
    }

    companion object {
        fun newInstance(): FragmentVolumeBar {
            val f = FragmentVolumeBar()
            return f
        }
    }
}
