package org.wycliffeassociates.translationrecorder.c2cchunk;

import org.wycliffeassociates.translationrecorder.chunkplugin.Chunk;

/**
 * Created by mxaln on 9/10/2025.
 */

public class C2CChunk extends Chunk {
    public C2CChunk(String startVerse, String endVerse) {
        super(startVerse, Integer.parseInt(startVerse), Integer.parseInt(endVerse), ((Integer.parseInt(endVerse) - Integer.parseInt(startVerse)) + 1));
    }

    public String getRangeDisplay(){
        return String.valueOf(getStartVerse());
    }
}
