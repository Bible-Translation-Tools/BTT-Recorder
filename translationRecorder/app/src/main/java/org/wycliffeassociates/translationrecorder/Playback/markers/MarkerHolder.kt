package org.wycliffeassociates.translationrecorder.Playback.markers

import android.widget.FrameLayout
import org.wycliffeassociates.translationrecorder.Playback.AudioVisualController
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentPlaybackTools
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MarkerMediator
import org.wycliffeassociates.translationrecorder.Playback.overlays.DraggableViewFrame
import org.wycliffeassociates.translationrecorder.wav.WavCue
import org.wycliffeassociates.translationrecorder.widgets.marker.DraggableMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.SectionMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarker
import kotlin.math.max
import kotlin.math.min

/**
 * Created by sarabiaj on 11/30/2016.
 */
class MarkerHolder(
    private val mAudioController: AudioVisualController,
    private val mActivity: PlaybackActivity,
    private val mMarkerButtons: FragmentPlaybackTools,
    private val mTotalVerses: Int
) : MarkerMediator {
    private var mDraggableViewFrame: FrameLayout? = null
    private var mMarkers: HashMap<Int, DraggableMarker> = HashMap()

    override fun setDraggableViewFrame(dvf: DraggableViewFrame?) {
        mDraggableViewFrame = dvf
    }

    override fun onAddVerseMarker(verseNumber: Int, marker: VerseMarker) {
        addMarker(verseNumber, marker)
    }

    fun getVerseMarker(verseNumber: Int): VerseMarker? {
        if (verseNumber > 0) {
            return mMarkers[verseNumber] as VerseMarker?
        }
        return null
    }

    override fun onAddStartSectionMarker(marker: SectionMarker) {
        addMarker(START_MARKER_ID, marker)
        mMarkerButtons.viewOnSetStartMarker()
    }

    override fun onAddEndSectionMarker(marker: SectionMarker) {
        addMarker(END_MARKER_ID, marker)
        mMarkerButtons.viewOnSetEndMarker()
    }

    override fun onRemoveVerseMarker(verseNumber: Int) {
        removeMarker(verseNumber)
    }

    override fun onRemoveSectionMarkers() {
        onRemoveStartSectionMarker()
        onRemoveEndSectionMarker()
        mAudioController.clearLoopPoints()
        mActivity.onLocationUpdated()
    }

    override fun onRemoveStartSectionMarker() {
        removeMarker(START_MARKER_ID)
    }

    override fun onRemoveEndSectionMarker() {
        removeMarker(END_MARKER_ID)
    }

    private fun removeMarker(id: Int) {
        if (mMarkers.containsKey(id)) {
            val marker = mMarkers[id]!!.view
            mDraggableViewFrame!!.removeView(marker)
            mMarkers.remove(id)
        }
        mAudioController.setCueList(cueLocationList)
        mActivity.onLocationUpdated()
    }

    @Synchronized
    private fun addMarker(id: Int, marker: DraggableMarker) {
        if (mMarkers[id] != null) {
            if (mDraggableViewFrame != null) {
                mDraggableViewFrame!!.removeView(mMarkers[id]!!.view)
            }
        }
        mMarkers[id] = marker
        if (mDraggableViewFrame != null) {
            mDraggableViewFrame!!.addView(marker.view)
        }
        mAudioController.setCueList(cueLocationList)
        mActivity.onLocationUpdated()
    }

    override val markers: Collection<DraggableMarker>
        get() = mMarkers.values

    override fun updateCurrentFrame(frame: Int) {
        for (m in mMarkers.values) {
            m.updateX(frame, mDraggableViewFrame!!.width)
        }
    }

    override fun getMarker(id: Int): DraggableMarker {
        return mMarkers[id]!!
    }

    override fun contains(id: Int): Boolean {
        return mMarkers.containsKey(id)
    }

    override fun onCueScroll(id: Int, newXPos: Float) {
        val selectedMarker = mMarkers[id]
        val fpp = framesPerPixel
        var position = selectedMarker!!.frame + Math.round((newXPos - selectedMarker.markerX) * fpp)
        position = min(
            max(position.toDouble(), 0.0),
            mAudioController.absoluteDurationInFrames.toDouble()
        ).toInt()
        if (id == START_MARKER_ID) {
            mAudioController.setStartMarker(position)
        } else if (id == END_MARKER_ID) {
            mAudioController.setEndMarker(position)
        }
        selectedMarker.updateFrame(position)
        mAudioController.setCueList(cueLocationList)
        mActivity.onLocationUpdated()
    }

    private val framesPerPixel: Float
        get() = (44100 * 10) / mDraggableViewFrame!!.width.toFloat()

    override fun updateStartMarkerFrame(frame: Int) {
        updateMarkerFrame(START_MARKER_ID, frame)
    }

    override fun updateEndMarkerFrame(frame: Int) {
        updateMarkerFrame(END_MARKER_ID, frame)
    }

    private fun updateMarkerFrame(id: Int, frame: Int) {
        if (mMarkers.containsKey(id)) {
            mMarkers[id]!!.updateFrame(frame)
            mMarkers[id]!!.updateX(frame, mDraggableViewFrame!!.width)
        }
        mAudioController.setCueList(cueLocationList)
    }

    override fun hasVersesRemaining(): Boolean {
        return numVersesRemaining() > 0
    }

    override fun numVersesRemaining(): Int {
        return mTotalVerses - numVerseMarkersPlaced()
    }

    override fun numVerseMarkersPlaced(): Int {
        var markers = mMarkers.size
        if (mMarkers.containsKey(START_MARKER_ID)) {
            markers--
        }
        if (mMarkers.containsKey(END_MARKER_ID)) {
            markers--
        }
        return markers
    }

    override fun availableMarkerNumber(startVerse: Int, endVerse: Int): Int {
        for (i in startVerse..endVerse) {
            if (!contains(i)) {
                return i
            }
        }

        throw IllegalStateException(
            String.format(
                "No markers available to insert in range of verses %s - %s",
                startVerse,
                endVerse
            )
        )
    }

    override fun hasSectionMarkers(): Boolean {
        return mMarkers.containsKey(START_MARKER_ID) || mMarkers.containsKey(END_MARKER_ID)
    }

    override val cueLocationList: List<WavCue>
        get() {
            val markers: Collection<DraggableMarker> = mMarkers.values
            val cueList = ArrayList<WavCue>()
            for (marker in markers) {
                if (marker is VerseMarker) {
                    cueList.add(WavCue(marker.getFrame()))
                }
            }
            cueList.sortBy { it.location }
            return cueList
        }

    companion object {
        const val START_MARKER_ID: Int = -1
        const val END_MARKER_ID: Int = -2
    }
}
