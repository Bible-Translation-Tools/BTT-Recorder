package org.wycliffeassociates.translationrecorder.AudioVisualization

import java.util.concurrent.BlockingQueue
import kotlin.math.floor
import kotlin.math.max

/**
 * Created by sarabiaj on 12/20/2016.
 */
class VisualizerRunnable : Runnable {
    private var mStart: Int = 0
    private var mEnd: Int = 0
    private var mStartPosition: Int = 0
    private var mIndex: Int = 0
    private var mScreenHeight: Int = 0
    private var mIncrement: Float = 0f
    private var mTid: Int = 0

    private lateinit var mAccessor: AudioFileAccessor
    private lateinit var mResponse: BlockingQueue<Int>
    private lateinit var mSamples: FloatArray

    fun newState(
        start: Int,
        end: Int,
        response: BlockingQueue<Int>,
        accessor: AudioFileAccessor,
        samples: FloatArray,
        index: Int,
        screenHeight: Int,
        startPosition: Int,
        increment: Float,
        tid: Int
    ): VisualizerRunnable {
        mStart = start
        mEnd = end
        mResponse = response
        mAccessor = accessor
        mSamples = samples
        mIndex = index
        mScreenHeight = screenHeight
        mStartPosition = startPosition
        mIncrement = increment
        mTid = tid
        return this
    }

    override fun run() {
        var wroteData = false
        var resetIncrementNextIteration = false
        var offset = 0f

        for (i in mStart until mEnd) {
            if (mStartPosition > mAccessor.size()) {
                break
            }
            mIndex = max(
                WavVisualizer.addHighAndLowToDrawingArray(
                    mAccessor,
                    mSamples,
                    mStartPosition,
                    mStartPosition + mIncrement.toInt(),
                    mIndex,
                    mScreenHeight
                ).toDouble(),
                mIndex.toDouble()
            ).toInt()

            mStartPosition += floor(mIncrement.toDouble()).toInt()
            if (resetIncrementNextIteration) {
                resetIncrementNextIteration = false
                mIncrement--
                offset--
            }
            if (offset > 1.0) {
                mIncrement++
                resetIncrementNextIteration = true
            }
            offset += (mIncrement - floor(mIncrement.toDouble())).toFloat()

            wroteData = true
        }

        try {
            mResponse.put(if (wroteData) mIndex else 0)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}