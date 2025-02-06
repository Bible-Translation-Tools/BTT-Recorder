package org.wycliffeassociates.translationrecorder

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.door43.tools.reporting.GithubReporter
import com.door43.tools.reporting.GlobalExceptionHandler
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.apache.commons.io.FileUtils
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityProjectManager
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.Reporting.BugReportDialog
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.MainBinding
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectWizardActivity
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

@AndroidEntryPoint
class MainMenu : AppCompatActivity() {
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider

    private var mNumProjects = 0

    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        AudioInfo.SCREEN_WIDTH = metrics.widthPixels

        initApp()
    }

    override fun onResume() {
        super.onResume()

        mNumProjects = db.numProjects

        binding.newRecord.setOnClickListener {
            if (mNumProjects <= 0 || emptyPreferences()) {
                setupNewProject()
            } else {
                startRecordingScreen()
            }
        }

        binding.files.setOnClickListener { v ->
            val intent = Intent(v.context, ActivityProjectManager::class.java)
            startActivityForResult(intent, 0)
            overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_left)
        }

        initViews()
    }

    private fun emptyPreferences(): Boolean {
        return prefs.getDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, -1) == -1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PROJECT_WIZARD_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    val project = data?.getParcelableExtra<Project>(Project.PROJECT_EXTRA)!!
                    if (addProjectToDatabase(project)) {
                        loadProject(project)
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

            else -> {}
        }
    }

    private fun setupNewProject() {
        startActivityForResult(
            Intent(baseContext, ProjectWizardActivity::class.java),
            PROJECT_WIZARD_REQUEST
        )
    }

    private fun startRecordingScreen() {
        val project = Project.getProjectFromPreferences(db, prefs)!!
        val chapter = prefs.getDefaultPref(SettingsActivity.KEY_PREF_CHAPTER, ChunkPlugin.DEFAULT_CHAPTER)
        val unit = prefs.getDefaultPref(SettingsActivity.KEY_PREF_CHUNK, ChunkPlugin.DEFAULT_UNIT)
        val intent = RecordingActivity.getNewRecordingIntent(
            this,
            project,
            chapter,
            unit
        )
        startActivity(intent)
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


    private fun loadProject(project: Project) {
        prefs.setDefaultPref("resume", "resume")

        if (db.projectExists(project)) {
            prefs.setDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, db.getProjectId(project))
        } else {
            Logger.e(
                this.toString(),
                "Project $project doesn't exist in the database"
            )
        }
    }

    fun report(message: String) {
        val t = Thread { reportCrash(message) }
        t.start()
    }

    private fun reportCrash(message: String) {
        val dir = File(directoryProvider.externalCacheDir, STACKTRACE_DIR)
        val stackTraces = GlobalExceptionHandler.getStacktraces(dir)
        val githubTokenIdentifier = resources.getString(R.string.github_token)
        val githubUrl = resources.getString(R.string.github_bug_report_repo)

        // TRICKY: make sure the github_oauth2 token has been set
        if (githubTokenIdentifier != null) {
            val reporter = GithubReporter(this, githubUrl, githubTokenIdentifier)
            if (stackTraces.isNotEmpty()) {
                // upload most recent stacktrace
                reporter.reportCrash(message, File(stackTraces[0]), Logger.getLogFile())
                // empty the log
                try {
                    FileUtils.write(Logger.getLogFile(), "", Charset.defaultCharset())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                archiveStackTraces()
            }
        }
    }

    fun archiveStackTraces() {
        val dir = File(directoryProvider.externalCacheDir, STACKTRACE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val archive = File(dir, "Archive")
        if (!archive.exists()) {
            archive.mkdirs()
        }
        val stackTraces = GlobalExceptionHandler.getStacktraces(dir)
        // delete stackTraces
        for (filePath in stackTraces) {
            val traceFile = File(filePath)
            if (traceFile.exists()) {
                val move = File(archive, traceFile.name)
                traceFile.renameTo(move)
            }
        }
    }

    private fun initViews() {
        val projectId = prefs.getDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, -1)
        if (projectId != -1) {
            val project = db.getProject(projectId)
            if (project != null) {
                var language = project.targetLanguageSlug
                if (language.compareTo("") != 0) {
                    language = db.getLanguageName(language)
                }
                binding.languageView.text = language

                var book = project.bookSlug
                if (book.compareTo("") != 0) {
                    book = project.bookName
                }
                binding.bookView.text = book
            } else {
                prefs.setDefaultPref(SettingsActivity.KEY_RECENT_PROJECT_ID, -1)
                binding.languageView.text = ""
                binding.bookView.text = ""
            }
        } else {
            binding.languageView.text = ""
            binding.bookView.text = ""
        }
    }

    private fun initApp() {
        prefs.setDefaultPref("version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        //configure logger
        val dir = File(directoryProvider.externalCacheDir, STACKTRACE_DIR)
        dir.mkdirs()

        GlobalExceptionHandler.register(dir)
        val minLogLevel = prefs.getDefaultPref(
            KEY_PREF_LOGGING_LEVEL,
            PREF_DEFAULT_LOGGING_LEVEL
        ).toInt()
        configureLogger(minLogLevel, dir)

        //check if we crashed
        val stackTraces = GlobalExceptionHandler.getStacktraces(dir)
        if (stackTraces.isNotEmpty()) {
            val fm = supportFragmentManager
            val brd = BugReportDialog()
            brd.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
            brd.show(fm, "Bug Report Dialog")
        }

        removeUnusedVisualizationFiles()
    }

    @SuppressLint("DefaultLocale")
    private fun removeUnusedVisualizationFiles() {
        val visFilesLocation = directoryProvider.visualizationDir
        val visFiles = visFilesLocation.listFiles() ?: return
        val rootPath = directoryProvider.translationsDir.absolutePath
        val patterns = db.projectPatternMatchers
        for (v in visFiles) {
            var matched = false
            var takeInfo: TakeInfo? = null
            //no idea what project the vis file is, so try all known anthology regexes until one works
            for (ppm in patterns) {
                if (ppm.match(v)) {
                    matched = true
                    takeInfo = ppm.takeInfo
                    break
                }
            }
            if (!matched) {
                v.delete()
                continue
            }
            val found = false
            val slugs = takeInfo!!.projectSlugs
            val path = (rootPath + "/" + slugs.language + "/" + slugs.version + "/" + slugs.book
                    + "/" + String.format("%02d", takeInfo.chapter))
            val visFileWithoutExtension =
                v.name.split(".vis$".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val name =
                visFileWithoutExtension + "_t" + String.format("%02d", takeInfo.take) + ".wav"
            val searchName = File(path, name)
            if (searchName.exists()) {
                //check if the names match up; exclude the path to get to them or the file extension
                if (extractFilename(searchName) == extractFilename(v)) {
                    continue
                }
            }
            if (!found) {
                println("Removing " + v.name)
                v.delete()
            }
        }
    }

    private fun extractFilename(a: File): String {
        if (a.isDirectory) {
            return ""
        }
        val nameWithExtension = a.name
        val hasNoExtension = nameWithExtension.lastIndexOf('.') < 0
        if (hasNoExtension || nameWithExtension.lastIndexOf('.') > nameWithExtension.length) {
            return ""
        }
        val filename = nameWithExtension.substring(0, nameWithExtension.lastIndexOf('.'))
        return filename
    }

    private fun configureLogger(minLogLevel: Int, logDir: File?) {
        val logFile = File(logDir, "log.txt")
        try {
            logFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Logger.configure(logFile, Logger.Level.getLevel(minLogLevel))
        if (logFile.exists()) {
            Logger.w(this.toString(), "SUCCESS: Log file initialized.")
        } else {
            Logger.e(this.toString(), "ERROR: could not initialize log file.")
        }
    }

    companion object {
        const val KEY_PREF_LOGGING_LEVEL: String = "logging_level"
        const val PREF_DEFAULT_LOGGING_LEVEL: String = "1"
        const val STACKTRACE_DIR: String = "stacktrace"

        const val PROJECT_WIZARD_REQUEST: Int = 1
    }
}
