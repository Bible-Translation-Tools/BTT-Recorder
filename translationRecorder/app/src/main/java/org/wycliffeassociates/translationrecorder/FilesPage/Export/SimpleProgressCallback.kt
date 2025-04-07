package org.wycliffeassociates.translationrecorder.FilesPage.Export

/**
 * Created by sarabiaj on 1/24/2018.
 */
interface SimpleProgressCallback {
    fun onStart(id: Int)
    fun setCurrentFile(id: Int, currentFile: String?)
    fun setUploadProgress(id: Int, progress: Int)
    fun onComplete(id: Int)
}
