package org.wycliffeassociates.translationrecorder.utilities

/**
 * Created by sarabiaj on 9/23/2016.
 */
interface RunnableTask : Runnable {
    fun onTaskProgressUpdateDelegator(progress: Int)

    fun onTaskCompleteDelegator()

    fun onTaskCancelDelegator()

    fun onTaskErrorDelegator(message: String? = null)
}
