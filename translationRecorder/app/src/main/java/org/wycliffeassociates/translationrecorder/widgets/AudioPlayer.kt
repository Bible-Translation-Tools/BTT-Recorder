package org.wycliffeassociates.translationrecorder.widgets

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.door43.tools.reporting.Logger
import java.io.File
import java.io.IOException

/**
 * Created by sarabiaj on 7/7/2016.
 */
class AudioPlayer(
    private var elapsedView: TextView? = null,
    private var durationView: TextView? = null,
    private var playPauseBtn: ImageButton? = null,
    private var seekBar: SeekBar? = null
) {

    // Attributes
    private val mMediaPlayer: MediaPlayer = MediaPlayer()
    private var mHandler: Handler = Handler(Looper.getMainLooper())

    private var mCurrentProgress = 0
    private var mDuration = 0

    // State
    private var mPlayerReleased = false

    // Getters
    var isLoaded: Boolean = false
        private set

    init {
        attachSeekBarListener()
        attachMediaPlayerListener()
    }

    // Setters
    private fun setSeekBarView(seekBar: SeekBar) {
        this.seekBar = seekBar
        if (isLoaded) {
            this.seekBar!!.max = mMediaPlayer.duration
        }
        updateSeekBar(mCurrentProgress)
    }

    private fun setPlayPauseBtn(playPauseBtn: ImageButton) {
        this.playPauseBtn = playPauseBtn
        togglePlayPauseButton(mMediaPlayer.isPlaying)
    }

    private fun setElapsedView(elapsedView: TextView) {
        this.elapsedView = elapsedView
        updateElapsedView(mCurrentProgress)
        attachSeekBarListener()
    }

    private fun setDurationView(durationView: TextView?) {
        this.durationView = durationView
        updateDurationView(mDuration)
        attachMediaPlayerListener()
    }

    val isPlaying: Boolean
        get() = mMediaPlayer.isPlaying


    // Private Methods
    private fun attachSeekBarListener() {
        seekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMediaPlayer.seekTo(progress)
                    updateElapsedView(progress)
                }
            }
        })
    }

    private fun attachMediaPlayerListener() {
        mMediaPlayer.setOnCompletionListener {
            togglePlayPauseButton(false)
            seekBar?.let {
                val max = it.max
                updateDurationView(max)
                updateSeekBar(0)
                updateElapsedView(0)
            }
            mMediaPlayer.seekTo(0)
        }
        mMediaPlayer.setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
            Logger.e(
                mp.toString(),
                "onError called, error what is $what error extra is $extra"
            )
            false
        }
    }

    private fun togglePlayPauseButton(isPlaying: Boolean) {
        playPauseBtn?.isActivated = isPlaying
    }

    @SuppressLint("DefaultLocale")
    private fun convertTimeToString(time: Int): String {
        return String.format(
            "%02d:%02d:%02d",
            time / 3600000,
            (time / 60000) % 60,
            (time / 1000) % 60
        )
    }

    // Public API
    fun refreshView(
        elapsedView: TextView,
        durationView: TextView,
        playPauseBtn: ImageButton,
        seekBar: SeekBar
    ) {
        setSeekBarView(seekBar)
        setElapsedView(elapsedView)
        setDurationView(durationView)
        setPlayPauseBtn(playPauseBtn)

        attachSeekBarListener()
        attachMediaPlayerListener()
    }

    fun loadFile(file: File) {
        try {
            togglePlayPauseButton(false)
            mMediaPlayer.setDataSource(file.absolutePath)
            isLoaded = true
            mMediaPlayer.prepare()
            seekBar?.let {
                mDuration = mMediaPlayer.duration
                it.max = mDuration
                updateDurationView(mDuration)
            }
        } catch (e: IOException) {
            Logger.w(this.toString(), "loading a file threw an IO exception")
            e.printStackTrace()
        }
    }

    fun play() {
        if (!mMediaPlayer.isPlaying) {
            try {
                togglePlayPauseButton(true)
                updateSeekBar(0)

                mMediaPlayer.start()

                val loop: Runnable = object : Runnable {
                    override fun run() {
                        if (!mPlayerReleased && mMediaPlayer.isPlaying) {
                            mCurrentProgress = mMediaPlayer.currentPosition
                            updateElapsedView(mCurrentProgress)
                            updateSeekBar(mCurrentProgress)
                        }
                        mHandler.postDelayed(this, 200)
                    }
                }
                loop.run()
            } catch (e: IllegalStateException) {
                Logger.w(this.toString(), "playing threw an illegal state exception")
            }
        }
    }

    fun pause() {
        if (!mPlayerReleased && mMediaPlayer.isPlaying) {
            try {
                mMediaPlayer.pause()
                togglePlayPauseButton(false)
            } catch (e: IllegalStateException) {
                Logger.w(this.toString(), "Pausing threw an illegal state exception")
            }
        }
    }

    fun reset() {
        synchronized(mMediaPlayer) {
            if (!mPlayerReleased && mMediaPlayer.isPlaying) {
                mMediaPlayer.pause()
            }
            mMediaPlayer.reset()
            isLoaded = false
            updateSeekBar(0)
            updateElapsedView(0)
        }
    }

    fun cleanup() {
        synchronized(mMediaPlayer) {
            if (!mPlayerReleased) {
                if (mMediaPlayer.isPlaying) {
                    mMediaPlayer.stop()
                }
                mMediaPlayer.reset()
                mMediaPlayer.release()
            }
            mPlayerReleased = true
        }
    }

    fun updateElapsedView(elapsed: Int) {
        elapsedView?.text = convertTimeToString(elapsed)
        elapsedView?.invalidate()
    }

    fun updateDurationView(duration: Int) {
        durationView?.text = convertTimeToString(duration)
        durationView?.invalidate()
    }

    fun updateSeekBar(progress: Int) {
        seekBar?.progress = progress
        seekBar?.invalidate()
    }
}
