package org.wycliffeassociates.translationrecorder.utilities

/**
 * Created by sarabiaj on 9/23/2016.
 */
interface OnTaskProgressListener {
    fun onTaskProgressUpdate(id: Long, progress: Int)

    fun onTaskComplete(id: Long)

    fun onTaskCancel(id: Long)

    fun onTaskError(id: Long, message: String?)
}
