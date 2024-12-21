package org.wycliffeassociates.translationrecorder.Playback

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.AudioVisualization.WavVisualizer
import org.wycliffeassociates.translationrecorder.FilesPage.ExitDialog
import org.wycliffeassociates.translationrecorder.FilesPage.ExitDialog.DeleteFileCallback
import org.wycliffeassociates.translationrecorder.Playback.SourceAudio.OnAudioListener
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentFileBar
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentFileBar.InsertCallback
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentFileBar.RatingCallback
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentFileBar.RerecordCallback
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentPlaybackTools
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentTabbedWidget
import org.wycliffeassociates.translationrecorder.Playback.fragments.FragmentTabbedWidget.DelegateMinimapMarkerDraw
import org.wycliffeassociates.translationrecorder.Playback.fragments.MarkerCounterFragment
import org.wycliffeassociates.translationrecorder.Playback.fragments.MarkerToolbarFragment
import org.wycliffeassociates.translationrecorder.Playback.fragments.MarkerToolbarFragment.OnMarkerPlacedListener
import org.wycliffeassociates.translationrecorder.Playback.fragments.WaveformFragment
import org.wycliffeassociates.translationrecorder.Playback.fragments.WaveformFragment.OnScrollDelegator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.AudioEditDelegator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.AudioStateCallback
import org.wycliffeassociates.translationrecorder.Playback.interfaces.EditStateInformer
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MarkerMediator
import org.wycliffeassociates.translationrecorder.Playback.interfaces.MediaController
import org.wycliffeassociates.translationrecorder.Playback.interfaces.VerseMarkerModeToggler
import org.wycliffeassociates.translationrecorder.Playback.interfaces.ViewCreatedCallback
import org.wycliffeassociates.translationrecorder.Playback.markers.MarkerHolder
import org.wycliffeassociates.translationrecorder.Playback.overlays.MinimapLayer.MinimapDrawDelegator
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RatingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RatingDialog.Companion.newInstance
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity.Companion.getInsertIntent
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity.Companion.getRerecordIntent
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp
import org.wycliffeassociates.translationrecorder.WavFileLoader
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityPlaybackScreenBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.chapterIntToString
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getLargestTake
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getNameWithoutTake
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getProjectDirectory
import org.wycliffeassociates.translationrecorder.project.components.User
import org.wycliffeassociates.translationrecorder.wav.WavFile
import org.wycliffeassociates.translationrecorder.widgets.FourStepImageView
import org.wycliffeassociates.translationrecorder.widgets.marker.DraggableImageView.OnMarkerMovementRequest
import org.wycliffeassociates.translationrecorder.widgets.marker.DraggableMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarker
import org.wycliffeassociates.translationrecorder.widgets.marker.VerseMarkerView
import java.io.File
import java.io.IOException
import java.nio.ShortBuffer
import javax.inject.Inject
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Created by sarabiaj on 10/27/2016.
 */
@AndroidEntryPoint
class PlaybackActivity : AppCompatActivity(), RatingDialog.DialogListener,
    MediaController,
    AudioStateCallback, AudioEditDelegator, EditStateInformer, ViewCreatedCallback,
    OnScrollDelegator, VerseMarkerModeToggler, OnMarkerPlacedListener, MinimapDrawDelegator,
    DelegateMinimapMarkerDraw, RerecordCallback, RatingCallback, InsertCallback,
    OnMarkerMovementRequest, DeleteFileCallback, OnAudioListener {
    enum class MODE {
        EDIT,
        VERSE_MARKER
    }

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    @Volatile
    private var isSaved = true
    private val isPlaying = false
    private var wasPlaying = false
    private var mode: MODE? = null

    private var wavVis: WavVisualizer? = null
    private lateinit var mWavFile: WavFile
    private var wavFileLoader: WavFileLoader? = null
    private lateinit var mProject: Project
    private var mChapter = 0
    private var mUnit = 0
    private var mRating = 0
    private var startVerse = 0
    private var endVerse = 0
    private var mTotalVerses = 0
    private lateinit var mAudioController: AudioVisualController

    private var mFragmentContainerMapping: HashMap<Int, Fragment> = hashMapOf()
    private lateinit var mFragmentPlaybackTools: FragmentPlaybackTools
    private lateinit var mFragmentTabbedWidget: FragmentTabbedWidget
    private lateinit var mFragmentFileBar: FragmentFileBar
    private lateinit var mWaveformFragment: WaveformFragment
    private lateinit var mMarkerCounterFragment: MarkerCounterFragment
    private lateinit var mMarkerToolbarFragment: MarkerToolbarFragment

    private lateinit var mMarkerMediator: MarkerMediator
    private var mWaveformInflated = false
    private var mMinimapInflated = false
    private var mDrawLoop: DrawThread? = null

    private lateinit var mUser: User
    private lateinit var audioTrack: AudioTrack
    private var trackBufferSize = 0

    private lateinit var binding: ActivityPlaybackScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityPlaybackScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioTrack = (application as TranslationRecorderApp).audioTrack
        trackBufferSize = (application as TranslationRecorderApp).trackBufferSize
        try {
            initialize(intent)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        Logger.w(this.toString(), "onCreate")
        val userId = prefs.getDefaultPref(Settings.KEY_USER, 1)
        mUser = db.getUser(userId)
    }

    @Throws(IOException::class)
    private fun initialize(intent: Intent) {
        isSaved = true
        parseIntent(intent)
        verseRange
        try {
            mAudioController = AudioVisualController(
                audioTrack,
                trackBufferSize,
                mWavFile,
                this,
                this
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        initializeFragments()
        wavFileLoader = mAudioController.wavLoader
        mode = MODE.EDIT
    }

    fun startDrawThread() {
        mDrawLoop?.finish()
        mDrawLoop = DrawThread()
        val draw = Thread(mDrawLoop)
        draw.start()
    }

    private fun initializeMarkers() {
        val cues = mWavFile.metadata.cuePoints
        for (cue in cues) {
            mWaveformFragment.addVerseMarker(
                cue.label.toInt(),
                cue.location
            )
        }
        if (cues.isEmpty()) {
            mWaveformFragment.addVerseMarker(0, 0)
        }
    }

    private fun parseIntent(intent: Intent) {
        mWavFile = intent.getParcelableExtra(KEY_WAV_FILE)!!
        mProject = intent.getParcelableExtra(KEY_PROJECT)!!
        mChapter = intent.getIntExtra(KEY_CHAPTER, ChunkPlugin.DEFAULT_CHAPTER)
        mUnit = intent.getIntExtra(KEY_UNIT, ChunkPlugin.DEFAULT_UNIT)
    }

    @Throws(IOException::class)
    private fun initializeFragments() {
        val plugin = mProject.getChunkPlugin(ChunkPluginLoader(
            directoryProvider,
            assetsProvider
        ))
        plugin.initialize(mChapter, mUnit)

        mFragmentPlaybackTools = FragmentPlaybackTools.newInstance()
        mFragmentContainerMapping[R.id.playback_tools_fragment_holder] = mFragmentPlaybackTools

        mMarkerMediator = MarkerHolder(
            mAudioController,
            this,
            mFragmentPlaybackTools,
            mTotalVerses
        )

        mFragmentTabbedWidget = FragmentTabbedWidget.newInstance(
            mMarkerMediator,
            mProject,
            getNameWithoutTake(mWavFile.file.name),
            mChapter
        )
        mFragmentContainerMapping[R.id.tabbed_widget_fragment_holder] = mFragmentTabbedWidget

        val chapterLabel = if (plugin.chapterLabel == "chapter") getString(R.string.chapter_title) else ""

        mFragmentFileBar = FragmentFileBar.newInstance(
            mProject.targetLanguageSlug,
            mProject.versionSlug,
            mProject.bookName,
            chapterLabel,
            plugin.getChapterName(mChapter),
            mProject.getLocalizedModeName(this),
            plugin.chunkName,
            mProject.modeType
        )

        mFragmentContainerMapping[R.id.file_bar_fragment_holder] = mFragmentFileBar

        mWaveformFragment = WaveformFragment.newInstance(mMarkerMediator)
        mFragmentContainerMapping[R.id.waveform_fragment_holder] = mWaveformFragment

        mMarkerCounterFragment = MarkerCounterFragment.newInstance(mMarkerMediator)
        mMarkerToolbarFragment = MarkerToolbarFragment.newInstance()
        attachFragments()
    }

    override fun finish() {
        super.finish()
        mDrawLoop?.finish()
        if (mAudioController.isPlaying) {
            mAudioController.pause()
        }
    }

    private fun attachFragments() {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        val entrySet: Set<Map.Entry<Int, Fragment>> = mFragmentContainerMapping.entries
        for ((key, value) in entrySet) {
            ft.add(key, value)
        }
        ft.commit()
    }


    override fun onMediaPause() {
        mAudioController.pause()
    }

    override fun onMediaPlay() {
        try {
            mAudioController.play()
            mFragmentTabbedWidget.pauseSource()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun onSourcePlay() {
        mAudioController.pause()
        mFragmentPlaybackTools.showPlayButton()
    }

    override fun onSourcePause() {}

    private fun requestUserToRestart() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.audio_player_error)
        builder.setMessage(resources.getString(R.string.restart_device))
        builder.setCancelable(false)
        builder.setPositiveButton(R.string.label_ok) { _, _ -> finish() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onSeekForward() {
        try {
            mAudioController.seekNext()
            onLocationUpdated()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    @Throws(IllegalStateException::class)
    override fun onSeekBackward() {
        try {
            mAudioController.seekPrevious()
            onLocationUpdated()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun onSeekTo(x: Float) {
        try {
            mAudioController.seekTo(
                mAudioController.cutOp.relativeLocToAbsolute(
                    (x * mAudioController.relativeDurationInFrames).toInt(),
                    false
                )
            )
            onLocationUpdated()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun getDurationMs(): Int {
        return mAudioController.relativeDurationMs
    }

    @Throws(IllegalStateException::class)
    override fun getLocationMs(): Int {
        try {
            return mAudioController.relativeLocationMs
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
        return 0
    }

    override fun getLocationInFrames(): Int {
        try {
            return mAudioController.relativeLocationInFrames
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
        return 0
    }

    override fun getDurationInFrames(): Int {
        try {
            return mAudioController.relativeDurationInFrames
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
        return 0
    }

    override fun setOnCompleteListner(onComplete: Runnable) {
        //mAudioController.setOnCompleteListener(onComplete);
    }

    override fun getStartMarkerFrame(): Int {
        return mAudioController.cutOp.absoluteLocToRelative(
            mAudioController.loopStart,
            false
        )
    }

    override fun getEndMarkerFrame(): Int {
        return mAudioController.cutOp.absoluteLocToRelative(
            mAudioController.loopEnd,
            false
        )
    }

    override fun onPlayerPaused() {
        mFragmentPlaybackTools.onPlayerPaused()
        mMarkerToolbarFragment.showPlayButton()
        onLocationUpdated()
    }

    override fun onDeleteRecording() {
        super.onBackPressed()
    }

    override fun onSave() {
        save(null)
    }

    @Synchronized
    override fun onCut() {
        isSaved = false
        val markers = mMarkerMediator.markers
        val markerList: List<DraggableMarker> = ArrayList(markers)
        val relativeLoopStart = mAudioController.cutOp.absoluteLocToRelative(
            mAudioController.loopStart,
            false
        ).toLong()
        val relativeLoopEnd = mAudioController.cutOp.absoluteLocToRelative(
            mAudioController.loopEnd,
            false
        ).toLong()

        for (i in markerList.indices) {
            val marker = markerList[i]
            if (marker.frame in (relativeLoopStart + 1)..relativeLoopEnd) {
                if (marker is VerseMarker) {
                    mMarkerMediator.onRemoveVerseMarker(
                        (marker.getView() as VerseMarkerView).markerId
                    )
                }
            } else {
                if (marker is VerseMarker) {
                    marker.updateFrame(
                        mAudioController.cutOp.relativeLocToAbsolute(
                            marker.getFrame(),
                            false
                        )
                    )
                }
            }
        }
        mAudioController.cut()
        for (marker in markers) {
            if (marker is VerseMarker) {
                marker.updateFrame(
                    mAudioController.cutOp.absoluteLocToRelative(
                        marker.getFrame(),
                        false
                    )
                )
            }
        }
        try {
            mFragmentPlaybackTools.onLocationUpdated(mAudioController.absoluteLocationMs)
            mFragmentPlaybackTools.onDurationUpdated(mAudioController.relativeDurationMs)
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
        onClearMarkers()
        mFragmentTabbedWidget.invalidateMinimap()
        mFragmentTabbedWidget.onLocationChanged()
    }

    override fun onDropStartMarker() {
        try {
            mAudioController.dropStartMarker()
            val location = mAudioController.loopStart
            mWaveformFragment.addStartMarker(
                mAudioController.cutOp.absoluteLocToRelative(
                    location,
                    false
                )
            )
            onLocationUpdated()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun onDropEndMarker() {
        try {
            mAudioController.dropEndMarker()
            val location = mAudioController.loopEnd
            mWaveformFragment.addEndMarker(
                mAudioController.cutOp.absoluteLocToRelative(
                    location,
                    false
                )
            )
            onLocationUpdated()
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun setStartMarkerAt(frameRelative: Int) {
        mAudioController.setStartMarker(frameRelative)
        mWaveformFragment.addStartMarker(frameRelative)
        onLocationUpdated()
    }

    override fun setEndMarkerAt(frame: Int) {
        mAudioController.setEndMarker(frame)
        mWaveformFragment.addEndMarker(frame)
        onLocationUpdated()
    }

    override fun onClearMarkers() {
        mMarkerMediator.onRemoveSectionMarkers()
    }

    override fun hasSetMarkers(): Boolean {
        return mMarkerMediator.hasSectionMarkers()
    }

    override fun isPlaying(): Boolean {
        return mAudioController.isPlaying
    }

    override fun onDropVerseMarker() {
    }

    override fun onUndo() {
        val markers = mMarkerMediator.markers
        //map markers back to absolute before
        for (marker in markers) {
            marker.updateFrame(
                mAudioController.cutOp.relativeLocToAbsolute(
                    marker.frame,
                    false
                )
            )
        }
        mAudioController.undo()
        for (marker in markers) {
            marker.updateFrame(
                mAudioController.cutOp.absoluteLocToRelative(
                    marker.frame,
                    false
                )
            )
        }
        if (!mAudioController.cutOp.hasCut()) {
            isSaved = true
        }
        mFragmentTabbedWidget.invalidateMinimap()
        onLocationUpdated()
    }

    override fun hasEdits(): Boolean {
        return mAudioController.cutOp.hasCut()
    }

    override fun onPositiveClick(dialog: RatingDialog) {
        Logger.w(this.toString(), "rating set")
        mRating = dialog.rating

        db.setTakeRating(dialog.takeInfo, mRating)
        mFragmentFileBar.onRatingChanged(mRating)
    }

    override fun onNegativeClick(dialog: RatingDialog) {
        Logger.w(this.toString(), "rating canceled")
        dialog.dismiss()
    }

    override fun onBackPressed() {
        Logger.w(this.toString(), "Back was pressed.")
        if (mode == MODE.VERSE_MARKER) {
            onDisableVerseMarkerMode()
        } else if (actionsToSave()) {
            Logger.i(this.toString(), "Asking if user wants to save before going back")
            //keep file needs to be false so the callback will go through and
            // the super.onBackPressed is called
            val exit = ExitDialog.Build(
                this,
                R.style.Theme_AppCompat_Light_Dialog,
                false,
                isPlaying,
                mWavFile.file
            )
            exit.show()
        } else {
            super.onBackPressed()
        }
    }


    private fun actionsToSave(): Boolean {
        val cuts = mAudioController.cutOp.hasCut()
        val markersOriginally =
            max(mWavFile.metadata.cuePoints.size.toDouble(), 1.0).toInt()
        val markersNow = mMarkerMediator.numVerseMarkersPlaced()
        val markersPlaced = markersNow > markersOriginally
        return cuts || markersPlaced
    }

    override fun onOpenRating(view: FourStepImageView?) {
        Logger.w(this.toString(), "Rating dialog opened")
        val ppm = mProject.patternMatcher
        ppm.match(mWavFile.file)
        val dialog = newInstance(ppm.takeInfo, mRating)
        dialog.show(supportFragmentManager, "single_unit_rating")
    }

    override fun onRerecord() {
        val intent = getRerecordIntent(
            this,
            mProject,
            mWavFile,
            mChapter,
            mUnit
        )
        save(intent)
    }

    private fun writeMarkers(wav: WavFile) {
        val markers = mMarkerMediator.markers
        val markersList = ArrayList(markers)
        markersList.sortWith { lhs, rhs ->
            lhs.frame.compareTo(rhs.frame)
        }
        for ((i, m) in markersList.withIndex()) {
            if (m is VerseMarker) {
                wav.addMarker((startVerse + i).toString(), m.getFrame())
            }
        }
        wav.commit()
    }

    @SuppressLint("DefaultLocale")
    private fun save(intent: Intent?) {
        //no changes were made, so just exit
        if (isSaved) {
            writeMarkers(mWavFile)
            if (intent == null) {
                this.finish()
                return
            } else {
                startActivity(intent)
                this.finish()
                return
            }
        }

        val dir = File(
            getProjectDirectory(mProject, directoryProvider),
            chapterIntToString(mProject, mChapter)
        )
        val from = mWavFile.file
        val takeInt = getLargestTake(mProject, dir, from) + 1
        val take = String.format("%02d", takeInt)
        val ppm = mProject.patternMatcher
        ppm.match(from)
        val to = File(
            dir,
            (getNameWithoutTake(from)
                    + "_t"
                    + take
                    + AUDIO_RECORDER_FILE_EXT_WAV)
        )
        writeCutToFile(to, mWavFile, intent)
    }

    /**
     * Names the currently recorded .wav file.
     *
     * @return the absolute path of the file created
     */
    private fun writeCutToFile(to: File, from: WavFile, intent: Intent?) {
        val pd = ProgressDialog(this)
        pd.setTitle(R.string.saving)
        pd.setMessage(resources.getString(R.string.writing_changes))
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        pd.setProgressNumberFormat(null)
        pd.setCancelable(false)
        pd.show()
        val saveThread = Thread {
            if (mAudioController.cutOp.hasCut()) {
                try {
                    val dir = getProjectDirectory(mProject, directoryProvider)
                    val toTemp = File(dir, "temp.wav")
                    val toTempWav = WavFile(toTemp, from.metadata)
                    mAudioController.cutOp.writeCut(
                        toTempWav,
                        wavFileLoader!!.mapAndGetAudioBuffer(),
                        pd
                    )
                    toTempWav.clearMarkers()
                    writeMarkers(toTempWav)
                    to.delete()
                    toTemp.renameTo(to)
                    val ppm = mProject.patternMatcher
                    ppm.match(to)
                    db.addTake(
                        ppm.takeInfo,
                        to.name,
                        from.metadata.modeSlug,
                        to.lastModified(),
                        0,
                        mUser.id
                    )
                    var oldName = from.file.name
                    oldName = oldName.substring(0, oldName.lastIndexOf("."))
                    val visDir = File(externalCacheDir, "Visualization")
                    val toVis = File(visDir, "$oldName.vis")
                    toVis.delete()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            isSaved = true
            try {
                pd.dismiss()
            } catch (e: IllegalArgumentException) {
                Logger.e(
                    "PlaybackActivity",
                    "Tried to dismiss cut dialog",
                    e
                )
            }
            if (intent == null) {
                finish()
            } else {
                val result = WavFile(to)
                intent.putExtra(RecordingActivity.KEY_WAV_FILE, result)
                startActivity(intent)
                finish()
            }
        }
        saveThread.start()
    }

    override fun onInsert() {
        val insertIntent = getInsertIntent(
            this,
            mProject,
            mWavFile,
            mChapter,
            mUnit,
            mAudioController.relativeLocationInFrames
        )
        save(insertIntent)
    }

    private fun allVersesMarked(): Boolean {
        return mMarkerMediator.hasVersesRemaining()
    }

    private val verseRange: Unit
        get() {
            val ppm = mProject.patternMatcher
            ppm.match(mWavFile.file)
            val takeInfo = ppm.takeInfo
            mTotalVerses = (takeInfo.endVerse - takeInfo.startVerse + 1)
            startVerse = takeInfo.startVerse
            endVerse = takeInfo.endVerse
        }

    private fun setVerseMarkerCount(count: Int) {
        // - 1 because the first verse marker should be automatically dropped at the beginning
        //mVerseMarkerCount.setText(String.valueOf(count));
    }

    private fun dropVerseMarker() {
        //mMainCanvas.dropVerseMarker(mManager.getLocationMs());
        //mManager.updateUI();
    }

    private fun saveVerseMarkerPosition() {
        // NOTE: Put real code here
        println("Save verse marker position here")
    }

    override fun onViewCreated(ref: Fragment) {
        if (ref is WaveformFragment) {
            val view = mWaveformFragment.view
            val vto = view!!.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mWaveformInflated = true
                    if (mMinimapInflated) {
                        initializeRenderer()
                        initializeMarkers()
                        mWaveformFragment.requireView()
                            .getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this)
                        startDrawThread()
                    }
                }
            })
        } else if (ref is FragmentTabbedWidget) {
            val view = mFragmentTabbedWidget.view
            val vto = view!!.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mMinimapInflated = true
                    if (mWaveformInflated) {
                        initializeRenderer()
                        initializeMarkers()
                        mFragmentTabbedWidget.requireView()
                            .getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this)
                        startDrawThread()
                    }
                }
            })
        }
    }

    private fun initializeRenderer() {
        try {
            val numThreads = 1
            val uncompressed = wavFileLoader!!.mapAndGetAudioBuffer()
            val compressed = wavFileLoader!!.mapAndGetVisualizationBuffer()
            wavVis = WavVisualizer(
                uncompressed,
                compressed,
                numThreads,
                mWaveformFragment.requireView().width,
                mWaveformFragment.requireView().height,
                mFragmentTabbedWidget.widgetWidth,
                mAudioController.cutOp
            )
            mWaveformFragment.setWavRenderer(wavVis)
            mFragmentTabbedWidget.initializeTimecode(mAudioController.relativeDurationMs)
        } catch (e: IOException) {
        }
    }

    override fun delegateOnScroll(distX: Float) {
        if (mAudioController.isPlaying) {
            wasPlaying = true
            mAudioController.pause()
        }
        mAudioController.scrollAudio(distX)
    }

    override fun delegateOnScrollComplete() {
        if (wasPlaying) {
            wasPlaying = false
            mAudioController.play()
        }
    }

    override fun onLocationUpdated() {
        try {
            val absoluteFrame = mAudioController.absoluteLocationInFrames
            val relativeFrame = mAudioController.relativeLocationInFrames
            val absoluteMs = mAudioController.absoluteLocationMs

            mWaveformFragment.invalidateFrame(absoluteFrame, relativeFrame, absoluteMs)

            //                //// TODO
//                mFragmentTabbedWidget.invalidateFrame(frame);
//                mFragmentPlaybackTools.invalidateMs(ms);
//                mMarkerToolbarFragment.invalidateMs(ms);
            mFragmentPlaybackTools.onLocationUpdated(mAudioController.relativeLocationMs)
            mFragmentTabbedWidget.onLocationChanged()
            mMarkerToolbarFragment.onLocationUpdated(mAudioController.relativeLocationMs)
        } catch (e: IllegalStateException) {
            requestUserToRestart()
        }
    }

    override fun onVisualizationLoaded(mappedVisualizationFile: ShortBuffer) {
        val handler = Handler(Looper.getMainLooper())
        if (wavVis == null) {
            //delay the call if the visualizer hasn't loaded yet
            handler.postDelayed({ onVisualizationLoaded(mappedVisualizationFile) }, 1000)
        } else {
            handler.post {
                wavVis!!.enableCompressedFileNextDraw(mappedVisualizationFile)
                mFragmentTabbedWidget.invalidateMinimap()
            }
        }
    }

    override fun onEnableVerseMarkerMode() {
        Logger.w(this.toString(), "onEnableVerseMarkerMode")
        mAudioController.pause()
        mFragmentTabbedWidget.pauseSource()
        onClearMarkers()
        mode = MODE.VERSE_MARKER
        supportFragmentManager.beginTransaction()
            .remove(mFragmentFileBar)
            .add(R.id.file_bar_fragment_holder, mMarkerCounterFragment)
            .remove(mFragmentPlaybackTools)
            .add(R.id.playback_tools_fragment_holder, mMarkerToolbarFragment)
            .commit()
        onLocationUpdated()
    }

    override fun onDisableVerseMarkerMode() {
        Logger.w(this.toString(), "onDisableVerseMarkerMode")
        mAudioController.pause()
        mFragmentTabbedWidget.pauseSource()
        mode = MODE.EDIT
        val fm = supportFragmentManager
        fm.beginTransaction()
            .remove(mMarkerCounterFragment)
            .add(R.id.file_bar_fragment_holder, mFragmentFileBar)
            .remove(mMarkerToolbarFragment)
            .add(R.id.playback_tools_fragment_holder, mFragmentPlaybackTools)
            .commit()
        onLocationUpdated()
    }

    override fun onMarkerPlaced() {
        if (mMarkerMediator.hasVersesRemaining()) {
            Logger.w(this.toString(), "Placed verse marker")
            val frame = mAudioController.relativeLocationInFrames
            val markerNumber: Int

            try {
                markerNumber = mMarkerMediator.availableMarkerNumber(startVerse, endVerse)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                return
            }

            mAudioController.dropVerseMarker(markerNumber.toString(), frame)
            mWaveformFragment.addVerseMarker(markerNumber, frame)
            mMarkerCounterFragment.decrementVersesRemaining()
            try {
                mWaveformFragment.invalidateFrame(
                    mAudioController.absoluteLocationInFrames,
                    mAudioController.relativeLocationInFrames,
                    mAudioController.absoluteLocationMs
                )
            } catch (e: IllegalStateException) {
                requestUserToRestart()
            }
        }
    }

    override fun onCueScroll(id: Int, distX: Float) {
        mMarkerMediator.onCueScroll(id, distX)
    }

    override fun onDelegateMinimapDraw(canvas: Canvas, paint: Paint): Boolean {
        if (wavVis != null) {
            canvas.drawLines(
                wavVis!!.getMinimap(
                    canvas.height,
                    canvas.width,
                    mAudioController.relativeDurationInFrames
                ),
                paint
            )
            return true
        } else {
            return false
        }
    }

    override fun onDelegateMinimapMarkerDraw(
        canvas: Canvas,
        location: Paint,
        section: Paint,
        verse: Paint
    ) {
        val x = (locationInFrames / durationInFrames.toFloat()) * canvas.width
        canvas.drawLine(x, 0f, x, canvas.height.toFloat(), location)
        val start = ((startMarkerFrame) / durationInFrames.toFloat()) * canvas.width
        val end = ((endMarkerFrame) / durationInFrames.toFloat()) * canvas.width
        val markers = mMarkerMediator.markers
        for (m in markers) {
            val markerPos = ((m.frame) / durationInFrames.toFloat()) * canvas.width
            canvas.drawLine(markerPos, 0f, markerPos, canvas.height.toFloat(), verse)
        }
        canvas.drawLine(start, 0f, start, canvas.height.toFloat(), section)
        canvas.drawLine(end, 0f, end, canvas.height.toFloat(), section)
    }

    override fun onMarkerMovementRequest(markerId: Int): Boolean {
        val isEditMode = mode == MODE.EDIT
        val isVerseMarkerMode = mode == MODE.VERSE_MARKER
        val isStartMarker = markerId == MarkerHolder.START_MARKER_ID
        val isEndMarker = markerId == MarkerHolder.END_MARKER_ID

        return if (isEditMode && (isEndMarker || isStartMarker)) {
            true
        } else if (isVerseMarkerMode && !isStartMarker && !isEndMarker) {
            true
        } else {
            false
        }
    }

    override fun isInVerseMarkerMode(): Boolean {
        return mode == MODE.VERSE_MARKER
    }

    override fun isInEditMode(): Boolean {
        return mode == MODE.EDIT
    }

    private inner class DrawThread : Runnable {
        @Volatile
        private var finished = false

        fun finish() {
            finished = true
        }

        override fun run() {
            while (!finished) {
                if (mAudioController.isPlaying) {
                    onLocationUpdated()
                }
                try {
                    Thread.sleep(45)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val AUDIO_RECORDER_FILE_EXT_WAV: String = ".wav"
        const val KEY_PROJECT: String = "key_project"
        const val KEY_WAV_FILE: String = "wavfile"
        const val KEY_CHAPTER: String = "key_chapter"
        const val KEY_UNIT: String = "key_unit"

        fun getPlaybackIntent(
            ctx: Context?,
            file: WavFile?,
            project: Project?,
            chapter: Int,
            unit: Int
        ): Intent {
            val intent = Intent(ctx, PlaybackActivity::class.java)
            intent.putExtra(KEY_PROJECT, project)
            intent.putExtra(KEY_WAV_FILE, file)
            intent.putExtra(KEY_CHAPTER, chapter)
            intent.putExtra(KEY_UNIT, unit)
            return intent
        }
    }
}