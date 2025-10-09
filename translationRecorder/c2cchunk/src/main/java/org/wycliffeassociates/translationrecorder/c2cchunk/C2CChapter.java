package org.wycliffeassociates.translationrecorder.c2cchunk;

import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter;
import org.wycliffeassociates.translationrecorder.chunkplugin.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mxaln on 9/10/2025.
 */

public class C2CChapter extends Chapter {

    List<C2CChunk> mChunks;
    int mNumber;

    public C2CChapter(int number, Map<String, String> chunks) {
        mChunks = constructChunks(chunks);
        mNumber = number;
    }

    public String[] getChunkDisplayValues() {
        String[] display = new String[mChunks.size()];
        if (mChunks.size() > 0) {
            for (int i = 0; i < mChunks.size(); i++) {
                display[i] = mChunks.get(i).getRangeDisplay();
            }
        }
        return display;
    }

    private List<C2CChunk> constructChunks(Map<String,String> map) {
        mChunks = new ArrayList<>();
        for (String startVerse : map.keySet()) {
            mChunks.add(new C2CChunk(startVerse, map.get(startVerse)));
        }
        return mChunks;
    }

    @Override
    public List<Chunk> getChunks() {
        return new ArrayList<>(mChunks);
    }

    @Override
    public String getLabel() {
        return "part";
    }

    @Override
    public String getName() {
        return String.valueOf(mNumber);
    }

    @Override
    public int getNumber() {
        return mNumber;
    }

    public void addChunk(Chunk chunk) {
        mChunks.add((C2CChunk)chunk);
    }
}
