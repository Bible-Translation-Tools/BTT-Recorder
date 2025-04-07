package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.AudioVisualization.WavVisualizer
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MarkerMediator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController
import org.wycliffeassociates.translationrecorder.Playback.interfaces.ViewCreatedCallback
import org.wycliffeassociates.translationrecorder.Playback.markers.MarkerHolder
import org.wycliffeassociates.translationrecorder.Playback.overlays.DraggableViewFrame.PositionChangeMediator
import org.wycliffeassociates.translationrecorder.Playback.overlays.MarkerLineLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.MarkerLineLayer.MarkerLineDrawDelegator
import org.wycliffeassociates.translationrecorder.Playback.overlays.RectangularHighlightLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.RectangularHighlightLayer.HighlightDelegator
import org.wycliffeassociates.translationrecorder.Playback.overlays.ScrollGestureLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.WaveformLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.WaveformLayer.WaveformDrawDelegator
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentWaveformBinding
import org.wycliffeassociates.translationrecorder.widgets.marker.DraggableImageView
import org.wycliffeassociates.translationrecorder.widgets.marker.SectionMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.SectionMarkerView
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarkerView
import kotlin.math.max
import kotlin.math.min

/**
 * Created by sarabiaj on 11/4/2016.
 */
class WaveformFragment : Fragment(), PositionChangeMediator, MarkerLineDrawDelegator,
    WaveformDrawDelegator, ScrollGestureLayer.OnScrollListener, HighlightDelegator {

    //------------Views-----------------//
    private var mMarkerLineLayer: MarkerLineLayer? = null
    private var mWaveformLayer: WaveformLayer? = null
    private var mHighlightLayer: RectangularHighlightLayer? = null
    private var mScrollGestureLayer: ScrollGestureLayer? = null

    private lateinit var mHandler: Handler

    private var mOnScrollDelegator: OnScrollDelegator? = null
    private var mViewCreatedCallback: ViewCreatedCallback? = null
    private var mMediaController: MediaController? = null
    private var mMarkerMediator: MarkerMediator? = null
    
    private lateinit var mPaintPlayback: Paint
    private lateinit var mPaintBaseLine: Paint

    private var mCurrentRelativeFrame = 0
    private var mWavVis: WavVisualizer? = null
    private var mCurrentMs = 0

    private var mCurrentAbsoluteFrame = 0

    private var _binding: FragmentWaveformBinding? = null
    private val binding get() = _binding!!

    interface OnScrollDelegator {
        fun delegateOnScroll(distY: Float)
        fun delegateOnScrollComplete()
        fun onCueScroll(id: Int, distY: Float)
    }

    override fun onScroll(x1: Float, x2: Float, distX: Float) {
        mOnScrollDelegator?.delegateOnScroll(distX)
    }

    override fun onScrollComplete() {
        mOnScrollDelegator?.delegateOnScrollComplete()
    }

    private fun setMarkerMediator(mediator: MarkerMediator) {
        mMarkerMediator = mediator
    }

    fun setWavRenderer(vis: WavVisualizer?) {
        mWavVis = vis
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentWaveformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mHandler = Handler(Looper.getMainLooper())

        mWaveformLayer = WaveformLayer.newInstance(activity, this)
        mMarkerLineLayer = MarkerLineLayer.newInstance(activity, this)
        mHighlightLayer = RectangularHighlightLayer.newInstance(activity, this)
        mScrollGestureLayer = ScrollGestureLayer.newInstance(activity, this)
        mViewCreatedCallback?.onViewCreated(this)

        binding.waveformFrame.addView(mWaveformLayer)
        binding.waveformFrame.addView(mScrollGestureLayer)
        binding.waveformFrame.addView(mMarkerLineLayer)
        binding.waveformFrame.addView(mHighlightLayer)

        val dpSize = 2

        mPaintPlayback = Paint().apply {
            color = resources.getColor(R.color.primary)
            style = Paint.Style.STROKE
            strokeWidth = dpSize.toFloat()
        }
        mPaintBaseLine = Paint().apply {
            color = resources.getColor(R.color.secondary)
            style = Paint.Style.STROKE
            strokeWidth = dpSize.toFloat()
        }

        binding.draggableViewFrame.bringToFront()
        binding.draggableViewFrame.setPositionChangeMediator(this)

        mMarkerMediator?.setDraggableViewFrame(binding.draggableViewFrame)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mOnScrollDelegator = context as OnScrollDelegator
        mViewCreatedCallback = context as ViewCreatedCallback
        mMediaController = context as MediaController
    }

    override fun onDestroy() {
        super.onDestroy()
        mOnScrollDelegator = null
        mViewCreatedCallback = null
        mMediaController = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //-------------MARKERS----------------------//
    fun addStartMarker(frame: Int) {
        val dpSize = 4
        val dm = resources.displayMetrics
        val strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpSize.toFloat(),
            dm
        )

        val color = resources.getColor(R.color.dark_moderate_cyan)
        val div = SectionMarkerView(
            activity,
            R.drawable.ic_startmarker_cyan,
            MarkerHolder.START_MARKER_ID,
            SectionMarkerView.Orientation.LEFT_MARKER,
            color,
            strokeWidth
        )
        div.x =
            (DraggableImageView.mapLocationToScreenSpace(
                frame,
                binding.waveformFrame.width
            ) - div.width).toFloat()
        mMarkerMediator?.onAddStartSectionMarker(SectionMarker(div, frame))
        invalidateFrame(mCurrentAbsoluteFrame, mCurrentRelativeFrame, mCurrentMs)
    }

    fun addEndMarker(frame: Int) {
        val dpSize = 4
        val dm = resources.displayMetrics
        val strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize.toFloat(), dm)

        val color = resources.getColor(R.color.dark_moderate_cyan)
        val div = SectionMarkerView(
            activity,
            R.drawable.ic_endmarker_cyan,
            Gravity.BOTTOM,
            MarkerHolder.END_MARKER_ID,
            SectionMarkerView.Orientation.RIGHT_MARKER,
            color,
            strokeWidth
        )
        div.x = DraggableImageView.mapLocationToScreenSpace(frame, binding.waveformFrame.width).toFloat()
        mMarkerMediator?.onAddEndSectionMarker(SectionMarker(div, frame))
        invalidateFrame(mCurrentAbsoluteFrame, mCurrentRelativeFrame, mCurrentMs)
    }

    fun addVerseMarker(verseNumber: Int, frame: Int) {
        val dpSize = 4
        val dm = resources.displayMetrics
        val strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpSize.toFloat(), dm)

        val color = resources.getColor(R.color.yellow)
        val div = VerseMarkerView(
            activity,
            R.drawable.verse_marker_yellow,
            verseNumber,
            color,
            strokeWidth
        )
        mMarkerMediator?.onAddVerseMarker(verseNumber, VerseMarker(div, frame))
    }

    override fun onPositionRequested(id: Int, x: Float): Float {
        var posX = x
        mMarkerMediator?.let { mediator ->
            if (id < 0) {
                if (id == MarkerHolder.END_MARKER_ID) {
                    posX = max(
                        mediator.getMarker(MarkerHolder.START_MARKER_ID).markerX.toDouble(),
                        posX.toDouble()
                    ).toFloat()
                } else {
                    posX += (mediator.getMarker(MarkerHolder.START_MARKER_ID).width)
                    if (mediator.contains(MarkerHolder.END_MARKER_ID)) {
                        posX = min(
                            mediator.getMarker(MarkerHolder.END_MARKER_ID).markerX.toDouble(),
                            posX.toDouble()
                        ).toFloat()
                    }
                }
            }
        }
        return posX
    }

    override fun onPositionChanged(id: Int, x: Float) {
        mOnScrollDelegator?.onCueScroll(id, x)
    }

    override fun onDrawMarkers(canvas: Canvas) {
        val markers = mMarkerMediator?.markers ?: listOf()
        for (d in markers) {
            if (d is VerseMarker && mMediaController?.isInEditMode == true) {
                continue
            }
            d.drawMarkerLine(canvas)
        }
        mWaveformLayer?.let { waveformLayer ->
            canvas.drawLine(
                waveformLayer.width.toFloat() / 8,
                0f,
                waveformLayer.width.toFloat() / 8,
                waveformLayer.height.toFloat(),
                mPaintPlayback
            )
            canvas.drawLine(
                0f,
                waveformLayer.height.toFloat() / 2,
                waveformLayer.width.toFloat(),
                waveformLayer.height.toFloat() / 2,
                mPaintBaseLine
            )
        }
    }

    override fun onDrawHighlight(canvas: Canvas, paint: Paint) {
        mMarkerMediator?.let { mediator ->
            if (mediator.contains(MarkerHolder.END_MARKER_ID) && mediator.contains(MarkerHolder.START_MARKER_ID)) {
                canvas.drawRect(
                    mediator.getMarker(MarkerHolder.START_MARKER_ID).markerX,
                    0f,
                    mediator.getMarker(MarkerHolder.END_MARKER_ID).markerX,
                    binding.waveformFrame.height.toFloat(),
                    paint
                )
            }
        }
    }

    override fun onDrawWaveform(canvas: Canvas, paint: Paint) {
        mWavVis?.let {
            canvas.drawLines(it.getDataToDraw(mCurrentRelativeFrame), paint)
        }
    }

    fun invalidateFrame(absoluteFrame: Int, relativeFrame: Int, ms: Int) {
        mCurrentRelativeFrame = relativeFrame
        mCurrentAbsoluteFrame = absoluteFrame
        mCurrentMs = ms

        mHandler.post {
            mMarkerMediator?.updateCurrentFrame(mCurrentRelativeFrame)
            mWaveformLayer?.invalidate()
            mMarkerLineLayer?.invalidate()
            mHighlightLayer?.invalidate()

            if (_binding != null) {
                binding.draggableViewFrame.invalidate()
            }
        }
    }

    companion object {
        fun newInstance(mediator: MarkerMediator): WaveformFragment {
            val f = WaveformFragment()
            f.setMarkerMediator(mediator)
            return f
        }
    }
}
