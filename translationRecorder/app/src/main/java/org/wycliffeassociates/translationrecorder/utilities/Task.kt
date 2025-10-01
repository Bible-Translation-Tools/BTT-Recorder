package org.wycliffeassociates.translationrecorder.utilities

/**
 * Created by sarabiaj on 9/23/2016.
 */

/**
 * Base class for Tasks to run on the TaskFragment
 * Task allows for creation of Runnables that can communicate to the TaskFragment over the RunnableTask interface
 */
abstract class Task(var tag: Int) : RunnableTask {
    private var mCallback: OnTaskProgressListener? = null
    private var mId: Long = 0

    fun setOnTaskProgressListener(progressListener: OnTaskProgressListener?) {
        mCallback = progressListener
        mId = 0
    }

    fun setOnTaskProgressListener(progressListener: OnTaskProgressListener?, id: Long) {
        mCallback = progressListener
        mId = id
    }

    override fun onTaskProgressUpdateDelegator(progress: Int) {
        mCallback?.onTaskProgressUpdate(mId, progress)
    }

    override fun onTaskCompleteDelegator() {
        mCallback?.onTaskComplete(mId)
    }

    override fun onTaskCancelDelegator() {
        mCallback?.onTaskCancel(mId)
    }

    override fun onTaskErrorDelegator(message: String?) {
        mCallback?.onTaskError(mId, message)
    }

    companion object {
        var FIRST_TASK: Int = 1
    }
}