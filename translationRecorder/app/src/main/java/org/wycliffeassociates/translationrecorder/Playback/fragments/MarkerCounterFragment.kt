package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MarkerMediator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.VerseMarkerModeToggler
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentMarkerTopBarBinding

/**
 * Created by sarabiaj on 11/15/2016.
 */
class MarkerCounterFragment : Fragment() {
    private var mModeToggleCallback: VerseMarkerModeToggler? = null
    private var mMarkerMediator: MarkerMediator? = null

    private var _binding: FragmentMarkerTopBarBinding? = null
    private val binding get() = _binding!!

    private fun setMarkerMediator(mediator: MarkerMediator?) {
        mMarkerMediator = mediator
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mModeToggleCallback = context as VerseMarkerModeToggler
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMarkerTopBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun initViews() {
        binding.verseMarkerLabel.text = getString(R.string.left_title)
        binding.verseMarkerCount.text = mMarkerMediator?.numVersesRemaining().toString()
        binding.btnExitVerseMarkerMode.setOnClickListener {
            mModeToggleCallback?.onDisableVerseMarkerMode()
        }
    }

    @SuppressLint("SetTextI18n")
    fun decrementVersesRemaining() {
        binding.verseMarkerCount.text = mMarkerMediator?.numVersesRemaining().toString()
    }

    companion object {
        private const val KEY_MARKERS_REMAINING = "markers_remaining"

        fun newInstance(mediator: MarkerMediator?): MarkerCounterFragment {
            val f = MarkerCounterFragment()
            f.setMarkerMediator(mediator)
            return f
        }
    }
}
