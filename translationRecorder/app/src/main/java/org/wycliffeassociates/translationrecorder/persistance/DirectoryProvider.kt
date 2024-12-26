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

    override val sourceAudioDir: File
        get() {
            val path = File(externalAppDir, "source_audio")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val translationsDir: File
        get() {
            val path = File(externalAppDir, "translations")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val profilesDir: File
        get() {
            val path = File(externalAppDir, "profiles")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val uploadDir: File
        get() {
            val path = File(externalAppDir, "upload")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val pluginsDir: File
        get() {
            val path = File(codeCacheDir, "plugins")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val anthologiesDir: File
        get() {
            val path = File(pluginsDir, "anthologies")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override val visualizationDir: File
        get() {
            val path = File(externalCacheDir, "visualization")
            if (!path.exists()) path.mkdirs()
            return path
        }

    override fun createTempDir(name: String?): File {
        val tempName = name ?: System.currentTimeMillis().toString()
        val tempDir = File(internalCacheDir, tempName)
        tempDir.mkdirs()
        return tempDir
    }

    override fun createTempFile(prefix: String, suffix: String?, dir: File?): File {
        return File.createTempFile(prefix, suffix, dir ?: internalCacheDir)
    }
}