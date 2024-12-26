package org.wycliffeassociates.translationrecorder.Playback.player

import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import org.wycliffeassociates.translationrecorder.Playback.player.BufferPlayer.BufferProvider
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Created by sarabiaj on 10/27/2016.
 */
class AudioBufferProvider(
    private var mAudio: ShortBuffer,
    private var mCutOp: CutOp
) : BufferProvider {

    private var lastRequestedPosition: Int = 0

    var startPosition: Int = 0
        private set
    var mark: Int = 0
        private set

    init {
        //audio is written in little endian, 16 bit PCM. Read as shorts therefore to comply with
        //Android's AudioTrack Spec
        mAudio.position(0)
    }

    @Synchronized
    fun reset() {
        mAudio.position(mark)
        startPosition = mark
    }

    //Keep a variable for mark rather than use the Buffer api- a call to position
    @Synchronized
    fun mark(position: Int) {
        mark = position
    }

    /**
     * Clears the mark by setting it to zero and resuming the position
     */
    @Synchronized
    fun clearMark() {
        mark = 0
    }

    @Synchronized
    fun clearLimit() {
        mAudio.limit(mAudio.capacity())
    }

    override fun onPauseAfterPlayingXSamples(pausedHeadPosition: Int) {
        mAudio.position(startPosition)
        val skip = ShortArray(pausedHeadPosition)
        get(skip)
        if (mAudio.position() == mAudio.limit()) {
            reset()
            Logger.e(this.toString(), "Paused right at the limit")
        }
        startPosition = mAudio.position()
    }

    val sizeOfNextSession: Int
        get() {
            val current = mCutOp.absoluteLocToRelative(mAudio.position(), false)
            val end = mCutOp.absoluteLocToRelative(mAudio.limit(), false)
            return end - current
        }

    override fun onBufferRequested(shorts: ShortArray): Int {
        lastRequestedPosition = mAudio.position()
        return get(shorts)
    }

    private fun get(shorts: ShortArray): Int {
        val shortsWritten = if (mCutOp.cutExistsInRange(mAudio.position(), shorts.size)) {
            getWithSkips(shorts)
        } else {
            getWithoutSkips(shorts)
        }
        if (shortsWritten < shorts.size) {
            for (i in shortsWritten until shorts.size) {
                shorts[i] = 0
            }
        }
        return shortsWritten
    }

    private fun getWithoutSkips(shorts: ShortArray): Int {
        val size = shorts.size
        var shortsWritten = 0
        var brokeEarly = false
        for (i in 0 until size) {
            if (!mAudio.hasRemaining()) {
                brokeEarly = true
                shortsWritten = i
                break
            }
            shorts[i] = mAudio.get()
        }
        return if (brokeEarly) {
            shortsWritten
        } else {
            size
        }
    }

    private fun getWithSkips(shorts: ShortArray): Int {
        val size = shorts.size
        var skip: Int
        var end = 0
        var brokeEarly = false
        for (i in 0 until size) {
            if (!mAudio.hasRemaining()) {
                brokeEarly = true
                end = i
                break
            }
            skip = mCutOp.skip(mAudio.position())
            if (skip != -1) {
                //Logger.i(this.toString(), "Location is " + getLocationMs() + "position is " + mAudio.position());
                var start = skip
                //make sure the playback start is within the bounds of the file's capacity
                start = max(
                    min(
                        mAudio.capacity().toDouble(),
                        start.toDouble()
                    ), 0.0
                ).toInt()
                mAudio.position(start)
                //Logger.i(this.toString(), "Location is now " + getLocationMs() + "position is " + mAudio.position());
            }
            //check a second time in case there was a skip
            if (!mAudio.hasRemaining()) {
                brokeEarly = true
                end = i
                break
            }
            shorts[i] = mAudio.get()
        }
        return if (brokeEarly) {
            end
        } else {
            shorts.size
        }
    }

    @Synchronized
    fun setPosition(position: Int) {
        mAudio.position(position)
        startPosition = position
    }

    val duration: Int
        get() = mAudio.capacity()

    @set:Synchronized
    var limit: Int
        get() = mAudio.limit()
        set(limit) {
            mAudio.limit(limit)
            reset()
        }
}
