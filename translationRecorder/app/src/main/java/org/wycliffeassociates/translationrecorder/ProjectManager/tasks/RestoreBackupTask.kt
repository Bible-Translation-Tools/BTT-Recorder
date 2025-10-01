package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.net.Uri
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.usecases.RestoreBackup
import org.wycliffeassociates.translationrecorder.utilities.Task

/**
 * Created by sarabiaj on 9/27/2016.
 */
class RestoreBackupTask(
    taskTag: Int,
    private val backupUri: Uri,
    private val restoreBackup: RestoreBackup
) : Task(taskTag) {

    override fun run() {
        try {
            val result = restoreBackup(backupUri)
            if (result.success) {
                onTaskCompleteDelegator()
            } else {
                onTaskErrorDelegator(result.message)
            }
        } catch (e: Exception) {
            Logger.e("RestoreBackupTask", "Restore from backup failed", e)
            onTaskErrorDelegator(e.message)
        }
    }
}
