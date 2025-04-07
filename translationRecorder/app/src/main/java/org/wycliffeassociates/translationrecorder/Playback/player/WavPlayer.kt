package org.wycliffeassociates.translationrecorder.Playback.player

import android.media.AudioTrack
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import org.wycliffeassociates.translationrecorder.wav.WavCue
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Created by sarabiaj on 10/28/2016.
 */
/**
 * Controls interaction between BufferPlayer and AudioBufferProvider. The BufferPlayer simply plays audio
 * that is passed to it, and the BufferProvider manages processing audio to get the proper buffer to onPlay
 * based on performing operations on the audio buffer (such as cut).
 */
class WavPlayer(
    audioTrack: AudioTrack,
    trackBufferSize: Int,
    mAudioBuffer: ShortBuffer,
    private val mOperationStack: CutOp,
    cueList: List<WavCue>
) {
    private val mCueList: MutableList<WavCue> = ArrayList(cueList)

    private var mPlayer: BufferPlayer
    private val mBufferProvider = AudioBufferProvider(mAudioBuffer, mOperationStack)
    private var mOnCompleteListener: OnCompleteListener? = null

    interface OnCompleteListener {
        fun onComplete()
    }

    companion object {
        private const val EPSILON = 200
    }

    init {
        mPlayer = BufferPlayer(
            audioTrack,
            trackBufferSize,
            mBufferProvider,
            object : BufferPlayer.OnCompleteListener {
                override fun onComplete() {
                    if (mOnCompleteListener != null) {
                        mBufferProvider.reset()
                        mOnCompleteListener?.onComplete()
                    }
                }
            }
        )
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    fun seekNext() {
        var seekLocation = absoluteDurationInFrames
        val currentLocation = absoluteLocationInFrames
        var location: Int
        for (i in mCueList.indices) {
            location = mCueList[i].location
            if (currentLocation < location) {
                seekLocation = location
                break
            }
        }
        seekToAbsolute(
            min(
                seekLocation.toDouble(),
                mBufferProvider.limit.toDouble()
            ).toInt()
        )
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    fun seekPrevious() {
        var seekLocation = 0
        val currentLocation = absoluteLocationInFrames
        var location: Int
        for (i in mCueList.indices.reversed()) {
            location = mCueList[i].location
            //if playing, you won't be able to keep pressing back, it will clamp to the last marker
            if (!isPlaying && currentLocation > location) {
                seekLocation = location
                break
            } else if (currentLocation - EPSILON > location) { //epsilon here is to prevent that clamping
                seekLocation = location
                break
            }
        }
        seekToAbsolute(
            max(
                seekLocation.toDouble(),
                mBufferProvider.mark.toDouble()
            ).toInt()
        )
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    fun seekToAbsolute(absoluteFrame: Int) {
        var frame = absoluteFrame
        if (frame > absoluteDurationInFrames || frame < 0) {
            return
        }
        frame = max(frame.toDouble(), mBufferProvider.mark.toDouble()).toInt()
        frame = min(frame.toDouble(), mBufferProvider.limit.toDouble()).toInt()
        val wasPlaying = mPlayer.isPlaying
        pause()
        mBufferProvider.setPosition(frame)
        if (wasPlaying) {
            play()
        }
    }

    @Synchronized
    fun setCueList(cueList: List<WavCue>) {
        mCueList.clear()
        mCueList.addAll(cueList)
    }

    @Throws(IllegalStateException::class)
    fun play() {
        if (absoluteLocationInFrames == loopEnd) {
            mBufferProvider.reset()
        }
        mPlayer.play(mBufferProvider.sizeOfNextSession)
    }

    fun pause() {
        mPlayer.pause()
    }

    fun setOnCompleteListener(onCompleteListener: OnCompleteListener?) {
        mOnCompleteListener = onCompleteListener
    }

    @get:Throws(IllegalStateException::class)
    val absoluteLocationMs: Int
        get() = (absoluteLocationInFrames / 44.1).toInt()

    @get:Throws(IllegalStateException::class)
    val absoluteDurationMs: Int
        get() = (mBufferProvider.duration / 44.1).toInt()

    @get:Throws(IllegalStateException::class)
    val absoluteLocationInFrames: Int
        get() {
            val relativeLocationOfHead = mOperationStack.absoluteLocToRelative(
                mBufferProvider.startPosition,
                false
            ) + mPlayer.playbackHeadPosition
            val absoluteLocationOfHead =
                mOperationStack.relativeLocToAbsolute(relativeLocationOfHead, false)
            return absoluteLocationOfHead
        }

    @get:Throws(IllegalStateException::class)
    val absoluteDurationInFrames: Int
        get() = mBufferProvider.duration

    @get:Throws(IllegalStateException::class)
    val relativeLocationMs: Int
        get() = (relativeLocationInFrames / 44.1).toInt()

    @get:Throws(IllegalStateException::class)
    val relativeDurationMs: Int
        get() = ((mBufferProvider.duration - mOperationStack.sizeFrameCutUncmp) / 44.1).toInt()

    @get:Throws(IllegalStateException::class)
    val relativeDurationInFrames: Int
        get() = mBufferProvider.duration - mOperationStack.sizeFrameCutUncmp

    @get:Throws(IllegalStateException::class)
    val relativeLocationInFrames: Int
        get() = mOperationStack.absoluteLocToRelative(absoluteLocationInFrames, false)

    val isPlaying: Boolean
        get() = mPlayer.isPlaying

    var loopStart: Int
        get() = mBufferProvider.mark
        set(frame) {
            if (frame > mBufferProvider.limit) {
                val oldLimit = mBufferProvider.limit
                clearLoopPoints()
                mBufferProvider.mark(oldLimit)
                mBufferProvider.limit = frame
                mBufferProvider.reset()
            } else {
                mBufferProvider.mark(frame)
            }
        }

    var loopEnd: Int
        get() = mBufferProvider.limit
        set(frame) {
            if (frame < mBufferProvider.mark) {
                val oldMark = mBufferProvider.mark
                clearLoopPoints()
                mBufferProvider.mark(oldMark)
                mBufferProvider.limit = oldMark
            } else {
                mBufferProvider.limit = frame
                mBufferProvider.reset()
            }
        }

    fun clearLoopPoints() {
        mBufferProvider.clearMark()
        mBufferProvider.clearLimit()
    }
}
