package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.net.Uri
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.usecases.CreateBackup
import org.wycliffeassociates.translationrecorder.utilities.Task

/**
 * Created by sarabiaj on 9/27/2016.
 */
class CreateBackupTask(
    taskTag: Int,
    private val backupUri: Uri,
    private val createBackup: CreateBackup
) : Task(taskTag) {

    override fun run() {
        try {
            createBackup(backupUri)
            onTaskCompleteDelegator()
        } catch (e: Exception) {
            Logger.e(CreateBackupTask::class.toString(), e.message, e)
            onTaskErrorDelegator(e.message)
        }
    }
}
