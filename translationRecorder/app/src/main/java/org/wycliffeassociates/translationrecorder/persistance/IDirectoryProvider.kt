package org.wycliffeassociates.translationrecorder.persistance

import java.io.File

interface IDirectoryProvider {

    /**
     * Returns the path to the internal files directory accessible by the app only.
     * This directory is not accessible by other applications and file managers.
     * It's good for storing private data, such as ssh keys.
     * Files saved in this directory will be removed when the application is uninstalled
     */
    val internalAppDir: File

    /**
     * Returns the path to the external files directory accessible by the app only.
     * This directory can be accessed by file managers.
     * It's good for storing user-created data, such as translations and backups.
     * Files saved in this directory will be removed when the application is uninstalled
     */
    val externalAppDir: File

    /**
     * Returns the absolute path to the application specific cache directory on the filesystem.
     */
    val internalCacheDir: File

    /**
     * Returns the path to the external cache directory accessible by the app only.
     */
    val externalCacheDir: File

    /**
     * Returns the absolute path to the application specific code cache directory on the filesystem.
     */
    val codeCacheDir: File

    /**
     * Returns the path to the source audio directory.
     */
    val sourceAudioDir: File

    /**
     * Returns the path to the translations directory.
     */
    val translationsDir: File

    /**
     * Returns the path to the profiles directory.
     */
    val profilesDir: File

    /**
     * Returns the path to the upload directory.
     */
    val uploadDir: File

    /**
     * Returns the path to the plugins directory.
     */
    val pluginsDir: File

    /**
     * Returns the path to the anthologies directory.
     */
    val anthologiesDir: File
}