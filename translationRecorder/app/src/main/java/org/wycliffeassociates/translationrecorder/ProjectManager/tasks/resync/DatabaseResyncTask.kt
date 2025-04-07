package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import androidx.fragment.app.FragmentManager
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RequestLanguageNameDialog
import org.wycliffeassociates.translationrecorder.database.CorruptFileDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnCorruptFile
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnLanguageNotFound
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getProjectDirectory
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Created by sarabiaj on 9/27/2016.
 */
class DatabaseResyncTask(
    taskId: Int,
    private val mFragmentManager: FragmentManager,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider
) : Task(taskId), OnLanguageNotFound, OnCorruptFile {

    val allTakes: List<File>
        get() {
            val root = directoryProvider.translationsDir
            val dirs = root.listFiles()
            val files: MutableList<File> = LinkedList()
            if (!dirs.isNullOrEmpty()) {
                for (f in dirs) {
                    val list = f.listFiles()
                    if (!list.isNullOrEmpty()) {
                        files.addAll(getFilesInDirectory(list))
                    }
                }
            }
            return files
        }

    private fun getFilesInDirectory(files: Array<File>): List<File> {
        val list: MutableList<File> = LinkedList()
        for (f in files) {
            if (f.isDirectory) {
                val lst = f.listFiles()
                if (!lst.isNullOrEmpty()) {
                    list.addAll(getFilesInDirectory(lst))
                }
            } else {
                list.add(f)
            }
        }
        return list
    }

    private val projectDirectoriesOnFileSystem: Map<Project, File>
        get() {
            val projectDirectories: MutableMap<Project, File> = HashMap()
            val root = directoryProvider.translationsDir
            val langs = root.listFiles()
            if (langs != null) {
                for (lang in langs) {
                    val versions = lang.listFiles()
                    if (versions != null) {
                        for (version in versions) {
                            val books = version.listFiles()
                            if (books != null) {
                                for (book in books) {
                                    val project = db.getProject(
                                        lang.name,
                                        version.name,
                                        book.name
                                    )
                                    if (project != null) {
                                        projectDirectories[project] = book
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return projectDirectories
        }

    private fun getProjectDirectories(projects: List<Project>): Map<Project, File> {
        val projectDirectories: MutableMap<Project, File> = HashMap()
        for (p in projects) {
            projectDirectories[p] = getProjectDirectory(p,directoryProvider)
        }
        return projectDirectories
    }

    private fun getDirectoriesMissingFromDb(
        fs: Map<Project, File>,
        db: Map<Project, File>
    ): Map<Project, File> {
        val missingDirectories: MutableMap<Project, File> = HashMap()
        for ((key, value) in fs) {
            if (!db.containsValue(value)) {
                missingDirectories[key] = value
            }
        }
        return missingDirectories
    }

    override fun run() {
        val projects = db.allProjects
        val directoriesOnFs = projectDirectoriesOnFileSystem
        val directoriesFromDb = getProjectDirectories(projects)
        val directoriesMissingFromFs = getDirectoriesMissingFromDb(
            directoriesOnFs,
            directoriesFromDb
        )

        //get directories of projects
        //check which directories are not in the list
        //for projects with directories, get their files and resync
        //for directories not in the list, try to find which pattern match succeeds
        for ((key, value) in directoriesOnFs) {
            val chapters = value.listFiles()
            if (chapters != null) {
                val takes = getFilesInDirectory(chapters)
                db.resyncDbWithFs(key, takes, this, this)
            }
        }
        for ((key, value) in directoriesMissingFromFs) {
            val chapters = value.listFiles()
            if (chapters != null) {
                val takes = getFilesInDirectory(chapters)
                db.resyncDbWithFs(key, takes, this, this)
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
