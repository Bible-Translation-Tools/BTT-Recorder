package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import org.apache.commons.lang3.StringUtils
import org.wycliffeassociates.translationrecorder.FilesPage.Manifest
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getProjectDirectory
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.concurrent.Volatile

/**
 * Created by sarabiaj on 12/10/2015.
 */
abstract class Export : SimpleProgressCallback {

    interface ProgressUpdateCallback {
        fun showProgress(mode: Boolean)

        fun incrementProgress(progress: Int)

        fun setUploadProgress(progress: Int)

        fun dismissProgress()

        fun setZipping(zipping: Boolean)

        fun setExporting(exporting: Boolean)

        fun setCurrentFile(currentFile: String?)

        fun setProgressTitle(title: String?)

        companion object {
            const val UPLOAD: Boolean = false
            const val ZIP: Boolean = true
        }
    }

    val project: Project
    private val directoryProvider: IDirectoryProvider
    private val db: IProjectDatabaseHelper
    private val assetsProvider: AssetsProvider
    lateinit var fragment: Fragment

    var directoryToZip: File?
    var filesToZip: List<File>?
    var handler: Handler

    @Volatile
    var zipDone: Boolean = false
    var progressCallback: ProgressUpdateCallback? = null
    var zipFile: File? = null

    constructor(
        project: Project,
        directoryProvider: IDirectoryProvider,
        databaseHelper: IProjectDatabaseHelper,
        assetsProvider: AssetsProvider
    ) {
        this.project = project
        this.directoryProvider = directoryProvider
        this.db = databaseHelper
        this.assetsProvider = assetsProvider

        directoryToZip = ProjectFileUtils.getProjectDirectory(project, directoryProvider)
        filesToZip = null
        handler = Handler(Looper.getMainLooper())
    }

    constructor(
        filesToExport: ArrayList<File>,
        project: Project,
        directoryProvider: IDirectoryProvider,
        databaseHelper: IProjectDatabaseHelper,
        assetsProvider: AssetsProvider
    ) {
        this.project = project
        this.directoryProvider = directoryProvider
        this.db = databaseHelper
        this.assetsProvider = assetsProvider

        directoryToZip = null
        filesToZip = filesToExport
        handler = Handler(Looper.getMainLooper())
    }

    fun setFragmentContext(f: Fragment) {
        progressCallback = f as ProgressUpdateCallback
        fragment = f
    }

    /**
     * Guarantees that all Export objects will have an export method
     */
    fun export() {
        initialize()
    }

    protected open fun initialize() {
        writeManifest()
        writeSelectedTakes()
        if (directoryToZip != null) {
            zip(directoryToZip!!)
        } else {
            zip(filesToZip)
        }
    }

    /**
     * Handles the step of the upload following the zipping of files
     * This may mean starting an activity to ask the user where to save,
     * or it may just mean calling upload.
     */
    protected abstract fun handleUserInput()

    fun cleanUp() {
        zipFile?.delete()
    }

    /**
     * Zips files if more than one file is selected
     */
    //TODO: Zip file appears to just use the name of the first file, what should this change to?
    protected fun outputFile(): File {
        val project = project
        var zipName = StringUtils.join(
            arrayOf(
                project.targetLanguageSlug,
                project.anthologySlug,
                project.versionSlug,
                project.bookSlug,
            ),
            "_"
        )
        zipName += ".zip"
        val root = directoryProvider.uploadDir
        root.mkdirs()
        zipFile = File(root, zipName)
        return zipFile!!
    }

    private fun writeManifest() {
        val projectDir = getProjectDirectory(project, directoryProvider)
        if (projectDir.exists()) {
            // writes manifest before backing up
            val manifest = Manifest(project, projectDir, db, directoryProvider, assetsProvider)
            try {
                manifest.createManifestFile()
                writeSelectedTakes()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun writeSelectedTakes() {
        val projectDir = getProjectDirectory(project, directoryProvider)
        val selectedTakes = ArrayList<String>()

        for (file in projectDir.listFiles()!!) {
            if (file.isDirectory && file.name.matches("\\d+".toRegex())) {
                val chapterDir = file
                for (take in chapterDir.listFiles()) {
                    val ppm: ProjectPatternMatcher = project.patternMatcher
                    if (ppm.match(take)) {
                        val takeInfo = ppm.takeInfo!!
                        val chosen = db.getSelectedTakeNumber(takeInfo)
                        val isSelected = chosen == takeInfo.take
                        if (isSelected) {
                            selectedTakes.add(take.name)
                        }
                    }
                }
            }
        }

        val gson = Gson()
        val output = File(projectDir, "selected.json") // an array of selected take file names

        try {
            gson.newJsonWriter(FileWriter(output)).use { jw ->
                jw.beginArray()
                for (item in selectedTakes) {
                    jw.value(item)
                }
                jw.endArray()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Zips files into a single folder
     *
     * @param directoryToZip Directory containing the project to zip
     * @throws IOException
     */
    private fun zip(directoryToZip: File) {
        val zp = ZipProject(directoryToZip)
        zp.zip(outputFile(), this)
    }

    private fun zip(filesToZip: List<File>?) {
        val zp = ZipProject(filesToZip)
        zp.zip(outputFile(), this)
    }

    override fun onStart(id: Int) {
        handler.post {
            zipDone = false
            progressCallback?.setZipping(true)
            progressCallback?.showProgress(ProgressUpdateCallback.ZIP)
        }
    }

    override fun setCurrentFile(id: Int, currentFile: String?) {
        handler.post { progressCallback?.setCurrentFile(currentFile) }
    }

    override fun setUploadProgress(id: Int, progress: Int) {
        handler.post { progressCallback?.setUploadProgress(progress) }
    }

    override fun onComplete(id: Int) {
        handler.post {
            if (id == ZipProject.ZIP_PROJECT_ID) {
                zipDone = true
                progressCallback?.setZipping(false)
                handleUserInput()
            }
            progressCallback?.dismissProgress()
        }
    }

    companion object {
        const val PROGRESS_REFRESH_RATE: Int =
            200 //200 ms refresh for progress dialog (arbitrary value)
    }
}
