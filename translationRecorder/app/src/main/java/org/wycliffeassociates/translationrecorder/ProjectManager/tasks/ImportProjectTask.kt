package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import com.door43.sysutils.FileUtilities
import com.door43.tools.reporting.Logger
import net.lingala.zip4j.ZipFile
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.usecases.ImportProject
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.regex.Pattern

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
        val tempZipFile = File(directoryProvider.internalCacheDir, "$uuid.zip")

        try {
            context.contentResolver.openInputStream(projectUri)?.use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (true) {
                        if (inputStream.read(buffer).also { length = it } <= 0) break
                        outputStream.write(buffer, 0, length)
                    }
                }

                ZipFile(tempZipFile).use { zipFile ->
                    if (!Utils.isAppBackup(zipFile)) {
                        findProjectInfo(zipFile)
                        projectInfo?.let { info ->
                            val projectDir = File(tempDir, "${info.language}/${info.version}")
                            projectDir.mkdirs()

                            zipFile.extractAll(projectDir.absolutePath)

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
            }
        } catch (e: IOException) {
            Logger.e(ImportProjectTask::class.toString(), e.message, e)
            onTaskErrorDelegator(e.message)
        }

        FileUtilities.deleteRecursive(tempDir)
        FileUtilities.deleteRecursive(tempZipFile)
    }

    private fun findProjectInfo(zipFile: ZipFile) {
        zipFile.fileHeaders.forEach { header ->
            if (!header.isDirectory) {
                try {
                    val chapterPattern = Pattern.compile(CHAPTER_FILE_REGEX)
                    chapterPattern.matcher(header.fileName).apply {
                        val found = find()
                        if (found) {
                            projectInfo = ProjectInfo(group(1)!!, group(2)!!)
                            return
                        }
                    }
                    val versePattern = Pattern.compile(VERSE_FILE_REGEX)
                    versePattern.matcher(header.fileName).apply {
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
    }

    private data class ProjectInfo(
        val language: String,
        val version: String
    )
}
