package org.wycliffeassociates.translationrecorder.AudioVisualization;

import com.door43.tools.reporting.Logger;

import org.wycliffeassociates.translationrecorder.AudioInfo;
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp;

import java.nio.ShortBuffer;

/**
 * Created by sarabiaj on 1/12/2016.
 */


/**
 * Keywords:
 * Relative - index or time with cuts abstracted away
 * Absolute - index or time with cut data still existing
 */
public class AudioFileAccessor {
    ShortBuffer mCompressed;
    ShortBuffer mUncompressed;
    CutOp mCut;
    int mWidth;
    boolean mUseCmp = false;

    public AudioFileAccessor(ShortBuffer compressed, ShortBuffer uncompressed, CutOp cut) {
        mCompressed = compressed;
        mUncompressed = uncompressed;
        mCut = cut;
        mWidth = AudioInfo.SCREEN_WIDTH;
        //increment to write the compressed file. ~44 indices uncompressed = 2 compressed
        mUseCmp = compressed != null;
    }

    public void switchBuffers(boolean cmpReady) {
        if (cmpReady) {
            mUseCmp = true;
        } else {
            mUseCmp = false;
        }
    }

    public void setCompressed(ShortBuffer compressed) {
        mCompressed = compressed;
    }

    //FIXME: should not be returning 0 if out of bounds access, there's a bigger issue here
    public short get(int idx) {
        int loc = mCut.relativeLocToAbsolute(idx, mUseCmp);
        short val;
        if (mUseCmp) {
            if (loc < 0) {
                Logger.e(this.toString(), "ERROR, tried to access a negative location from the compressed buffer!");
                return 0;
            } else if (loc >= mCompressed.capacity()) {
                Logger.e(this.toString(), "ERROR, tried to access a negative location from the compressed buffer!");
                return 0;
            }
            val = mCompressed.get(loc);
        } else {
            if (loc < 0) {
                Logger.e(this.toString(), "ERROR, tried to access a negative location from the compressed buffer!");
                return 0;
            } else if (loc >= mUncompressed.capacity()) {
                Logger.e(this.toString(), "ERROR, tried to access a negative location from the compressed buffer!");
                return 0;
            }
            val = mUncompressed.get(loc);
        }
        return val;
    }

    public int size() {
        if (mUseCmp) {
            return mCompressed.capacity() - mCut.getSizeFrameCutCmp();
        }
        return mUncompressed.capacity() - mCut.getSizeFrameCutUncmp();
    }

    public int[] indexAfterSubtractingFrame(int framesToSubtract, int currentFrame) {
        int frame = currentFrame;
        frame -= framesToSubtract;
        int loc = frameToIndex(frame);
        int[] locAndTime = new int[2];
        locAndTime[0] = loc;
        locAndTime[1] = frame;
        return locAndTime;
    }

    public int frameToIndex(int frame) {
        if (mUseCmp) {
            frame /= 25;
        }
        return frame;
    }

    public int relativeIndexToAbsolute(int idx) {
        return mCut.relativeLocToAbsolute(idx, mUseCmp);
    }

    public int absoluteIndexToRelative(int idx) {
        return mCut.absoluteLocToRelative(idx, mUseCmp);
    }

    public static int fileIncrement() {
        return AudioInfo.COMPRESSION_RATE;
    }

    //used for minimap
    public static double uncompressedIncrement(double adjustedDuration, double screenWidth) {
        return (adjustedDuration / screenWidth);
    }

    //used for minimap
    public static double compressedIncrement(double adjustedDuration, double screenWidth) {
        return (uncompressedIncrement(adjustedDuration, screenWidth) / 25.f);
    }

    //FIXME: rounding will compound error in long files, resulting in pixels being off
    //used for minimap- this is why the duration matters
    public static double getIncrement(boolean useCmp, double adjustedDuration, double screenWidth) {
        if (useCmp) {
            return compressedIncrement(adjustedDuration, screenWidth);
        } else {
            return uncompressedIncrement(adjustedDuration, screenWidth);
        }
    }
}
