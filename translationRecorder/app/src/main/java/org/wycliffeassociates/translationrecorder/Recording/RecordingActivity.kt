package org.wycliffeassociates.translationrecorder.Recording

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.AudioVisualization.ActiveRecordingRenderer
import org.wycliffeassociates.translationrecorder.FilesPage.ExitDialog
import org.wycliffeassociates.translationrecorder.FilesPage.ExitDialog.DeleteFileCallback
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingControls
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingControls.RecordingControlCallback
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingFileBar
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingFileBar.Companion.newInstance
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingFileBar.OnUnitChangedListener
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingWaveform
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingWaveform.Companion.newInstance
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentSourceAudio
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentVolumeBar
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityRecordingScreenBinding
import org.wycliffeassociates.translationrecorder.permissions.PermissionActivity
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.createFile
import org.wycliffeassociates.translationrecorder.project.ProjectProgress
import org.wycliffeassociates.translationrecorder.project.components.User
import org.wycliffeassociates.translationrecorder.wav.WavFile
import org.wycliffeassociates.translationrecorder.wav.WavMetadata
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 2/20/2017.
 */
@AndroidEntryPoint
class RecordingActivity : PermissionActivity(), RecordingControlCallback, InsertTaskFragment.Insert,
    OnUnitChangedListener, DeleteFileCallback {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private lateinit var mProject: Project
    private lateinit var mUser: User
    private var mInitialChapter = 0
    private var mInitialUnit = 0
    private var mLoadedWav: WavFile? = null
    private var mInsertLocation = 0
    private var mInsertMode = false
    private var isChunkMode = false
    private var mInsertTaskFragment: InsertTaskFragment? = null
    private var mInserting = false
    private var mProgressDialog: ProgressDialog? = null
    private var chunkPlugin: ChunkPlugin? = null
    private var projectProgress: ProjectProgress? = null

    //Fragments
    private var mFragmentHolder: HashMap<Int, Fragment> = hashMapOf()
    private lateinit var mFragmentRecordingFileBar: FragmentRecordingFileBar
    private lateinit var mFragmentVolumeBar: FragmentVolumeBar
    private lateinit var mFragmentRecordingControls: FragmentRecordingControls
    private lateinit var mFragmentSourceAudio: FragmentSourceAudio
    private lateinit var mFragmentRecordingWaveform: FragmentRecordingWaveform

    private lateinit var mRecordingRenderer: ActiveRecordingRenderer

    private var isRecording = false
    private var onlyVolumeTest = true
    private var mNewRecording: WavFile? = null
    private var isPausedRecording = false
    private var isSaved = false
    private var hasStartedRecording = false

    private lateinit var binding: ActivityRecordingScreenBinding

    private val currentUser: String
        get() = prefs.getDefaultPref(Settings.KEY_PROFILE, 1).toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityRecordingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialize(intent)
        initializeTaskFragment(savedInstanceState)
    }

    override fun onPermissionsAccepted() {
        onlyVolumeTest = true
        val volumeTestIntent = Intent(this, WavRecorder::class.java)
        volumeTestIntent.putExtra(WavRecorder.KEY_VOLUME_TEST, onlyVolumeTest)
        startService(volumeTestIntent)
        mRecordingRenderer.listenForRecording(onlyVolumeTest)
    }

    public override fun onPause() {
        Logger.w(this.toString(), "Recording screen onPauseRecording")
        super.onPause()
        if (!requestingPermission.get()) {
            if (isRecording) {
                isRecording = false
                stopService(Intent(this, WavRecorder::class.java))
                RecordingQueues.stopQueues(this)
            } else if (isPausedRecording) {
                RecordingQueues.stopQueues(this)
            } else if (!hasStartedRecording) {
                stopService(Intent(this, WavRecorder::class.java))
                RecordingQueues.stopVolumeTest()
            }
        }
    }

    override fun onBackPressed() {
        Logger.w(this.toString(), "User pressed back")
        if (!isSaved && hasStartedRecording) {
            mFragmentRecordingControls.pauseRecording()
            val exitDialog = ExitDialog.Build(
                this,
                DialogFragment.STYLE_NORMAL,
                false,
                false,
                mNewRecording!!.file
            )
            exitDialog.show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDeleteRecording() {
        isRecording = false
        isPausedRecording = false
        stopService(Intent(this, WavRecorder::class.java))
        RecordingQueues.stopQueues(this)
        RecordingQueues.clearQueues()
        mNewRecording?.file?.delete()
        //originally called from a backpress, so finish by calling super
        super.onBackPressed()
    }

    private fun initialize(intent: Intent) {
        initializeFromSettings()
        parseIntent(intent)
        currentUser
        initializeFragments()
        attachFragments()
        mRecordingRenderer = ActiveRecordingRenderer(
            mFragmentRecordingControls,
            mFragmentVolumeBar,
            mFragmentRecordingWaveform
        )
    }

    private fun initializeFromSettings() {
        mInitialChapter = prefs.getDefaultPref(Settings.KEY_PREF_CHAPTER, ChunkPlugin.DEFAULT_CHAPTER)
        mInitialUnit = prefs.getDefaultPref(Settings.KEY_PREF_CHUNK, ChunkPlugin.DEFAULT_UNIT)
        val userId = prefs.getDefaultPref(Settings.KEY_USER, 1)
        mUser = db.getUser(userId)
    }

    private fun initializeFragments() {
        //initialize fragments
        mFragmentRecordingControls = FragmentRecordingControls.newInstance(
            if ((mInsertMode))
                FragmentRecordingControls.Mode.INSERT_MODE
            else
                FragmentRecordingControls.Mode.RECORDING_MODE
        )
        mFragmentSourceAudio = FragmentSourceAudio.newInstance()
        mFragmentRecordingFileBar = newInstance(
            mProject,
            mInitialChapter,
            mInitialUnit,
            if ((mInsertMode))
                FragmentRecordingControls.Mode.INSERT_MODE
            else
                FragmentRecordingControls.Mode.RECORDING_MODE
        )

        mFragmentVolumeBar = FragmentVolumeBar.newInstance()
        mFragmentRecordingWaveform = newInstance()

        //add fragments to map
        mFragmentHolder[R.id.fragment_recording_controls_holder] = mFragmentRecordingControls

        mFragmentHolder[R.id.fragment_source_audio_holder] = mFragmentSourceAudio
        mFragmentHolder[R.id.fragment_recording_file_bar_holder] = mFragmentRecordingFileBar
        mFragmentHolder[R.id.fragment_volume_bar_holder] = mFragmentVolumeBar
        mFragmentHolder[R.id.fragment_recording_waveform_holder] = mFragmentRecordingWaveform
    }

    private fun attachFragments() {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        val entrySet: Set<Map.Entry<Int, Fragment>> = mFragmentHolder.entries
        for ((key, value) in entrySet) {
            ft.add(key, value)
        }
        ft.commit()
    }

    private fun parseIntent(intent: Intent) {
        mProject = intent.getParcelableExtra(KEY_PROJECT)!!
        //if a chapter and unit does not come from an intent, fallback to the ones from settings
        mInitialChapter = intent.getIntExtra(KEY_CHAPTER, mInitialChapter)
        mInitialUnit = intent.getIntExtra(KEY_UNIT, mInitialUnit)
        if (intent.hasExtra(KEY_WAV_FILE)) {
            mLoadedWav = intent.getParcelableExtra(KEY_WAV_FILE)
        }
        if (intent.hasExtra(KEY_INSERT_LOCATION)) {
            mInsertLocation = intent.getIntExtra(KEY_INSERT_LOCATION, 0)
            mInsertMode = true
        }
        isChunkMode = mProject.modeType == ChunkPlugin.TYPE.MULTI

        try {
            chunkPlugin = mProject.getChunkPlugin(ChunkPluginLoader(directoryProvider, assetsProvider))
            projectProgress = ProjectProgress(mProject, db, chunkPlugin!!.chapters)
        } catch (e: IOException) {
            Logger.e(this.toString(), e.message)
        }
    }

    private fun initializeTaskFragment(savedInstanceState: Bundle?) {
        val fm = supportFragmentManager
        mInsertTaskFragment = fm.findFragmentByTag(TAG_INSERT_TASK_FRAGMENT) as? InsertTaskFragment
        if (mInsertTaskFragment == null) {
            mInsertTaskFragment = InsertTaskFragment()
            fm.beginTransaction().add(mInsertTaskFragment!!, TAG_INSERT_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
        }
        if (savedInstanceState != null) {
            mInserting = savedInstanceState.getBoolean(STATE_INSERTING, false)
            if (mInserting) {
                displayProgressDialog()
            }
        }
    }

    private fun displayProgressDialog() {
        mProgressDialog = ProgressDialog(this).apply {
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setTitle(R.string.inserting_recording)
            setMessage(resources.getString(R.string.please_wait))
            isIndeterminate = true
            setCancelable(false)
            show()
        }
    }

    override fun onStartRecording() {
        hasStartedRecording = true
        mFragmentSourceAudio.disableSourceAudio()
        mFragmentRecordingFileBar.disablePickers()
        onlyVolumeTest = false
        isRecording = true
        stopService(Intent(this, WavRecorder::class.java))
        if (!isPausedRecording) {
            RecordingQueues.stopVolumeTest()
            isSaved = false
            RecordingQueues.clearQueues()
            val startVerse = mFragmentRecordingFileBar.startVerse
            val endVerse = mFragmentRecordingFileBar.endVerse
            val file = createFile(
                mProject,
                mFragmentRecordingFileBar.chapter,
                startVerse.toInt(),
                endVerse.toInt(),
                directoryProvider
            )
            mNewRecording = WavFile(
                file,
                WavMetadata(
                    mProject,
                    currentUser,
                    mFragmentRecordingFileBar.chapter.toString(),
                    startVerse,
                    endVerse
                )
            )
            startService(Intent(this, WavRecorder::class.java))
            startService(WavFileWriter.getIntent(this, mNewRecording))
            mRecordingRenderer.listenForRecording(false)
        } else {
            isPausedRecording = false
            startService(Intent(this, WavRecorder::class.java))
        }
    }

    override fun onPauseRecording() {
        isPausedRecording = true
        isRecording = false
        stopService(Intent(this, WavRecorder::class.java))
        RecordingQueues.pauseQueues()
        Logger.w(this.toString(), "Pausing recording")
    }

    override fun onStopRecording() {
        //Stop recording, load the recorded file, and draw
        stopService(Intent(this, WavRecorder::class.java))
        val start = System.currentTimeMillis()
        Logger.w(this.toString(), "Stopping recording")
        RecordingQueues.stopQueues(this)
        Logger.w(
            this.toString(),
            ("SUCCESS: exited queues, took "
                    + (System.currentTimeMillis() - start)
                    + " to finish writing")
        )
        isRecording = false
        isPausedRecording = false

        mNewRecording?.let { newRecording ->
            addTakeToDb(newRecording)
            newRecording.parseHeader()
            saveLocationToPreferences()

            if (mInsertMode) {
                mLoadedWav?.let { loadedWav ->
                    // need to reparse the sizes after recording
                    // updates to the object aren't reflected due to parceling to the writing service
                    newRecording.parseHeader()
                    loadedWav.parseHeader()

                    finalizeInsert(loadedWav, newRecording, mInsertLocation)
                }
            } else {
                val intent = PlaybackActivity.getPlaybackIntent(
                    this,
                    newRecording,
                    mProject,
                    mFragmentRecordingFileBar.chapter,
                    mFragmentRecordingFileBar.unit
                )
                startActivity(intent)
                this.finish()
            }
        }
    }

    private fun saveLocationToPreferences() {
        prefs.setDefaultPref(Settings.KEY_PREF_CHAPTER, mFragmentRecordingFileBar.chapter)
        prefs.setDefaultPref(Settings.KEY_PREF_CHUNK, mFragmentRecordingFileBar.unit)
    }

    private fun addTakeToDb(newRecording: WavFile) {
        val ppm = mProject.patternMatcher
        ppm.match(newRecording.file)
        db.addTake(
            ppm.takeInfo,
            newRecording.file.name,
            newRecording.metadata.modeSlug,
            newRecording.file.lastModified(),
            0,
            mUser.id
        )
        projectProgress?.updateProjectProgress()
    }

    private fun finalizeInsert(base: WavFile, insertClip: WavFile, insertFrame: Int) {
        mInserting = true
        displayProgressDialog()
        writeInsert(base, insertClip, insertFrame)
    }

    override fun writeInsert(base: WavFile, insertClip: WavFile, insertLoc: Int) {
        mInsertTaskFragment!!.writeInsert(base, insertClip, insertLoc)
    }

    fun insertCallback(result: WavFile) {
        mInserting = false
        try {
            mProgressDialog?.dismiss()
        } catch (e: IllegalArgumentException) {
            Logger.e(this.toString(), "Tried to dismiss insert progress dialog", e)
        }
        val intent = PlaybackActivity.getPlaybackIntent(
            this,
            result,
            mProject,
            mFragmentRecordingFileBar.chapter,
            mFragmentRecordingFileBar.unit
        )
        startActivity(intent)
        this.finish()
    }

    override fun onUnitChanged(project: Project, fileName: String, chapter: Int) {
        mFragmentSourceAudio.resetSourceAudio(project, fileName, chapter)
    }

    companion object {
        const val KEY_PROJECT: String = "key_project"
        const val KEY_WAV_FILE: String = "key_wav_file"
        const val KEY_CHAPTER: String = "key_chapter"
        const val KEY_UNIT: String = "key_unit"
        const val KEY_INSERT_LOCATION: String = "key_insert_location"
        private const val TAG_INSERT_TASK_FRAGMENT = "insert_task_fragment"
        private const val STATE_INSERTING = "state_inserting"

        fun getInsertIntent(
            ctx: Context,
            project: Project,
            wavFile: WavFile,
            chapter: Int,
            unit: Int,
            locationMs: Int
        ): Intent {
            Logger.w("RecordingActivity", "Creating Insert Intent")
            val intent = getRerecordIntent(ctx, project, wavFile, chapter, unit)
            intent.putExtra(KEY_INSERT_LOCATION, locationMs)
            return intent
        }

        fun getNewRecordingIntent(
            ctx: Context,
            project: Project,
            chapter: Int,
            unit: Int
        ): Intent {
            Logger.w("RecordingActivity", "Creating New Recording Intent")
            val intent = Intent(ctx, RecordingActivity::class.java)
            intent.putExtra(KEY_PROJECT, project)
            intent.putExtra(KEY_CHAPTER, chapter)
            intent.putExtra(KEY_UNIT, unit)
            return intent
        }

        fun getRerecordIntent(
            ctx: Context,
            project: Project,
            wavFile: WavFile,
            chapter: Int,
            unit: Int
        ): Intent {
            Logger.w("RecordingActivity", "Creating Rerecord Intent")
            val intent = getNewRecordingIntent(ctx, project, chapter, unit)
            intent.putExtra(KEY_WAV_FILE, wavFile as Parcelable)
            return intent
        }
    }
}
