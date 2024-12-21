package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.Playback.interfaces.AudioEditDelegator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.EditStateInformer
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.databinding.FragmentPlayerToolbarBinding
import org.wycliffeassociates.translationrecorder.widgets.PlaybackTimer

/**
 * Created by sarabiaj on 11/4/2016.
 */
class FragmentPlaybackTools : Fragment() {

    var mMediaController: MediaController? = null
    var mAudioEditDelegator: AudioEditDelegator? = null

    private var mTimer: PlaybackTimer? = null
    private var mUndoVisible = false

    private var _binding: FragmentPlayerToolbarBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mMediaController = context as MediaController
            mMediaController?.setOnCompleteListner {
                requireActivity().runOnUiThread {
                    Utils.swapViews(
                        arrayOf<View>(binding.btnPlay),
                        arrayOf<View>(binding.btnPause)
                    )
                }
            }
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement MediaController")
        }
        try {
            mAudioEditDelegator = context as AudioEditDelegator
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement AudioEditDelegator")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mMediaController = null
        mAudioEditDelegator = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerToolbarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attachListeners()
        initTimer(binding.playbackElapsed, binding.playbackDuration)
        Utils.swapViews(
            arrayOf<View>(
                binding.btnPlay
            ), arrayOf<View>(binding.btnPause)
        )
        if (mMediaController!!.isPlaying) {
            showPauseButton()
        } else {
            showPlayButton()
        }
        //restore undo button visibility from detach
        binding.btnUndo.visibility = if ((mUndoVisible)) View.VISIBLE else View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //hold the visibility of the undo button for switching contexts to verse marker mode and back
        mUndoVisible = binding.btnUndo.visibility == View.VISIBLE
        _binding = null
    }

    private fun initTimer(elapsed: TextView, duration: TextView) {
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

    private fun attachListeners() {
        attachMediaControllerListeners()
    }

    fun showPauseButton() {
        Utils.swapViews(
            arrayOf<View>(binding.btnPause),
            arrayOf<View>(binding.btnPlay)
        )
    }

    fun showPlayButton() {
        Utils.swapViews(
            arrayOf<View>(binding.btnPlay),
            arrayOf<View>(binding.btnPause)
        )
    }

    fun viewOnSetStartMarker() {
        Utils.swapViews(
            arrayOf<View>(binding.btnEndMark, binding.btnClear),
            arrayOf<View>(binding.btnStartMark)
        )
    }

    fun viewOnSetEndMarker() {
        Utils.swapViews(
            arrayOf<View>(binding.btnCut),
            arrayOf<View>(binding.btnEndMark, binding.btnStartMark)
        )
    }

    fun viewOnSetBothMarkers() {
        Utils.swapViews(
            arrayOf<View>(binding.btnCut),
            arrayOf<View>(binding.btnEndMark, binding.btnStartMark)
        )
    }

    fun viewOnCut() {
        Utils.swapViews(
            arrayOf<View>(binding.btnStartMark, binding.btnUndo),
            arrayOf<View>(binding.btnCut, binding.btnClear)
        )
    }

    fun viewOnUndo() {
        var toHide = arrayOf<View?>()
        if (!(mAudioEditDelegator as EditStateInformer).hasEdits()) {
            toHide = arrayOf(binding.btnUndo)
        }
        Utils.swapViews(arrayOf(), toHide)
    }

    fun viewOnClearMarkers() {
        Utils.swapViews(
            arrayOf<View>(binding.btnStartMark),
            arrayOf<View>(binding.btnClear, binding.btnCut, binding.btnEndMark)
        )
    }

    private fun attachMediaControllerListeners() {
        binding.btnPlay.setOnClickListener {
            showPauseButton()
            mMediaController!!.onMediaPlay()
        }

        binding.btnPause.setOnClickListener {
            showPlayButton()
            mMediaController?.onMediaPause()
        }

        binding.btnSkipBack.setOnClickListener { mMediaController?.onSeekBackward() }

        binding.btnSkipForward.setOnClickListener { mMediaController?.onSeekForward() }

        binding.btnStartMark.setOnClickListener {
            viewOnSetStartMarker()
            mAudioEditDelegator?.onDropStartMarker()
        }

        binding.btnEndMark.setOnClickListener {
            viewOnSetEndMarker()
            mAudioEditDelegator?.onDropEndMarker()
        }

        binding.btnDropVerseMarker.setOnClickListener {
            mAudioEditDelegator?.onDropVerseMarker()
        }

        binding.btnCut.setOnClickListener {
            viewOnCut()
            mAudioEditDelegator!!.onCut()
        }

        binding.btnUndo.setOnClickListener {
            mAudioEditDelegator?.onUndo()
            viewOnUndo()
        }

        binding.btnClear.setOnClickListener {
            viewOnClearMarkers()
            mAudioEditDelegator?.onClearMarkers()
        }

        binding.btnSave.setOnClickListener { mAudioEditDelegator?.onSave() }
    }

    fun onPlayerPaused() {
        if (_binding == null) return
        Utils.swapViews(
            arrayOf<View>(binding.btnPlay),
            arrayOf<View>(binding.btnPause)
        )
    }

    companion object {
        fun newInstance(): FragmentPlaybackTools {
            return FragmentPlaybackTools()
        }
    }
}
