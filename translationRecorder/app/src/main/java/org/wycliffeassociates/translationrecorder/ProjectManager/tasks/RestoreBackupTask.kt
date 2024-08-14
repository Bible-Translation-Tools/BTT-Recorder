package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.door43.tools.reporting.Logger
import net.lingala.zip4j.ZipFile
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Created by sarabiaj on 9/27/2016.
 */
class RestoreBackupTask(
    taskTag: Int,
    private val context: Context,
    private val backupUri: Uri
) : Task(taskTag) {

    private val appDataDir get() = File(context.applicationInfo.dataDir)
    private val userDataDir
        get() = File(
            Environment.getExternalStorageDirectory(),
            context.resources.getString(R.string.folder_name)
        )

    override fun run() {
        val uuid = UUID.randomUUID().toString()
        val tempZipFile = File(context.cacheDir, "$uuid.zip")

        try {
            context.contentResolver.openInputStream(backupUri).use { inputStream ->
                FileOutputStream(tempZipFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (true) {
                        checkNotNull(inputStream)
                        if (inputStream.read(buffer).also { length = it } <= 0) break
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
            ZipFile(tempZipFile).use { zipFile ->
                extractDirectory(zipFile, "user_data/", userDataDir)
                extractDirectory(zipFile, "app_data/", appDataDir)
            }
            tempZipFile.delete()
        } catch (e: IOException) {
            Logger.e(RestoreBackupTask::class.toString(), e.message, e)
        }

        onTaskCompleteDelegator()
    }

    @Throws(IOException::class)
    private fun extractDirectory(zipFile: ZipFile, dirToExtract: String, destDir: File) {
        for (fileHeader in zipFile.fileHeaders) {
            // Check if the file is a child of the specified folder within the zip file
            val filePathInZip = fileHeader.fileName
            if (filePathInZip.startsWith(dirToExtract) && filePathInZip != dirToExtract) {
                // Calculate the relative path of the file by removing the parent folder path
                val relativePath = filePathInZip.substring(dirToExtract.length)

                // Construct the destination file path
                val destFile = File(destDir, relativePath)

                // Ensure the parent directories exist
                if (fileHeader.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    // Extract the file to the destination directory
                    zipFile.extractFile(fileHeader, destFile.parent, destFile.name)
                }
            }
        }
    }
}
