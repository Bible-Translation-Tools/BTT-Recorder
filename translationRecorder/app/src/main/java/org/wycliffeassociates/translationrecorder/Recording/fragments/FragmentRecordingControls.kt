package org.wycliffeassociates.translationrecorder.Recording.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.AudioVisualization.RecordingTimer
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentRecordingControlsBinding

/**
 * Created by sarabiaj on 2/20/2017.
 */
class FragmentRecordingControls : Fragment() {
    enum class Mode {
        RECORDING_MODE,
        INSERT_MODE
    }

    var timer: RecordingTimer? = null
    lateinit var mHandler: Handler
    var mRecordingControlCallback: RecordingControlCallback? = null
    private var isRecording = false
    private var isPausedRecording = false
    private var mMode: Mode? = null

    interface RecordingControlCallback {
        fun onStartRecording()
        fun onPauseRecording()
        fun onStopRecording()
    }

    private fun setMode(mode: Mode) {
        mMode = mode
    }

    private var _binding: FragmentRecordingControlsBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RecordingControlCallback) {
            mRecordingControlCallback = context
        } else {
            throw RuntimeException(
                "Attempted to attach to an activity" +
                        " that does not implement RecordingControlCallback"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecordingControlCallback = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentRecordingControlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mHandler = Handler(Looper.getMainLooper())
        findViews()
        timer = RecordingTimer()
        if (mMode == Mode.INSERT_MODE) {
            binding.toolbar.setBackgroundColor(resources.getColor(R.color.secondary))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun findViews() {
        binding.btnRecording.setOnClickListener(btnClick)
        binding.btnStop.setOnClickListener(btnClick)
        binding.btnPauseRecording.setOnClickListener(btnClick)
    }

    fun startTimer() {
        timer?.startTimer()
    }

    fun pauseTimer() {
        timer?.pause()
    }

    fun resumeTimer() {
        timer?.resume()
    }

    @SuppressLint("DefaultLocale")
    fun updateTime() {
        val t = timer!!.timeElapsed
        val time = String.format("%02d:%02d:%02d", t / 3600000, (t / 60000) % 60, (t / 1000) % 60)
        mHandler.post {
            binding.timerView.text = time
            binding.timerView.invalidate()
        }
    }

    fun startRecording() {
        isRecording = true
        if (!isPausedRecording) {
            startTimer()
        } else {
            resumeTimer()
            isPausedRecording = false
        }
        val toShow = intArrayOf(R.id.btnPauseRecording)
        val toHide = intArrayOf(R.id.btnRecording, R.id.btnStop)
        swapViews(toShow, toHide)
        mRecordingControlCallback?.onStartRecording()
    }

    fun stopRecording() {
        if (isPausedRecording || isRecording) {
            mRecordingControlCallback?.onStopRecording()
            isRecording = false
            isPausedRecording = false
        }
    }

    fun pauseRecording() {
        isPausedRecording = true
        isRecording = false
        pauseTimer()
        val toShow = intArrayOf(R.id.btnRecording, R.id.btnStop)
        val toHide = intArrayOf(R.id.btnPauseRecording)
        swapViews(toShow, toHide)
        mRecordingControlCallback?.onPauseRecording()
    }

    fun swapViews(toShow: IntArray, toHide: IntArray) {
        for (v in toShow) {
            val view = binding.root.findViewById<View>(v)
            if (view != null) {
                view.visibility = View.VISIBLE
            }
        }
        for (v in toHide) {
            val view = binding.root.findViewById<View>(v)
            if (view != null) {
                view.visibility = View.INVISIBLE
            }
        }
    }

    private val btnClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.btnRecording -> {
                Logger.w(this.toString(), "User pressed Record")
                startRecording()
            }
            R.id.btnStop -> {
                Logger.w(this.toString(), "User pressed Stop")
                stopRecording()
            }
            R.id.btnPauseRecording -> {
                Logger.w(this.toString(), "User pressed Pause")
                pauseRecording()
            }
        }
    }

    companion object {
        fun newInstance(mode: Mode): FragmentRecordingControls {
            val f = FragmentRecordingControls()
            f.setMode(mode)
            return f
        }
    }
}
