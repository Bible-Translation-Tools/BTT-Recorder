package org.wycliffeassociates.translationrecorder.ProjectManager.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.door43.tools.reporting.Logger
import com.pixplicity.sharp.Sharp
import dagger.hilt.android.AndroidEntryPoint
import jdenticon.Jdenticon
import org.wycliffeassociates.translationrecorder.DocumentationActivity
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export.ProgressUpdateCallback
import org.wycliffeassociates.translationrecorder.FilesPage.Export.ExportTaskFragment
import org.wycliffeassociates.translationrecorder.FilesPage.FeedbackDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ProjectAdapter
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ProjectAdapter.Companion.initializeProjectCard
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog.ExportDelegator
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog.InfoDialogCallback
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog.SourceAudioDelegator
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.ExportSourceAudioTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.ImportProjectTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ProjectListResyncTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.SplashScreen
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityProjectManagementBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.Project.Companion.getProjectFromPreferences
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.project.ProjectWizardActivity
import org.wycliffeassociates.translationrecorder.usecases.ImportProject
import org.wycliffeassociates.translationrecorder.utilities.Task
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment.OnTaskComplete
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import kotlin.concurrent.Volatile

/**
 * Created by sarabiaj on 6/23/2016.
 */
@AndroidEntryPoint
class ActivityProjectManager : AppCompatActivity(), InfoDialogCallback, ExportDelegator,
    ProgressUpdateCallback, SourceAudioDelegator, OnTaskComplete {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider
    @Inject lateinit var importProject: ImportProject

    private lateinit var binding: ActivityProjectManagementBinding

    var mAdapter: ListAdapter? = null
    private var mNumProjects = 0
    private var mPd: ProgressDialog? = null

    @Volatile
    private var mProgress = 0

    @Volatile
    private var mProgressTitle: String? = null

    @Volatile
    private var mZipping = false

    @Volatile
    private var mExporting = false
    private var mExportTaskFragment: ExportTaskFragment? = null
    private var mTaskFragment: TaskFragment? = null

    private var mDbResyncing = false
    private var mSourceAudioFile: File? = null
    private var mProjectToExport: Project? = null
    private var isIdenticonPlaying = false

    private lateinit var openProject: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProjectManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.projectManagementToolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)
            initializeIdenticon()
        }

        if (savedInstanceState != null) {
            mZipping = savedInstanceState.getBoolean(STATE_ZIPPING, false)
            mExporting = savedInstanceState.getBoolean(STATE_EXPORTING, false)
            mProgress = savedInstanceState.getInt(STATE_PROGRESS, 0)
            mProgressTitle = savedInstanceState.getString(STATE_PROGRESS_TITLE, null)
            mDbResyncing = savedInstanceState.getBoolean(STATE_RESYNC, false)
        }
    }

    //This code exists here rather than onResume due to the potential for onResume() -> onResume()
    //This scenario occurs when the user begins to create a new project and backs out. Calling onResume()
    //twice will result in two background processes trying to sync the database, and only one reference
    //will be kept in the activity- thus leaking the reference to the first dialog causing in it never closing
    override fun onStart() {
        super.onStart()
        //Moved this section to onResume so that these dialogs pop up above the dialog info fragment
        //check if fragment was retained from a screen rotation
        val fm = supportFragmentManager
        mExportTaskFragment = fm.findFragmentByTag(TAG_EXPORT_TASK_FRAGMENT) as ExportTaskFragment?
        mTaskFragment = fm.findFragmentByTag(TAG_TASK_FRAGMENT) as TaskFragment?
        //TODO: refactor export to fit the new task fragment
        if (mExportTaskFragment == null) {
            mExportTaskFragment = ExportTaskFragment()
            fm.beginTransaction().add(mExportTaskFragment!!, TAG_EXPORT_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
        } else {
            if (mZipping) {
                zipProgress(mProgress, mProgressTitle)
            } else if (mExporting) {
                exportProgress(mProgress, mProgressTitle)
            }
        }
        if (mTaskFragment == null) {
            mTaskFragment = TaskFragment()
            fm.beginTransaction().add(mTaskFragment!!, TAG_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
        }
        //still need to track whether a db re-sync was issued so as to not issue them in the middle of another
        if (!mDbResyncing) {
            mDbResyncing = true
            val task = ProjectListResyncTask(
                DATABASE_RESYNC_TASK,
                supportFragmentManager,
                db,
                directoryProvider,
                ChunkPluginLoader(directoryProvider, assetsProvider)
            )
            mTaskFragment?.executeRunnable(
                task,
                getString(R.string.resyncing_database),
                getString(R.string.please_wait),
                true
            )
        }

        openProject = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let(::importProject)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mPd != null) {
            outState.putInt(STATE_PROGRESS, mPd!!.progress)
        }
        outState.putString(STATE_PROGRESS_TITLE, mProgressTitle)
        outState.putBoolean(STATE_EXPORTING, mExporting)
        outState.putBoolean(STATE_ZIPPING, mZipping)
        outState.putBoolean(STATE_RESYNC, mDbResyncing)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.project_management_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_import -> {
                import()
                return true
            }
            R.id.action_logout -> {
                logout()
                return true
            }
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_help -> {
                val help = Intent(this, DocumentationActivity::class.java)
                startActivity(help)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews() {
        binding.newProjectFab.setOnClickListener(btnClick)
        binding.newProjectButton.setOnClickListener(btnClick)

        hideProjectsIfEmpty(mNumProjects)
        if (mNumProjects > 0) {
            val recent = initializeRecentProject()
            populateProjectList(recent)
        }
    }

    //Returns the project that was initialized
    private fun initializeRecentProject(): Project? {
        var project: Project? = null
        val projectId = prefs.getDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, -1)
        if (projectId != -1) {
            project = db.getProject(projectId)
            Logger.w(
                this.toString(), ("Recent Project: language " + project!!.targetLanguageSlug
                        + " book " + project.bookSlug + " version "
                        + project.versionSlug + " mode " + project.modeSlug)
            )
        } else {
            val projects = db.allProjects
            if (projects.isNotEmpty()) {
                project = projects[0]
            }
        }
        if (project != null) {
            initializeProjectCard(this, project, db, prefs, binding.recentProject)
            return project
        } else {
            findViewById<View>(R.id.recent_project).visibility = View.GONE
            return null
        }
    }

    private fun initializeIdenticon() {
        val userId = prefs.getDefaultPref(SettingsActivity.KEY_PROFILE, 1)
        val user = db.getUser(userId)!!
        val svg = Jdenticon.toSvg(user.hash!!, 512, 0f)
        binding.identicon?.background = Sharp.loadString(svg).drawable
        binding.identicon?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        binding.identicon?.setOnClickListener(identiconPlayerClick(user.audio.toString()))
    }

    fun hideProjectsIfEmpty(numProjects: Int) {
        if (numProjects > 0) {
            binding.newProjectButton.visibility = View.GONE
        } else {
            binding.projectListLayout.visibility = View.GONE
            binding.newProjectButton.visibility = View.VISIBLE
        }
    }

    private fun removeProjectFromPreferences() {
        prefs.setDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, -1)
    }

    private fun populateProjectList(recent: Project?) {
        val projects = db.allProjects
        if (recent != null) {
            for (i in projects.indices) {
                if (recent == projects[i]) {
                    projects as MutableList
                    projects.removeAt(i)
                    break
                }
            }
        }
        for (p in projects) {
            val projectName = ("language " + p.targetLanguageSlug
                    + " book " + p.bookSlug
                    + " version " + p.versionSlug
                    + " mode " + p.modeSlug)
            Logger.w(
                this.toString(),
                "Project: $projectName"
            )
        }
        mAdapter = ProjectAdapter(this, projects, db, prefs)
        binding.projectList.adapter = mAdapter
    }

    //sets the profile in the preferences to "" then returns to the splash screen
    private fun logout() {
        prefs.setDefaultPref<Int>(SettingsActivity.KEY_PROFILE, null)
        finishAffinity()
        val intent = Intent(this, SplashScreen::class.java)
        startActivity(intent)
    }

    private fun import() {
        openProject.launch("application/zip")
    }

    private fun importProject(uri: Uri) {
        val task = ImportProjectTask(
            IMPORT_TASK,
            this,
            uri,
            directoryProvider,
            importProject
        )
        mTaskFragment?.executeRunnable(
            task,
            getString(R.string.importing_project),
            getString(R.string.please_wait),
            false
        )
    }

    private fun createNewProject() {
        startActivityForResult(
            Intent(baseContext, ProjectWizardActivity::class.java),
            PROJECT_WIZARD_REQUEST
        )
    }

    private fun loadProject(project: Project) {
        if (!db.projectExists(project)) {
            Logger.e(
                this.toString(),
                "Project $project does not exist"
            )
        }
        val projectId = db.getProjectId(project)
        prefs.setDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, projectId)
    }

    private fun addProjectToDatabase(project: Project): Boolean {
        if (db.projectExists(project)) {
            ProjectWizardActivity.displayProjectExists(this)
            return false
        } else {
            db.addProject(project)
            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PROJECT_WIZARD_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val project = data?.getParcelableExtra<Project>(Project.PROJECT_EXTRA)!!
                    if (addProjectToDatabase(project)) {
                        loadProject(project)
                        finish()
                        //TODO: should find place left off at?
                        val intent = RecordingActivity.getNewRecordingIntent(
                            this,
                            project,
                            ChunkPlugin.DEFAULT_CHAPTER,
                            ChunkPlugin.DEFAULT_UNIT
                        )
                        startActivity(intent)
                    } else {
                        onResume()
                    }
                } else {
                    onResume()
                }
            }
            SAVE_SOURCE_AUDIO_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    uri?.let {
                        try {
                            mProjectToExport?.let { project ->
                                val task = ExportSourceAudioTask(
                                    SOURCE_AUDIO_TASK,
                                    baseContext,
                                    project,
                                    ProjectFileUtils.getProjectDirectory(project, directoryProvider),
                                    directoryProvider.internalAppDir,
                                    it
                                )
                                mTaskFragment!!.executeRunnable(
                                    task,
                                    getString(R.string.exporting_source_audio),
                                    getString(R.string.please_wait),
                                    false
                                )
                            }
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private val btnClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.new_project_button, R.id.new_project_fab -> createNewProject()
        }
    }

    private fun identiconPlayerClick(audioPath: String): View.OnClickListener {
        return View.OnClickListener {
            if (!isIdenticonPlaying) {
                try {
                    val player = MediaPlayer()
                    player.setDataSource(audioPath)
                    player.prepare()
                    player.setOnPreparedListener(onIdenticonPlayerPrepared())
                    player.setOnCompletionListener(onIdenticonPlayerCompleted())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun onIdenticonPlayerCompleted(): OnCompletionListener {
        return OnCompletionListener { mp ->
            mp.release()
            isIdenticonPlaying = false
        }
    }

    private fun onIdenticonPlayerPrepared(): OnPreparedListener {
        return OnPreparedListener { mp ->
            isIdenticonPlaying = true
            mp.start()
        }
    }

    override fun onDelete(project: Project) {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle(getString(R.string.delete_project))
            .setMessage(getString(R.string.confirm_delete_project_alt))
            .setPositiveButton(getString(R.string.yes), object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        val projectName = ("language " + project.targetLanguageSlug
                                + " book " + project.bookSlug + " version "
                                + project.versionSlug + " mode " + project.modeSlug)
                        Logger.w(
                            this.toString(),
                            "Delete Project: $projectName"
                        )
                        if (project == getProjectFromPreferences(db, prefs)) {
                            removeProjectFromPreferences()
                        }
                        ProjectFileUtils.deleteProject(project, directoryProvider, db)
                        hideProjectsIfEmpty(mAdapter!!.count)
                        mNumProjects--
                        initializeViews()
                    }
                }
            })
            .setNegativeButton(
                getString(R.string.no)
            ) { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun exportProgress(progress: Int, title: String?) {
        mPd = ProgressDialog(this)
        mPd!!.setTitle(title ?: getString(R.string.uploading))
        mPd!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mPd!!.progress = progress
        mPd!!.setCancelable(false)
        mPd!!.show()
    }

    private fun zipProgress(progress: Int, title: String?) {
        mPd = ProgressDialog(this).apply {
            setTitle(title ?: getString(R.string.packaging_files))
            setMessage(getString(R.string.please_wait))
            this.progress = progress
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            setCancelable(false)
            show()
        }
    }

    override fun dismissProgress() {
        mPd?.dismiss()
    }

    override fun incrementProgress(progress: Int) {
        mPd?.incrementProgressBy(progress)
    }

    override fun setUploadProgress(progress: Int) {
        mPd?.progress = progress
    }

    override fun showProgress(mode: Boolean) {
        if (mode) {
            zipProgress(0, mProgressTitle)
        } else {
            exportProgress(0, mProgressTitle)
        }
    }

    override fun setZipping(zipping: Boolean) {
        mZipping = zipping
    }

    override fun setExporting(exporting: Boolean) {
        mExporting = exporting
    }

    override fun setCurrentFile(currentFile: String?) {
        mPd?.setMessage(currentFile)
    }

    override fun setProgressTitle(title: String?) {
        mProgressTitle = title
        mPd?.setTitle(mProgressTitle)
    }

    public override fun onPause() {
        super.onPause()
        dismissExportProgressDialog()
    }

    private fun dismissExportProgressDialog() {
        if (mPd != null && mPd!!.isShowing) {
            mPd?.dismiss()
            mPd = null
        }
    }

    override fun delegateExport(exp: Export) {
        mExportTaskFragment?.let { fragment ->
            exp.setFragmentContext(fragment)
            fragment.delegateExport(exp)
        }
    }

    override fun delegateSourceAudio(project: Project) {
        mProjectToExport = project
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")
        val trName = (project.targetLanguageSlug + "_"
                + project.versionSlug + "_"
                + project.bookSlug + "_"
                + project.modeSlug
                + ".tr")
        mSourceAudioFile = File(directoryProvider.internalAppDir, trName)
        intent.putExtra(Intent.EXTRA_TITLE, mSourceAudioFile!!.name)
        startActivityForResult(intent, SAVE_SOURCE_AUDIO_REQUEST)
    }

    override fun onTaskComplete(taskTag: Int, resultCode: Int) {
        if (resultCode == TaskFragment.STATUS_OK) {
            when (taskTag) {
                DATABASE_RESYNC_TASK -> {
                    mNumProjects = db.numProjects
                    mDbResyncing = false
                    initializeViews()
                }
                SOURCE_AUDIO_TASK -> {
                    val fd = FeedbackDialog.newInstance(
                        getString(R.string.source_audio),
                        getString(R.string.source_generation_complete)
                    )
                    fd.show(supportFragmentManager, "SOURCE_AUDIO")
                }
                IMPORT_TASK -> {
                    populateProjectList(null)
                    val fd = FeedbackDialog.newInstance(
                        getString(R.string.import_project),
                        getString(R.string.project_import_complete)
                    )
                    fd.show(supportFragmentManager, "PROJECT_IMPORT")
                }
            }
        } else if (resultCode == TaskFragment.STATUS_ERROR) {
            when (taskTag) {
                SOURCE_AUDIO_TASK -> {
                    val fd = FeedbackDialog.newInstance(
                        getString(R.string.source_audio),
                        getString(R.string.source_generation_failed)
                    )
                    fd.show(supportFragmentManager, "SOURCE_AUDIO")
                }
                IMPORT_TASK -> {
                    val fd = FeedbackDialog.newInstance(
                        getString(R.string.import_project),
                        getString(R.string.project_import_failed)
                    )
                    fd.show(supportFragmentManager, "PROJECT_IMPORT")
                }
            }
        }
    }

    companion object {
        val SOURCE_AUDIO_TASK: Int = Task.FIRST_TASK
        private val DATABASE_RESYNC_TASK = Task.FIRST_TASK + 1
        private val EXPORT_TASK: Int = Task.FIRST_TASK + 2
        private val IMPORT_TASK: Int = Task.FIRST_TASK + 3

        private const val TAG_EXPORT_TASK_FRAGMENT = "export_task_fragment"
        private const val TAG_TASK_FRAGMENT = "task_fragment"
        private const val STATE_EXPORTING = "was_exporting"
        private const val STATE_ZIPPING = "was_zipping"
        private const val STATE_RESYNC = "db_resync"

        private const val STATE_PROGRESS = "upload_progress"
        private const val STATE_PROGRESS_TITLE = "upload_progress_title"

        const val PROJECT_WIZARD_REQUEST: Int = RESULT_FIRST_USER
        const val SAVE_SOURCE_AUDIO_REQUEST: Int = RESULT_FIRST_USER + 1
    }
}
