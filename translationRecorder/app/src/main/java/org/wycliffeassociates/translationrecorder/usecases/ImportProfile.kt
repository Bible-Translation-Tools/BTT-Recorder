package org.wycliffeassociates.translationrecorder.usecases

import android.media.MediaMetadataRetriever
import android.util.Log
import com.door43.sysutils.FileUtilities
import com.door43.tools.reporting.Logger
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.components.User
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

class ImportProfile @Inject constructor(
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider
) {
    operator fun invoke(profile: File) {
        if (profile.isDirectory) return

        val hash = getHash(profile)
        var mimeType: String? = null

        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(profile.absolutePath)
            mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: Exception) {
            Log.i(this::javaClass.name, "File is not a media file")
        }

        if (hash != null && mimeType != null && mimeType == "audio/mp4") {
            val targetProfile = File(directoryProvider.profilesDir, profile.name)
            FileUtilities.copyFile(profile, targetProfile)

            try {
                db.addUser(User(targetProfile, hash))
            } catch (e: Exception) {
                Logger.e(this::javaClass.name, "Could not add user $hash to database", e)
            }
        }
    }

    private fun getHash(file: File): String? {
        return try {
            String(Hex.encodeHex(DigestUtils.md5(FileInputStream(file))))
        } catch (e: IOException) {
            null
        }
    }
}