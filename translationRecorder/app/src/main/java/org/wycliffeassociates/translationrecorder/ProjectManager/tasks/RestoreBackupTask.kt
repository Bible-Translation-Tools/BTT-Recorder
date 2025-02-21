package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.net.Uri
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
        restoreBackup(backupUri)
        onTaskCompleteDelegator()
    }
}
