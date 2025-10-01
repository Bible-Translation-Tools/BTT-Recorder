package org.wycliffeassociates.translationrecorder.Playback.interfaces

import org.wycliffeassociates.translationrecorder.Playback.overlays.DraggableViewFrame
import org.wycliffeassociates.translationrecorder.wav.WavCue
import org.wycliffeassociates.translationrecorder.widgets.marker.DraggableMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.SectionMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarker

/**
 * Created by sarabiaj on 11/30/2016.
 */
interface MarkerMediator {
    fun onAddVerseMarker(verseNumber: Int, marker: VerseMarker)
    fun onAddStartSectionMarker(marker: SectionMarker)
    fun onAddEndSectionMarker(marker: SectionMarker)
    fun onRemoveVerseMarker(verseNumber: Int)
    fun onRemoveStartSectionMarker()
    fun onRemoveEndSectionMarker()
    val markers: Collection<DraggableMarker>

    fun updateCurrentFrame(frame: Int)
    fun getMarker(id: Int): DraggableMarker
    fun contains(id: Int): Boolean
    fun onCueScroll(id: Int, distX: Float)
    fun setDraggableViewFrame(mFrame: DraggableViewFrame?)
    fun onRemoveSectionMarkers()
    fun updateStartMarkerFrame(frame: Int)
    fun updateEndMarkerFrame(frame: Int)
    fun hasVersesRemaining(): Boolean
    fun numVersesRemaining(): Int
    fun numVerseMarkersPlaced(): Int
    fun availableMarkerNumber(startVerse: Int, endVerse: Int): Int
    fun hasSectionMarkers(): Boolean
    val cueLocationList: List<WavCue>
}
