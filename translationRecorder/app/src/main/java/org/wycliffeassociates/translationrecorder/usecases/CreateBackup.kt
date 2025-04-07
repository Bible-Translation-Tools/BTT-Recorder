package org.wycliffeassociates.translationrecorder.usecases

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject

const val APP_DATA_DIR = "app_data"
const val USER_DATA_DIR = "user_data"
const val CACHE_DIR = "cache"
const val CODE_CACHE_DIR = "code_cache"

class CreateBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryProvider: IDirectoryProvider
) {
    operator fun invoke(backupUri: Uri) {
        val uuid = UUID.randomUUID().toString()
        val tempZipFile = File(directoryProvider.internalCacheDir, "$uuid.zip")

        ZipFile(tempZipFile).use { zipper ->
            val zp = ZipParameters()
            zp.compressionLevel = CompressionLevel.ULTRA
            zp.excludeFileFilter = ExcludeFileFilter { file: File ->
                file.name == CACHE_DIR || file.name == CODE_CACHE_DIR
            }

            val internalDir = directoryProvider.internalAppDir.parentFile!!
            val externalDir = directoryProvider.externalAppDir.parentFile!!

            zipper.addFolder(internalDir, zp)
            zipper.renameFile(internalDir.name + "/", APP_DATA_DIR)

            zipper.addFolder(externalDir, zp)
            zipper.renameFile(externalDir.name + "/", USER_DATA_DIR)
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
    }
}