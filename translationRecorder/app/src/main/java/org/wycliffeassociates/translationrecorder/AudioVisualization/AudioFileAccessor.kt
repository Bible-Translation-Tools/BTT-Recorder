package org.wycliffeassociates.translationrecorder.AudioVisualization

import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.AudioInfo
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import java.nio.ShortBuffer

/**
 * Created by sarabiaj on 1/12/2016.
 */
/**
 * Keywords:
 * Relative - index or time with cuts abstracted away
 * Absolute - index or time with cut data still existing
 */
class AudioFileAccessor(
    private var mCompressed: ShortBuffer?,
    private var mUncompressed: ShortBuffer,
    private var mCut: CutOp
) {
    private var mUseCmp = mCompressed != null

    fun switchBuffers(cmpReady: Boolean) {
        mUseCmp = cmpReady
    }

    fun setCompressed(compressed: ShortBuffer?) {
        mCompressed = compressed
    }

    //FIXME: should not be returning 0 if out of bounds access, there's a bigger issue here
    fun get(idx: Int): Short {
        val loc = mCut.relativeLocToAbsolute(idx, mUseCmp)
        val value: Short
        if (mUseCmp) {
            if (loc < 0) {
                Logger.e(
                    this.toString(),
                    "ERROR, tried to access a negative location from the compressed buffer!"
                )
                return 0
            } else if (loc >= mCompressed!!.capacity()) {
                Logger.e(
                    this.toString(),
                    "ERROR, tried to access a negative location from the compressed buffer!"
                )
                return 0
            }
            value = mCompressed!![loc]
        } else {
            if (loc < 0) {
                Logger.e(
                    this.toString(),
                    "ERROR, tried to access a negative location from the compressed buffer!"
                )
                return 0
            } else if (loc >= mUncompressed.capacity()) {
                Logger.e(
                    this.toString(),
                    "ERROR, tried to access a negative location from the compressed buffer!"
                )
                return 0
            }
            value = mUncompressed[loc]
        }
        return value
    }

    fun size(): Int {
        if (mUseCmp) {
            return mCompressed!!.capacity() - mCut.sizeFrameCutCmp
        }
        return mUncompressed.capacity() - mCut.sizeFrameCutUncmp
    }

    fun indexAfterSubtractingFrame(framesToSubtract: Int, currentFrame: Int): IntArray {
        var frame = currentFrame
        frame -= framesToSubtract
        val loc = frameToIndex(frame)
        val locAndTime = IntArray(2)
        locAndTime[0] = loc
        locAndTime[1] = frame
        return locAndTime
    }

    fun frameToIndex(frame: Int): Int {
        var mFrame = frame
        if (mUseCmp) {
            mFrame /= 25
        }
        return mFrame
    }

    fun relativeIndexToAbsolute(idx: Int): Int {
        return mCut.relativeLocToAbsolute(idx, mUseCmp)
    }

    fun absoluteIndexToRelative(idx: Int): Int {
        return mCut.absoluteLocToRelative(idx, mUseCmp)
    }

    companion object {
        fun fileIncrement(): Int {
            return AudioInfo.COMPRESSION_RATE
        }

        //FIXME: rounding will compound error in long files, resulting in pixels being off
        //used for minimap- this is why the duration matters
        fun getIncrement(useCmp: Boolean, adjustedDuration: Double, screenWidth: Double): Double {
            return if (useCmp) {
                compressedIncrement(adjustedDuration, screenWidth)
            } else {
                uncompressedIncrement(adjustedDuration, screenWidth)
            }
        }

        //used for minimap
        private fun uncompressedIncrement(adjustedDuration: Double, screenWidth: Double): Double {
            return (adjustedDuration / screenWidth)
        }

        //used for minimap
        private fun compressedIncrement(adjustedDuration: Double, screenWidth: Double): Double {
            return (uncompressedIncrement(adjustedDuration, screenWidth) / 25f)
        }
    }
}
