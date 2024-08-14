package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.door43.tools.reporting.Logger
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

/**
 * Created by sarabiaj on 9/27/2016.
 */
class CreateBackupTask(
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
        try {
            val uuid = UUID.randomUUID().toString()
            val tempZipFile = File(context.cacheDir, "$uuid.zip")

            ZipFile(tempZipFile).use { zipper ->
                val zp = ZipParameters()
                zp.compressionLevel = CompressionLevel.ULTRA
                zp.excludeFileFilter = ExcludeFileFilter { file: File ->
                    file.name == "cache" || file.name == "code_cache"
                }

                zipper.addFolder(appDataDir, zp)
                zipper.renameFile(appDataDir.name + "/", "app_data")

                zipper.addFolder(userDataDir, zp)
                zipper.renameFile(userDataDir.name + "/", "user_data")
            }

            context.contentResolver.openOutputStream(backupUri).use { outputStream ->
                FileInputStream(tempZipFile).use { inputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((inputStream.read(buffer).also { length = it }) > 0) {
                        checkNotNull(outputStream)
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
            tempZipFile.delete()
        } catch (e: IOException) {
            Logger.e(CreateBackupTask::class.toString(), e.message, e)
        } catch (e: NullPointerException) {
            Logger.e(CreateBackupTask::class.toString(), e.message, e)
        }

        onTaskCompleteDelegator()
    }
}
