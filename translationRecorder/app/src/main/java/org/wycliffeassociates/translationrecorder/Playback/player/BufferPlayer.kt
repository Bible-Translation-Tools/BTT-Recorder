package org.wycliffeassociates.translationrecorder.Playback.player

import android.media.AudioTrack
import com.door43.tools.reporting.Logger

/**
 * Plays .Wav audio files
 */
class BufferPlayer (
    private val player: AudioTrack,
    private val minBufferSize: Int,
    private val mBufferProvider: BufferProvider,
    private var mOnCompleteListener: OnCompleteListener?
) {
    private var mPlaybackThread: Thread? = null
    private var mSessionLength = 0

    private lateinit var mAudioShorts: ShortArray

    interface OnCompleteListener {
        fun onComplete()
    }

    interface BufferProvider {
        fun onBufferRequested(shorts: ShortArray): Int
        fun onPauseAfterPlayingXSamples(pausedHeadPosition: Int)
    }

    init {
        initialize()
    }

    fun setOnCompleteListener(onCompleteListener: OnCompleteListener?): BufferPlayer {
        mOnCompleteListener = onCompleteListener
        initialize()
        return this
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    fun play(durationToPlay: Int) {
        if (isPlaying) {
            return
        }
        println("duration to play $durationToPlay")
        mSessionLength = durationToPlay
        player.setPlaybackHeadPosition(0)
        player.flush()
        player.setNotificationMarkerPosition(durationToPlay)
        player.play()
        mPlaybackThread = object : Thread() {
            override fun run() {
                //the starting position needs to beginning of the 16bit PCM data, not in the middle
                //position in the buffer keeps track of where we are for playback
                var shortsRetrieved = 1
                var shortsWritten = 0
                while (!mPlaybackThread!!.isInterrupted && this@BufferPlayer.isPlaying && shortsRetrieved > 0) {
                    shortsRetrieved = mBufferProvider.onBufferRequested(mAudioShorts)
                    shortsWritten = player.write(mAudioShorts, 0, minBufferSize)
                    when (shortsWritten) {
                        AudioTrack.ERROR_INVALID_OPERATION -> {
                            Logger.e(this.toString(), "ERROR INVALID OPERATION")
                        }

                        AudioTrack.ERROR_BAD_VALUE -> {
                            Logger.e(this.toString(), "ERROR BAD VALUE")
                        }

                        AudioTrack.ERROR -> {
                            Logger.e(this.toString(), "ERROR")
                        }
                    }
                }
                println("shorts written $shortsWritten")
            }
        }
        mPlaybackThread?.start()
    }

    fun initialize() {
        mAudioShorts = ShortArray(minBufferSize)
        if (mOnCompleteListener != null) {
            player.setPlaybackPositionUpdateListener(object :
                AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) {
                    finish()
                }

                override fun onPeriodicNotification(track: AudioTrack) {
                }
            })
        }
    }

    @Synchronized
    private fun finish() {
        println("marker reached")
        player.stop()
        mPlaybackThread?.interrupt()
        mOnCompleteListener?.onComplete()
    }

    //Simply pausing the audio track does not seem to allow the player to resume.
    @Synchronized
    fun pause() {
        player.pause()
        val location = player.playbackHeadPosition
        println("paused at $location")
        mBufferProvider.onPauseAfterPlayingXSamples(location)
        player.setPlaybackHeadPosition(0)
        player.flush()
    }

    fun exists(): Boolean {
        return if (player != null) {
            true
        } else false
    }

    @Synchronized
    fun stop() {
        if (isPlaying || isPaused) {
            player.pause()
            player.stop()
            player.flush()
            if (mPlaybackThread != null) {
                mPlaybackThread!!.interrupt()
            }
        }
    }

    @Synchronized
    fun release() {
        stop()
        player.release()
    }

    val isPlaying: Boolean
        get() = player.playState == AudioTrack.PLAYSTATE_PLAYING

    val isPaused: Boolean
        get() = player.playState == AudioTrack.PLAYSTATE_PAUSED

    @get:Throws(IllegalStateException::class)
    val playbackHeadPosition: Int
        get() = player.playbackHeadPosition

    val duration: Int
        get() = 0

    val adjustedDuration: Int
        get() = 0

    val adjustedLocation: Int
        get() = 0

    fun startSectionAt(i: Int) {
    }

    fun seekTo(i: Int) {
    }

    fun seekToEnd() {
    }

    fun seekToStart() {
    }

    fun checkIfShouldStop(): Boolean {
        return true
    }

    fun setOnlyPlayingSection(b: Boolean) {
    }

    fun stopSectionAt(i: Int) {
    }
}
