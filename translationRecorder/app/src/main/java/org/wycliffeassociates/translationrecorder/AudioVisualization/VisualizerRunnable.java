package org.wycliffeassociates.translationrecorder.AudioVisualization;

import java.util.concurrent.BlockingQueue;

/**
 * Created by sarabiaj on 12/20/2016.
 */

public class VisualizerRunnable implements Runnable {

    int mStart;
    int mEnd;
    int mUseCompressedFile;
    int startPosition;
    int index;
    float[] mSamples;
    AudioFileAccessor mAccessor;
    BlockingQueue<Integer> mResponse;
    int mIndex;
    int mScreenHeight;
    float increment;
    int tid;

    public VisualizerRunnable(){

    }

    public VisualizerRunnable newState(int start, int end, BlockingQueue<Integer> response, AudioFileAccessor accessor, float[] samples, int index, int screenHeight, int startPosition, float increment, int tid){
        mStart = start;
        mEnd = end;
        mResponse = response;
        mAccessor = accessor;
        mSamples = samples;
        mIndex = start * 4;
        mScreenHeight = screenHeight;
        this.startPosition = startPosition;
        this.increment = increment;
        this.tid = tid;
        return this;
    }

    @Override
    public void run() {
        boolean wroteData = false;
        boolean resetIncrementNextIteration = false;
        float offset = 0;

        for(int i = mStart; i < mEnd; i++){
            if(startPosition > mAccessor.size()){
                break;
            }
            mIndex = Math.max(WavVisualizer.addHighAndLowToDrawingArray(mAccessor, mSamples, startPosition, startPosition+(int)increment, mIndex, mScreenHeight), mIndex);

            startPosition += (int) Math.floor(increment);
            if(resetIncrementNextIteration){
                resetIncrementNextIteration = false;
                increment--;
                offset--;
            }
            if(offset > 1.0) {
                increment++;
                resetIncrementNextIteration = true;
            }
            offset += (float) (increment - Math.floor(increment));

            wroteData = true;
        }

        try {
            mResponse.put((wroteData)? mIndex : 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}