package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import androidx.fragment.app.FragmentManager
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RequestLanguageNameDialog
import org.wycliffeassociates.translationrecorder.database.CorruptFileDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnCorruptFile
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnLanguageNotFound
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Created by sarabiaj on 1/23/2017.
 */
class ChapterResyncTask(
    taskId: Int,
    private val mFragmentManager: FragmentManager,
    private val mProject: Project,
    private val db: IProjectDatabaseHelper,
    directoryProvider: IDirectoryProvider
) : Task(taskId), OnLanguageNotFound, OnCorruptFile {
    private var mChapterDir: File

    init {
        val path = String.format(
            "%s/%s/%s/",
            mProject.targetLanguageSlug,
            mProject.versionSlug,
            mProject.bookSlug
        )
        mChapterDir = File(directoryProvider.translationsDir, path)
    }

    private fun getAllChapters(chapterDir: File): List<Int> {
        val chapterList: MutableList<Int> = ArrayList()
        val dirs = chapterDir.listFiles()
        if (dirs != null) {
            for (f in dirs) {
                if (f.isDirectory) {
                    try {
                        chapterList.add(f.name.toInt())
                    } catch (e: NumberFormatException) {
                        Logger.e(
                            this.toString(),
                            "Tried to add chapter " + f.name + " which does not parse as an Integer"
                        )
                    }
                }
            }
        }
        return chapterList
    }

    override fun run() {
        val chapters = getAllChapters(mChapterDir)
        for (i in chapters) {
            if (!db.chapterExists(mProject, i)) {
                db.resyncProjectWithFilesystem(
                    mProject, ResyncUtils.getFilesInDirectory(mChapterDir.listFiles()),
                    this,
                    this
                )
                break
            }
        }
        onTaskCompleteDelegator()
    }

    override fun onCorruptFile(file: File) {
        val cfd = CorruptFileDialog.newInstance(file)
        cfd.show(mFragmentManager, "CORRUPT_FILE")
    }

    override fun requestLanguageName(code: String): String {
        val response: BlockingQueue<String> = ArrayBlockingQueue(1)
        val dialog = RequestLanguageNameDialog.newInstance(code, response)
        dialog.show(mFragmentManager, "REQUEST_LANGUAGE")
        try {
            return response.take()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return "???"
    }
}
