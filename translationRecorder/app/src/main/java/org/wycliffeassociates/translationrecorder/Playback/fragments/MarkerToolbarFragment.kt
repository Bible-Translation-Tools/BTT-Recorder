package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController
import org.wycliffeassociates.translationrecorder.Playback.interfaces.VerseMarkerModeToggler
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.databinding.FragmentMarkerToolbarBinding
import org.wycliffeassociates.translationrecorder.widgets.PlaybackTimer

/**
 * Created by sarabiaj on 11/15/2016.
 */
class MarkerToolbarFragment : Fragment() {

    interface OnMarkerPlacedListener {
        fun onMarkerPlaced()
    }

    private var mOnMarkerPlacedListener: OnMarkerPlacedListener? = null
    private var mModeToggleCallback: VerseMarkerModeToggler? = null
    private var mMediaController: MediaController? = null
    private var mTimer: PlaybackTimer? = null

    private var _binding: FragmentMarkerToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mModeToggleCallback = context as VerseMarkerModeToggler
        mMediaController = context as MediaController
        mOnMarkerPlacedListener = context as OnMarkerPlacedListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMarkerToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initTimer(binding.playbackElapsed, binding.playbackDuration)
        if (mMediaController!!.isPlaying) {
            showPauseButton()
        } else {
            showPlayButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun showPlayButton() {
        if (_binding == null) return
        Utils.swapViews(
            arrayOf(binding.btnPlay),
            arrayOf(binding.btnPause)
        )
    }

    fun showPauseButton() {
        if (_binding == null) return
        Utils.swapViews(
            arrayOf(binding.btnPause),
            arrayOf(binding.btnPlay)
        )
    }

    private fun initViews() {
        binding.btnPlay.setOnClickListener {
            showPauseButton()
            mMediaController?.onMediaPlay()
        }

        binding.btnPause.setOnClickListener {
            showPlayButton()
            mMediaController?.onMediaPause()
        }

        binding.btnSkipBack.setOnClickListener {
            mMediaController?.onSeekBackward()
        }

        binding.btnSkipForward.setOnClickListener {
            mMediaController?.onSeekForward()
        }

        binding.btnDropVerseMarker.setOnClickListener {
            mOnMarkerPlacedListener?.onMarkerPlaced()
        }
    }

    private fun initTimer(elapsed: TextView?, duration: TextView?) {
        mTimer = PlaybackTimer(elapsed, duration)
        mTimer?.setElapsed(mMediaController!!.locationMs)
        mTimer?.setDuration(mMediaController!!.durationMs)
    }

    fun onLocationUpdated(ms: Int) {
        mTimer?.setElapsed(ms)
    }

    fun onDurationUpdated(ms: Int) {
        mTimer?.setDuration(ms)
    }

    companion object {
        fun newInstance(): MarkerToolbarFragment {
            val f = MarkerToolbarFragment()
            return f
        }
    }
}
