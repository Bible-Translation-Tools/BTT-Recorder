package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.net.Uri
import org.wycliffeassociates.translationrecorder.usecases.MigrateOldApp
import org.wycliffeassociates.translationrecorder.utilities.Task

class MigrateAppFolderTask(
    taskTag: Int,
    private val uri: Uri,
    private val migrateOldApp: MigrateOldApp
) : Task(taskTag) {

    override fun run() {
        migrateOldApp(uri)
        onTaskCompleteDelegator()
    }
}
