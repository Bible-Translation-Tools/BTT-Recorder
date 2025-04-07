package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import androidx.fragment.app.FragmentManager
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RequestLanguageNameDialog
import org.wycliffeassociates.translationrecorder.database.CorruptFileDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnCorruptFile
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnLanguageNotFound
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Created by sarabiaj on 1/20/2017.
 */
class UnitResyncTask(
    taskId: Int,
    private var mFragmentManager: FragmentManager,
    private val mProject: Project,
    private val mChapter: Int,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider
) : Task(taskId), OnLanguageNotFound, OnCorruptFile {

    private val allTakes: List<File>
        get() {
            val path = String.format(
                "%s/%s/%s/%s/",
                mProject.targetLanguageSlug,
                mProject.versionSlug,
                mProject.bookSlug,
                ProjectFileUtils.chapterIntToString(mProject, mChapter)
            )
            val root = File(directoryProvider.translationsDir, path)
            val dirs = root.listFiles()
            val files: MutableList<File> = if (dirs != null) {
                LinkedList(listOf(*dirs))
            } else {
                ArrayList()
            }
            filterFiles(files)
            return files
        }

    private fun filterFiles(files: MutableList<File>) {
        val iter = files.iterator()
        while (iter.hasNext()) {
            val ppm = mProject.patternMatcher
            ppm.match(iter.next())
            if (!ppm.matched()) {
                iter.remove()
            }
        }
    }

    override fun run() {
        db.resyncChapterWithFilesystem(
            mProject,
            mChapter,
            allTakes,
            this,
            this
        )
        onTaskCompleteDelegator()
    }

    override fun onCorruptFile(file: File) {
        val cfd = CorruptFileDialog.newInstance(file)
        cfd.show(mFragmentManager, "CORRUPT_FILE")
    }

    override fun requestLanguageName(languageCode: String): String {
        val response: BlockingQueue<String> = ArrayBlockingQueue(1)
        val dialog = RequestLanguageNameDialog.newInstance(languageCode, response)
        dialog.show(mFragmentManager, "REQUEST_LANGUAGE")
        try {
            return response.take()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return "???"
    }
}
