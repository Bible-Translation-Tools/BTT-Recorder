package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.Playback.SourceAudio.OnAudioListener
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MarkerMediator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController
import org.wycliffeassociates.translationrecorder.Playback.interfaces.ViewCreatedCallback
import org.wycliffeassociates.translationrecorder.Playback.overlays.MarkerLineLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.MarkerLineLayer.MarkerLineDrawDelegator
import org.wycliffeassociates.translationrecorder.Playback.overlays.MinimapLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.MinimapLayer.MinimapDrawDelegator
import org.wycliffeassociates.translationrecorder.Playback.overlays.RectangularHighlightLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.RectangularHighlightLayer.HighlightDelegator
import org.wycliffeassociates.translationrecorder.Playback.overlays.ScrollGestureLayer
import org.wycliffeassociates.translationrecorder.Playback.overlays.ScrollGestureLayer.OnTapListener
import org.wycliffeassociates.translationrecorder.Playback.overlays.TimecodeLayer
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentTabbedWidgetBinding
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project
import javax.inject.Inject

/**
 * Created by sarabiaj on 11/4/2016.
 */
@AndroidEntryPoint
class FragmentTabbedWidget : Fragment(), MinimapDrawDelegator, MarkerLineDrawDelegator,
    OnTapListener, ScrollGestureLayer.OnScrollListener, HighlightDelegator {

    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var prefs: IPreferenceRepository

    private lateinit var mProject: Project
    private var mFilename: String = ""
    private var mChapter: Int = 0
    private lateinit var mLocationPaint: Paint
    private lateinit var mSectionPaint: Paint
    private lateinit var mVersePaint: Paint

    private lateinit var mMediaController: MediaController
    private lateinit var mAudioListener: OnAudioListener
    private lateinit var mMinimapDrawDelegator: MinimapDrawDelegator
    private lateinit var mMinimapLineDrawDelegator: DelegateMinimapMarkerDraw

    private var mTimeCodeLayer: TimecodeLayer? = null
    private var mMinimapLayer: MinimapLayer? = null
    private var mMarkerLineLayer: MarkerLineLayer? = null
    private var mGestureLayer: ScrollGestureLayer? = null
    private var mHighlightLayer: RectangularHighlightLayer? = null
    private var mMarkerMediator: MarkerMediator? = null
    private var mViewCreatedCallback: ViewCreatedCallback? = null

    private var _binding: FragmentTabbedWidgetBinding? = null
    private val binding get() = _binding!!

    interface DelegateMinimapMarkerDraw {
        fun onDelegateMinimapMarkerDraw(
            canvas: Canvas,
            location: Paint,
            section: Paint,
            verse: Paint
        )
    }

    private fun setMediator(mediator: MarkerMediator?) {
        mMarkerMediator = mediator
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mViewCreatedCallback = context as ViewCreatedCallback
        mMediaController = context as MediaController
        mMinimapDrawDelegator = context as MinimapDrawDelegator
        mMinimapLineDrawDelegator = context as DelegateMinimapMarkerDraw
        mAudioListener = context as OnAudioListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentTabbedWidgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parseArgs(arguments)
        initializePaints()
        mViewCreatedCallback?.onViewCreated(this)
        if (!view.isInEditMode) {
            binding.srcAudioPlayer.initSrcAudio(mProject, mFilename, mChapter, directoryProvider, prefs)
        }
        attachListeners()
        binding.switchMinimap.isSelected = true
        mTimeCodeLayer = TimecodeLayer.newInstance(activity)
        mMinimapLayer = MinimapLayer.newInstance(activity, this)
        mMarkerLineLayer = MarkerLineLayer.newInstance(activity, this)
        mGestureLayer = ScrollGestureLayer.newInstance(activity, this, this)
        mHighlightLayer = RectangularHighlightLayer.newInstance(activity, this)
        binding.minimap.addView(mMinimapLayer)
        binding.minimap.addView(mTimeCodeLayer)
        binding.minimap.addView(mGestureLayer)
        binding.minimap.addView(mMarkerLineLayer)
        binding.minimap.addView(mHighlightLayer)
    }

    private fun initializePaints() {
        mLocationPaint = Paint()
        mLocationPaint.style = Paint.Style.STROKE
        mLocationPaint.strokeWidth = 2f
        mLocationPaint.color = resources.getColor(R.color.primary)

        mSectionPaint = Paint()
        mSectionPaint.color = resources.getColor(R.color.dark_moderate_cyan)
        mSectionPaint.style = Paint.Style.STROKE
        mSectionPaint.strokeWidth = 2f

        mVersePaint = Paint()
        mVersePaint.color = resources.getColor(R.color.tertiary)
        mVersePaint.style = Paint.Style.STROKE
        mVersePaint.strokeWidth = 2f
    }

    val widgetWidth: Int
        get() = binding.root.width - binding.switchMinimap.width

    private fun parseArgs(args: Bundle?) {
        mProject = args?.getParcelable(KEY_PROJECT)!!
        mFilename = args.getString(KEY_FILENAME, "")
        mChapter = args.getInt(KEY_CHAPTER, 0)
    }

    private fun attachListeners() {
        binding.switchMinimap.setOnClickListener { v ->
            // TODO: Refactor? Maybe use radio button to select one and exclude the other?
            v.isSelected = true
            v.setBackgroundColor(Color.parseColor("#00000000"))
            binding.minimap.visibility = View.VISIBLE
            binding.srcAudioPlayer.visibility = View.INVISIBLE
            binding.switchSourcePlayback.isSelected = false
            binding.switchSourcePlayback.setBackgroundColor(resources.getColor(R.color.mostly_black))
        }

        binding.switchSourcePlayback.setOnClickListener { v ->
            // TODO: Refactor? Maybe use radio button to select one and exclude the other?
            v.isSelected = true
            v.setBackgroundColor(Color.parseColor("#00000000"))
            binding.srcAudioPlayer.visibility = View.VISIBLE
            binding.minimap.visibility = View.INVISIBLE
            binding.switchMinimap.isSelected = false
            binding.switchMinimap.setBackgroundColor(resources.getColor(R.color.mostly_black))
        }

        binding.srcAudioPlayer.setSourceAudioListener(mAudioListener)
    }

    override fun onPause() {
        super.onPause()
        binding.srcAudioPlayer.pauseSource()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.srcAudioPlayer.cleanup()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewCreatedCallback = null
    }

    fun initializeTimeCode(durationMs: Int) {
        mTimeCodeLayer?.setAudioLength(durationMs)
    }

    override fun onDelegateMinimapDraw(canvas: Canvas, paint: Paint): Boolean {
        val success = mMinimapDrawDelegator.onDelegateMinimapDraw(canvas, paint)
        if (!success) {
            invalidateMinimap()
            return false
        }
        return true
    }

    fun invalidateMinimap() {
        mMinimapLayer?.invalidateMinimap()
    }

    fun onLocationChanged() {
        mMinimapLayer?.postInvalidate()
        initializeTimeCode(mMediaController.durationMs)
        mTimeCodeLayer?.postInvalidate()
        mMarkerLineLayer?.postInvalidate()
        mHighlightLayer?.postInvalidate()
    }

    fun pauseSource() {
        binding.srcAudioPlayer.pauseSource()
    }

    override fun onDrawMarkers(canvas: Canvas) {
        mMinimapLineDrawDelegator.onDelegateMinimapMarkerDraw(
            canvas,
            mLocationPaint,
            mSectionPaint,
            mVersePaint
        )
    }

    override fun onDrawHighlight(canvas: Canvas, paint: Paint) {
        if (mMediaController.hasSetMarkers()) {
            val left =
                (mMediaController.startMarkerFrame / mMediaController.durationInFrames.toFloat()) * canvas.width
            val right =
                (mMediaController.endMarkerFrame / mMediaController.durationInFrames.toFloat()) * canvas.width
            canvas.drawRect(left, 0f, right, canvas.height.toFloat(), paint)
        }
    }

    override fun onScroll(rawX1: Float, rawX2: Float, distX: Float) {
        var x1 = rawX1
        var x2 = rawX2
        if (mMediaController.isInEditMode) {
            if (x1 > x2) {
                val temp = x2
                x2 = x1
                x1 = temp
            }
            mMediaController.setStartMarkerAt((x1 / widgetWidth.toFloat() * mMediaController.durationInFrames).toInt())
            mMediaController.setEndMarkerAt((x2 / widgetWidth.toFloat() * mMediaController.durationInFrames).toInt())
            mMarkerMediator?.updateStartMarkerFrame(mMediaController.startMarkerFrame)
            mMarkerMediator?.updateEndMarkerFrame(mMediaController.endMarkerFrame)
            onLocationChanged()
        }
    }

    override fun onScrollComplete() {}

    override fun onTap(x: Float) {
        mMediaController.onSeekTo((x / widgetWidth.toFloat()))
    }

    companion object {
        private const val KEY_PROJECT = "key_project"
        private const val KEY_FILENAME = "key_filename"
        private const val KEY_CHAPTER = "key_chapter"

        fun newInstance(
            mediator: MarkerMediator,
            project: Project,
            filename: String,
            chapter: Int
        ): FragmentTabbedWidget {
            val f = FragmentTabbedWidget()
            val args = Bundle()
            args.putParcelable(KEY_PROJECT, project)
            args.putString(KEY_FILENAME, filename)
            args.putInt(KEY_CHAPTER, chapter)
            f.arguments = args
            f.setMediator(mediator)
            return f
        }
    }
}
