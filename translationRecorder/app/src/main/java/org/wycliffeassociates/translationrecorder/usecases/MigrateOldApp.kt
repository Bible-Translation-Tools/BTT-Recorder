package org.wycliffeassociates.translationrecorder.usecases

import android.content.Context
import android.net.Uri
import com.door43.sysutils.FileUtilities
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import java.io.File
import javax.inject.Inject

class MigrateOldApp @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryProvider: IDirectoryProvider,
    private val importProject: ImportProject,
    private val importProfile: ImportProfile
) {
    operator fun invoke(appDataFolder: Uri) {
        val tempAppDir = directoryProvider.createTempDir("appDir")
        FileUtilities.copyDirectory(context, appDataFolder, tempAppDir)
        importProfiles(tempAppDir)
        importTranslations(tempAppDir)
        FileUtilities.deleteRecursive(tempAppDir)
    }

    private fun importTranslations(appDir: File) {
        if (appDir.isDirectory) {
            val translations = arrayListOf<File>()
            appDir.listFiles()?.forEach { file ->
                if (file.name == "cache") return@forEach
                if (file.name == "Profiles") return@forEach
                if (file.isDirectory) {
                    translations.add(file)
                }
            }
            translations.forEach {
                importProject(it)
            }
        }
    }

    private fun importProfiles(appDir: File) {
        if (appDir.isDirectory) {
            appDir.listFiles()?.forEach { file ->
                if (file.name == "Profiles" && file.isDirectory) {
                    file.listFiles()?.forEach { profile ->
                        importProfile(profile)
                    }
                }
            }
        }
    }
}