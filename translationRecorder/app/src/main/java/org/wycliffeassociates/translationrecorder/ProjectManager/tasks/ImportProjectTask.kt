package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import com.door43.sysutils.FileUtilities
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.usecases.APP_DATA_DIR
import org.wycliffeassociates.translationrecorder.usecases.ImportProject
import org.wycliffeassociates.translationrecorder.usecases.USER_DATA_DIR
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by sarabiaj on 9/27/2016.
 */
class ImportProjectTask(
    taskTag: Int,
    private val context: Context,
    private val projectUri: Uri,
    private val directoryProvider: IDirectoryProvider,
    private val importProject: ImportProject
) : Task(taskTag) {

    private var projectInfo: ProjectInfo? = null

    private companion object {
        const val CHAPTER_FILE_REGEX = "([a-zA-Z]{2,3}[-\\w+]*)_[a-zA-Z0-9]{2}_([a-zA-Z]{3})_"
        const val VERSE_FILE_REGEX = "([a-zA-Z]{2,3}[-\\w+]*)_([a-zA-Z]{3})_b"
    }

    override fun run() {
        val uuid = UUID.randomUUID().toString()
        val tempDir = directoryProvider.createTempDir(uuid)
        val extractedDir = File(tempDir, "extracted")

        try {
            context.contentResolver.openInputStream(projectUri)?.use { inputStream ->
                extractZip(inputStream, extractedDir)

                if (validProjectDir(extractedDir)) {
                    projectInfo?.let { info ->
                        val projectDir = File(tempDir, "${info.language}/${info.version}")
                        projectDir.mkdirs()
                        FileUtilities.copyDirectory(extractedDir, projectDir, null)

                        importProject(File(tempDir, info.language))
                        onTaskCompleteDelegator()
                    } ?: run {
                        Logger.e(ImportProjectTask::class.toString(), "Could not define project details")
                        onTaskErrorDelegator(context.getString(R.string.project_details_undefined))
                    }
                } else {
                    Logger.e(ImportProjectTask::class.toString(), "Invalid project backup file.")
                    onTaskErrorDelegator(context.getString(R.string.invalid_project_backup))
                }
            }
        } catch (e: IOException) {
            Logger.e(ImportProjectTask::class.toString(), e.message, e)
            onTaskErrorDelegator(e.message)
        }

        FileUtilities.deleteRecursive(tempDir)
    }

    private fun setProjectInfo(fileName: String) {
        if (projectInfo == null) {
            try {
                val chapterPattern = Pattern.compile(CHAPTER_FILE_REGEX)
                chapterPattern.matcher(fileName).apply {
                    val found = find()
                    if (found) {
                        projectInfo = ProjectInfo(group(1)!!, group(2)!!)
                        return
                    }
                }
                val versePattern = Pattern.compile(VERSE_FILE_REGEX)
                versePattern.matcher(fileName).apply {
                    val found = find()
                    if (found) {
                        projectInfo = ProjectInfo(group(1)!!, group(2)!!)
                        return
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun extractZip(inputStream: InputStream, tempDir: File) {
        ZipInputStream(inputStream).use { zip ->
            var zipEntry: ZipEntry?
            while (zip.nextEntry.also { zipEntry = it } != null) {
                val file = File(tempDir, zipEntry!!.name)
                if (zipEntry!!.isDirectory) {
                    file.mkdirs()
                } else {
                    setProjectInfo(zipEntry!!.name)
                    file.outputStream().use { out ->
                        val buffer = ByteArray(4096)
                        var len: Int
                        while (zip.read(buffer).also { len = it } > 0) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun validProjectDir(projectDir: File): Boolean {
        val dirs = projectDir.listFiles()

        return when {
            dirs == null -> false
            dirs.any { it.name == APP_DATA_DIR || it.name == USER_DATA_DIR } -> false
            else -> true
        }
    }

    private data class ProjectInfo(
        val language: String,
        val version: String
    )
}
