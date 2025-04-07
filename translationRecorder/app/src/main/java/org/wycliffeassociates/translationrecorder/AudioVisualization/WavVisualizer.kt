package org.wycliffeassociates.translationrecorder.AudioVisualization

import org.wycliffeassociates.translationrecorder.AudioInfo
import org.wycliffeassociates.translationrecorder.AudioVisualization.Utils.U
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import java.nio.ShortBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class WavVisualizer(
    buffer: ShortBuffer,
    compressed: ShortBuffer?,
    numThreads: Int,
    private var mScreenWidth: Int,
    private var mScreenHeight: Int,
    minimapWidth: Int,
    cut: CutOp
) {
    private var mNumFramesOnScreen = DEFAULT_FRAMES_ON_SCREEN
    private var mUseCompressedFile = false
    private var mCanSwitch = compressed != null
    private val mSamples = FloatArray(mScreenWidth * 4)
    private val mMinimap = FloatArray(minimapWidth * 4)
    private var mAccessor = AudioFileAccessor(compressed, buffer, cut)

    private var mNumThreads = numThreads
    private var mThreadResponse: MutableList<LinkedBlockingQueue<Int>> = ArrayList(mNumThreads)
    private var mRunnable: Array<VisualizerRunnable?> = arrayOfNulls(mNumThreads)
    private var mThreads = ThreadPoolExecutor(
        mNumThreads,
        mNumThreads,
        20,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(mNumThreads)
    )

    init {
        mThreads.allowCoreThreadTimeOut(true)
        for (i in 0 until mNumThreads) {
            mThreadResponse.add(LinkedBlockingQueue(1))
            mRunnable[i] = VisualizerRunnable()
        }
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        mThreads.shutdown()
        mThreads.purge()
    }

    fun enableCompressedFileNextDraw(compressed: ShortBuffer?) {
        mAccessor.setCompressed(compressed)
        mCanSwitch = true
    }

    fun getMinimap(minimapHeight: Int, minimapWidth: Int, durationFrames: Int): FloatArray {
        //selects the proper buffer to use
        val useCompressed = mCanSwitch && mNumFramesOnScreen > AudioInfo.COMPRESSED_FRAMES_ON_SCREEN
        mAccessor.switchBuffers(useCompressed)

        var pos = 0
        var index = 0

        val incrementTemp = AudioFileAccessor.getIncrement(
            useCompressed,
            durationFrames.toDouble(),
            minimapWidth.toDouble()
        )
        val leftover = incrementTemp - floor(incrementTemp).toInt()
        var count = 0.0
        var increment = floor(incrementTemp).toInt()
        var leapedInc = false
        for (i in 0 until minimapWidth) {
            var max = Double.MIN_VALUE
            var min = Double.MAX_VALUE
            if (count > 1) {
                count -= 1.0
                increment++
                leapedInc = true
            }
            for (j in 0 until increment) {
                if (pos >= mAccessor.size()) {
                    break
                }
                val value = mAccessor.get(pos)
                max = if ((max < value.toDouble())) value.toDouble() else max
                min = if ((min > value.toDouble())) value.toDouble() else min
                pos++
            }
            if (leapedInc) {
                increment--
                leapedInc = false
            }
            count += leftover
            mMinimap[index] = index.toFloat() / 4
            mMinimap[index + 1] = U.getValueForScreen(max, minimapHeight)
            mMinimap[index + 2] = index.toFloat() / 4
            mMinimap[index + 3] = U.getValueForScreen(min, minimapHeight)
            index += 4
        }

        return mMinimap
    }

    fun getDataToDraw(frame: Int): FloatArray {
        mNumFramesOnScreen = computeNumFramesOnScreen(USER_SCALE)
        //based on the user scale, determine which buffer waveData should be
        mUseCompressedFile = shouldUseCompressedFile(mNumFramesOnScreen)
        mAccessor.switchBuffers(mUseCompressedFile)

        //get the number of array indices to skip over- the array will likely contain more data than one pixel can show
        val increment = getIncrement(mNumFramesOnScreen)
        val framesToSubtract = framesBeforePlaybackLine(mNumFramesOnScreen)
        val locAndTime = mAccessor.indexAfterSubtractingFrame(framesToSubtract, frame)
        var startPosition = locAndTime[0]
        val newTime = locAndTime[1]
        var index = initializeSamples(mSamples, startPosition, newTime)
        // in the event that the actual start position ends up being negative
        // (such as from shifting forward due to playback being at the start of the file)
        // it should be set to zero (and the buffer will already be initialized with some zeros,
        // with index being the index of where to resume placing data
        startPosition = max(0, startPosition)
        val end = mSamples.size / 4
        val iterations = end - index / 4
        val rangePerThread = iterations / mNumThreads

        for (i in mThreadResponse.indices) {
            mThreads.submit(
                mRunnable[i]?.newState(
                    index / 4 + (rangePerThread * i),
                    index / 4 + (rangePerThread * (i + 1)),
                    mThreadResponse[i],
                    mAccessor,
                    mSamples,
                    index + (rangePerThread * mNumThreads * i),
                    mScreenHeight,
                    startPosition + (increment * rangePerThread * i).toInt(),
                    increment,
                    i
                )
            )
        }

        for (integers in mThreadResponse) {
            try {
                val returnIdx = integers.take()
                index = max(returnIdx, index)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        //zero out the rest of the array
        //index -= 4
        for (i in index until mSamples.size) {
            mSamples[i] = 0f
        }

        return mSamples
    }

    private fun initializeSamples(
        samples: FloatArray,
        startPosition: Int,
        framesUntilZero: Int
    ): Int {
        var mFramesUntilZero = framesUntilZero
        if (startPosition <= 0) {
            var numberOfZeros = 0
            if (mFramesUntilZero < 0) {
                mFramesUntilZero *= -1
                val fpp = (mNumFramesOnScreen) / mScreenWidth.toDouble()
                numberOfZeros = Math.round(mFramesUntilZero / fpp).toInt()
            }
            var index = 0
            for (i in 0 until numberOfZeros) {
                samples[index] = index.toFloat() / 4
                samples[index + 1] = 0f
                samples[index + 2] = index.toFloat() / 4
                samples[index + 3] = 0f
                index += 4
            }
            return index
        }
        return 0
    }

    private fun shouldUseCompressedFile(numFramesOnScreen: Int): Boolean {
        return numFramesOnScreen >= AudioInfo.COMPRESSED_FRAMES_ON_SCREEN && mCanSwitch
    }

    private fun framesBeforePlaybackLine(numFramesOnScreen: Int): Int {
        return numFramesOnScreen / 8
    }

    private fun computeSampleStartPosition(startFrame: Int): Int {
        var mStartFrame = startFrame
        if (mUseCompressedFile) {
            mStartFrame /= 25
        }
        return mStartFrame
    }

    private fun getIncrement(numFramesOnScreen: Int): Float {
        var increment = numFramesOnScreen / mScreenWidth.toFloat()
        if (mUseCompressedFile) {
            increment /= 25.0f
        }
        return increment
    }

    private fun computeNumFramesOnScreen(userScale: Float): Int {
        val numSecondsOnScreen = Math.round(mNumFramesOnScreen * userScale)
        return max(
            numSecondsOnScreen,
            AudioInfo.COMPRESSED_SECONDS_ON_SCREEN
        )
    }

    companion object {
        private const val USER_SCALE = 1f
        private const val DEFAULT_FRAMES_ON_SCREEN = 441000

        fun addHighAndLowToDrawingArray(
            accessor: AudioFileAccessor,
            samples: FloatArray,
            beginIdx: Int,
            endIdx: Int,
            index: Int,
            screenHeight: Int
        ): Int {
            var mIndex = index
            var addedVal = false
            var max = Double.MIN_VALUE
            var min = Double.MAX_VALUE

            //loop over the indicated chunk of data to extract out the high and low in that section, then store it in samples
            for (i in beginIdx .. min(accessor.size(), endIdx)) {
                val value = accessor.get(i)
                max = if ((max < value.toDouble())) value.toDouble() else max
                min = if ((min > value.toDouble())) value.toDouble() else min
            }
            if (samples.size > mIndex + 4) {
                samples[mIndex] = mIndex.toFloat() / 4
                samples[mIndex + 1] = U.getValueForScreen(max, screenHeight)
                samples[mIndex + 2] = mIndex.toFloat() / 4
                samples[mIndex + 3] = U.getValueForScreen(min, screenHeight)
                mIndex += 4
                addedVal = true
            }

            //returns the end of relevant data in the buffer
            return if ((addedVal)) mIndex else 0
        }
    }
}