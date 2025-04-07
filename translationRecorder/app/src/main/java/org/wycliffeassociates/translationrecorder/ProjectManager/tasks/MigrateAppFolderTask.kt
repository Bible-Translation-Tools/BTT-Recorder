package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.net.Uri
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.usecases.MigrateOldApp
import org.wycliffeassociates.translationrecorder.utilities.Task

class MigrateAppFolderTask(
    taskTag: Int,
    private val uri: Uri,
    private val migrateOldApp: MigrateOldApp
) : Task(taskTag) {

    override fun run() {
        try {
            migrateOldApp(uri)
            onTaskCompleteDelegator()
        } catch (e: Exception) {
            Logger.e("MigrateAppFolderTask", e.message, e)
            onTaskErrorDelegator(e.message)
        }
    }
}
