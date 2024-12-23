package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import com.door43.tools.reporting.Logger
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
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
    private val backupUri: Uri,
    private val directoryProvider: IDirectoryProvider
) : Task(taskTag) {

    override fun run() {
        try {
            val uuid = UUID.randomUUID().toString()
            val tempZipFile = File(directoryProvider.internalCacheDir, "$uuid.zip")

            ZipFile(tempZipFile).use { zipper ->
                val zp = ZipParameters()
                zp.compressionLevel = CompressionLevel.ULTRA
                zp.excludeFileFilter = ExcludeFileFilter { file: File ->
                    file.name == "cache" || file.name == "code_cache"
                }

                val internalDir = directoryProvider.internalAppDir.parentFile!!
                val externalDir = directoryProvider.externalAppDir.parentFile!!

                zipper.addFolder(internalDir, zp)
                zipper.renameFile(internalDir.name + "/", "app_data")

                zipper.addFolder(externalDir, zp)
                zipper.renameFile(externalDir.name + "/", "user_data")
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
