package org.wycliffeassociates.translationrecorder.AudioVisualization;

import org.wycliffeassociates.translationrecorder.AudioInfo;
import org.wycliffeassociates.translationrecorder.AudioVisualization.Utils.U;
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class WavVisualizer {

    private float mUserScale = 1f;
    private final int mDefaultFramesOnScreen = 441000;
    public static int mNumFramesOnScreen;
    private boolean mUseCompressedFile = false;
    private boolean mCanSwitch = false;
    private final float[] mSamples;
    private final float[] mMinimap;
    int mScreenHeight;
    int mScreenWidth;
    AudioFileAccessor mAccessor;

    ThreadPoolExecutor mThreads;
    List<LinkedBlockingQueue<Integer>> mThreadResponse;
    VisualizerRunnable[] mRunnable;

    int mNumThreads;

    public WavVisualizer(ShortBuffer buffer, ShortBuffer compressed, int numThreads, int screenWidth, int screenHeight, int minimapWidth, CutOp cut) {
        mScreenHeight = screenHeight;
        mScreenWidth = screenWidth;
        mNumFramesOnScreen = mDefaultFramesOnScreen;
        mCanSwitch = compressed != null;
        mSamples = new float[screenWidth*4];
        mAccessor = new AudioFileAccessor(compressed, buffer, cut);
        mMinimap = new float[minimapWidth * 4];
        mNumThreads = numThreads;
        mThreads = new ThreadPoolExecutor(mNumThreads, mNumThreads, 20, TimeUnit.SECONDS, new ArrayBlockingQueue<>(mNumThreads));
        mThreads.allowCoreThreadTimeOut(true);
        mThreadResponse = new ArrayList<>(mNumThreads);
        mRunnable = new VisualizerRunnable[mNumThreads];
        for(int i = 0; i < mNumThreads; i++){
            mThreadResponse.add(new LinkedBlockingQueue<>(1));
            mRunnable[i] = new VisualizerRunnable();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mThreads.shutdown();
        mThreads.purge();
    }

    public void enableCompressedFileNextDraw(ShortBuffer compressed){
        mAccessor.setCompressed(compressed);
        mCanSwitch = true;
    }

    public float[] getMinimap(int minimapHeight, int minimapWidth, int durationFrames){
        //selects the proper buffer to use
        boolean useCompressed = mCanSwitch && mNumFramesOnScreen > AudioInfo.COMPRESSED_FRAMES_ON_SCREEN;
        mAccessor.switchBuffers(useCompressed);

        int pos = 0;
        int index = 0;

        double incrementTemp = AudioFileAccessor.getIncrement(useCompressed, durationFrames, minimapWidth);
        double leftover = incrementTemp - (int)Math.floor(incrementTemp);
        double count = 0;
        int increment = (int)Math.floor(incrementTemp);
        boolean leapedInc = false;
        for(int i = 0; i < minimapWidth; i++){
            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;
            if(count > 1){
                count-=1;
                increment++;
                leapedInc = true;
            }
            for(int j = 0; j < increment; j++){
                if(pos >= mAccessor.size()){
                    break;
                }
                short value = mAccessor.get(pos);
                max = (max < (double) value) ? value : max;
                min = (min > (double) value) ? value : min;
                pos++;
            }
            if(leapedInc){
                increment--;
                leapedInc = false;
            }
            count += leftover;
            mMinimap[index] = (float) index / 4;
            mMinimap[index+1] = U.getValueForScreen(max, minimapHeight);
            mMinimap[index+2] = (float) index / 4;
            mMinimap[index+3] = U.getValueForScreen(min, minimapHeight);
            index += 4;
        }

        return mMinimap;
    }

    public float[] getDataToDraw(int frame){
        mNumFramesOnScreen = computeNumFramesOnScreen(mUserScale);
        //based on the user scale, determine which buffer waveData should be
        mUseCompressedFile = shouldUseCompressedFile(mNumFramesOnScreen);
        mAccessor.switchBuffers(mUseCompressedFile);

        //get the number of array indices to skip over- the array will likely contain more data than one pixel can show
        float increment = getIncrement(mNumFramesOnScreen);
        int framesToSubtract = framesBeforePlaybackLine(mNumFramesOnScreen);
        int[] locAndTime = mAccessor.indexAfterSubtractingFrame(framesToSubtract, frame);
        int startPosition = locAndTime[0];
        int newTime = locAndTime[1];
        int index = initializeSamples(mSamples, startPosition, newTime);
        //in the event that the actual start position ends up being negative (such as from shifting forward due to playback being at the start of the file)
        //it should be set to zero (and the buffer will already be initialized with some zeros, with index being the index of where to resume placing data
        startPosition = Math.max(0, startPosition);
        int end = mSamples.length/4;

        int iterations = end - index/4;
        int rangePerThread = iterations / mNumThreads;

        for(int i = 0; i < mThreadResponse.size(); i++){
            mThreads.submit(mRunnable[i].newState(
                    index/4 + (rangePerThread * i),
                    index/4 + (rangePerThread * (i+1)),
                    mThreadResponse.get(i),
                    mAccessor,
                    mSamples,
                    index + ((rangePerThread * mNumThreads) * i),
                    mScreenHeight,
                    startPosition + (int)((increment) * (rangePerThread * i)),
                    increment,
                    i
            ));
        }

        for (LinkedBlockingQueue<Integer> integers : mThreadResponse) {
            try {
                int returnIdx = integers.take();
                index = Math.max(returnIdx, index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //zero out the rest of the array
        for (int i = index; i < mSamples.length; i++){
            mSamples[i] = 0;
        }

        return mSamples;
    }

    public static int addHighAndLowToDrawingArray(AudioFileAccessor accessor, float[] samples, int beginIdx, int endIdx, int index, int screenHeight){

        boolean addedVal = false;
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        //loop over the indicated chunk of data to extract out the high and low in that section, then store it in samples
        for(int i = beginIdx; i < Math.min(accessor.size(), endIdx); i++){
            short value = accessor.get(i);
            max = (max < (double) value) ? value : max;
            min = (min > (double) value) ? value : min;
        }
        if(samples.length > index+4){
            samples[index] = (float) index / 4;
            samples[index+1] = U.getValueForScreen(max, screenHeight);
            samples[index+2] = (float) index / 4;
            samples[index+3] = U.getValueForScreen(min, screenHeight);
            index += 4;
            addedVal = true;
        }

        //returns the end of relevant data in the buffer
        return (addedVal)? index : 0;
    }

    private int initializeSamples(float[] samples, int startPosition, int framesUntilZero){
        if(startPosition <= 0) {
            int numberOfZeros = 0;
            if(framesUntilZero < 0){
                framesUntilZero *= -1;
                double fpp = (mNumFramesOnScreen) / (double)mScreenWidth;
                numberOfZeros = (int)Math.round(framesUntilZero/fpp);
            }
            int index = 0;
            for (int i = 0; i < numberOfZeros; i++) {
                samples[index] = (float) index / 4;
                samples[index+1] = 0;
                samples[index+2] = (float) index / 4;
                samples[index+3] = 0;
                index += 4;
            }
            return index;
        }
        return 0;
    }

    public boolean shouldUseCompressedFile(int numFramesOnScreen){
        return numFramesOnScreen >= AudioInfo.COMPRESSED_FRAMES_ON_SCREEN && mCanSwitch;
    }

    private int framesBeforePlaybackLine(int numFramesOnScreen){
        return numFramesOnScreen / 8;
    }

    private int computeSampleStartPosition(int startFrame){
        if(mUseCompressedFile){
            startFrame /= 25;
        }
        return startFrame;
    }

    private float getIncrement(int numFramesOnScreen){
        float increment = (int)( numFramesOnScreen / (float)mScreenWidth);
        if(mUseCompressedFile) {
            increment /= 25.0F;
        }
        return increment;
    }

    private int computeNumFramesOnScreen(float userScale) {
        int numSecondsOnScreen = Math.round(mNumFramesOnScreen * userScale);
        return Math.max(numSecondsOnScreen, AudioInfo.COMPRESSED_SECONDS_ON_SCREEN);
    }
}