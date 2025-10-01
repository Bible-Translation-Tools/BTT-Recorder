package org.wycliffeassociates.translationrecorder.Playback.interfaces

import java.nio.ShortBuffer

/**
 * Created by Joe on 11/7/2016.
 */
interface AudioStateCallback {
    fun onPlayerPaused()
    fun onLocationUpdated()
    fun onVisualizationLoaded(mappedVisualizationFile: ShortBuffer)
}
