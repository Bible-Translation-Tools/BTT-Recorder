package org.wycliffeassociates.translationrecorder.utilities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.Volatile

/**
 * TaskFragment allows for the hosting of an arbitrary number of threads that need to block the UI and
 * report progress. The TaskFragment will persist through an activity orientation change, maintain a progress
 * dialog for each running task, and provide a callback to the activity on completion of the task using the OnTaskComplete
 * interface.
 */
class TaskFragment : Fragment(), OnTaskProgressListener {
    //provides a callback for the activity that initiated the task
    interface OnTaskComplete {
        fun onTaskComplete(taskTag: Int, resultCode: Int)
    }

    @Volatile
    var mIdGenerator: AtomicLong = AtomicLong(0)

    var handler: Handler? = null
    private var taskCompleteDelegator: OnTaskComplete? = null
    private val taskHolder: HashMap<Long, TaskHolder> = hashMapOf()

    @Synchronized
    @Throws(IllegalArgumentException::class)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnTaskComplete) {
            taskCompleteDelegator = context
        } else {
            throw IllegalArgumentException("Activity does not implement OnTaskComplete")
        }
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        } else {
            //Post to the handler so that dialog fragments will be attached to the activity prior to
            //these progress dialogs.
            handler!!.post {
                synchronized(taskHolder) {
                    val keys: Set<Long> = taskHolder.keys
                    for (id in keys) {
                        //duplicate code from configure progress dialog, however, it appears to be necessary for the dialog to display and set progress properly
                        val task = taskHolder[id]
                        task!!.dismissDialog()
                        val pd = configureProgressDialog(
                            task.mTitle,
                            task.mMessage,
                            task.progress,
                            task.isIndeterminate
                        )
                        task.progressDialog = pd
                        task.showProgress()
                        task.progressDialog?.progress = task.progress
                    }
                }
            }
        }
    }

    /**
     * Dismisses all progress dialogs and stores their current progress in order to recreate a new
     * dialog if a new activity is attached. We can trust that this (being a lifecycle method) will run
     * on the UI thread and therefore the activity reference will not be null, so posting to the UI Thread
     * here is safe.
     */
    @Synchronized
    override fun onDetach() {
        super.onDetach()
        (taskCompleteDelegator as Activity).runOnUiThread {
            synchronized(taskHolder) {
                val keys: Set<Long> = taskHolder.keys
                for (id in keys) {
                    //duplicate code from configure progress dialog, however, it appears to be necessary for the dialog to display and set progress properly
                    taskHolder[id]?.dismissDialog()
                }
            }
        }
    }

    /**
     * Method to post a thread to run on the TaskFragment.
     *
     * @param task            An Runnable that implements the interfaces to communicate progress to the TaskFragment
     * @param progressTitle   The title of the progress dialog to be created for the provided task
     * @param progressMessage The message of the progress dialog to be created for the provided task
     * @param indeterminate   Whether the progress dialog should display progress or is indeterminate for the provided task
     */
    @Synchronized
    fun executeRunnable(
        task: Task,
        progressTitle: String,
        progressMessage: String,
        indeterminate: Boolean
    ) {
        synchronized(taskHolder) {
            val id = mIdGenerator.incrementAndGet()
            task.setOnTaskProgressListener(this@TaskFragment, id)
            val th = TaskHolder(task, progressTitle, progressMessage)
            taskHolder[id] = th
            val pd = configureProgressDialog(progressTitle, progressMessage, 0, indeterminate)
            th.progressDialog = pd
            th.isIndeterminate = indeterminate
            th.showProgress()
            th.startTask()
        }
    }

    private fun configureProgressDialog(
        title: String,
        message: String,
        progress: Int,
        indeterminate: Boolean
    ): ProgressDialog {
        val pd = ProgressDialog(activity)
        pd.isIndeterminate = indeterminate
        if (!indeterminate) {
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            pd.progress = progress
            pd.max = 100
        }
        pd.setTitle(title)
        pd.setMessage(message)
        pd.setCancelable(false)
        return pd
    }


    override fun onTaskProgressUpdate(id: Long, progress: Int) {
        handler!!.post {
            synchronized(taskHolder) {
                val task = taskHolder[id]
                task?.progressDialog?.progress = progress
            }
        }
    }

    @Synchronized
    private fun endTask(id: Long, status: Int) {
        handler!!.post {
            synchronized(taskHolder) {
                val task = taskHolder[id]
                task?.dismissDialog()
                taskCompleteDelegator?.onTaskComplete(task!!.taskTag, status)
                taskHolder.remove(id)
            }
        }
    }

    override fun onTaskComplete(id: Long) {
        endTask(id, STATUS_OK)
    }

    override fun onTaskCancel(id: Long) {
        endTask(id, STATUS_CANCEL)
    }

    override fun onTaskError(id: Long) {
        endTask(id, STATUS_ERROR)
    }

    inner class TaskHolder(var mTask: Task, var mTitle: String, var mMessage: String) {
        var progressDialog: ProgressDialog? = null
        var progress: Int = 0
        var mThread: Thread = Thread(mTask)
        var isIndeterminate: Boolean = true

        val taskTag: Int
            get() = mTask.tag

        fun startTask() {
            mThread.start()
        }

        fun dismissDialog() {
            progressDialog?.dismiss()
        }

        fun showProgress() {
            if (progressDialog?.isShowing == false) {
                progressDialog?.show()
            }
        }
    }

    companion object {
        private const val STATUS_CANCEL = 0
        @JvmField
        var STATUS_OK: Int = 1
        var STATUS_ERROR: Int = -1
    }
}
