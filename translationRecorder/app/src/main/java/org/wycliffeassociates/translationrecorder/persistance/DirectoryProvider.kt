package org.wycliffeassociates.translationrecorder.persistance

import android.content.Context
import java.io.File

class DirectoryProvider (private val context: Context) : IDirectoryProvider {
    companion object {
        const val TAG = "DirectoryProvider"
    }

    override val internalAppDir: File
        get() = context.filesDir

    override val externalCacheDir: File
        get() = context.externalCacheDir
            ?: throw NullPointerException("External storage is currently unavailable.")

    override val externalAppDir: File
        get() = context.getExternalFilesDir(null)
            ?: throw NullPointerException("External storage is currently unavailable.")

    override val internalCacheDir: File
        get() = context.cacheDir

    override val codeCacheDir: File
        get() = context.codeCacheDir

    override val translationsDir: File
        get() = File(externalAppDir, "translations")

    override val profilesDir: File
        get() = File(externalAppDir, "profiles")

    override val uploadDir: File
        get() = File(externalAppDir, "upload")

    override val pluginsDir: File
        get() = File(codeCacheDir, "plugins")

    override val anthologiesDir: File
        get() = File(pluginsDir, "anthologies")

}