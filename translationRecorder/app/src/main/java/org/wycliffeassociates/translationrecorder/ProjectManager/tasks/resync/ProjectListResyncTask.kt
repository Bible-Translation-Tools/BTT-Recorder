package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import androidx.fragment.app.FragmentManager
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RequestLanguageNameDialog
import org.wycliffeassociates.translationrecorder.database.CorruptFileDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnCorruptFile
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper.OnLanguageNotFound
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.project.ProjectProgress
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.utilities.Task
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * Created by sarabiaj on 1/19/2017.
 */
class ProjectListResyncTask(
    taskId: Int,
    private val mFragmentManager: FragmentManager,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider,
    private val chunkPluginLoader: ChunkPluginLoader
) : Task(taskId), OnLanguageNotFound,
    OnCorruptFile {

        private val projectDirectoriesOnFileSystem: Map<Project, File>
        get() {
            val projectDirectories: MutableMap<Project, File> = HashMap()
            val root = directoryProvider.translationsDir
            val langs = root.listFiles()
            if (langs != null) {
                for (lang in langs) {
                    if (!lang.isDirectory) {
                        continue
                    }
                    val versions = lang.listFiles()
                    if (versions != null) {
                        for (version in versions) {
                            if (!version.isDirectory) {
                                continue
                            }
                            val bookDirs = version.listFiles()
                            if (bookDirs != null) {
                                for (bookDir in bookDirs) {
                                    if (!bookDir.isDirectory) {
                                        continue
                                    }
                                    //get the project from the database if it exists
                                    var project = db.getProject(
                                        lang.name,
                                        version.name,
                                        bookDir.name
                                    )
                                    if (project != null) {
                                        projectDirectories[project] = bookDir
                                    } else { //otherwise derive the project from the filename
                                        val chapters =
                                            bookDir.listFiles()
                                        var mode: Mode? =
                                            null
                                        if (chapters != null) {
                                            for (chapter in chapters) {
                                                if (!chapter.isDirectory) {
                                                    continue
                                                }
                                                val c =
                                                    chapter.listFiles()
                                                if (c != null) {
                                                    for (i in c.indices) {
                                                        try {
                                                            val wav = WavFile(c[i])
                                                            mode = db.getMode(
                                                                db.getModeId(
                                                                    wav.metadata
                                                                        .modeSlug,
                                                                    wav.metadata
                                                                        .anthology
                                                                )
                                                            )
                                                        } catch (e: IllegalArgumentException) {
                                                            //don't worry about the corrupt file dialog here;
                                                            // the database resync will pick it up.
                                                            Logger.e(
                                                                toString(),
                                                                c[i].name,
                                                                e
                                                            )
                                                            continue
                                                        }
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                        if (chapters != null && mode != null) {
                                            val languageId = db.getLanguageId(lang.name)
                                            val bookId = db.getBookId(bookDir.name)
                                            val book =
                                                db.getBook(bookId)
                                            val anthologyId =
                                                db.getAnthologyId(book.anthology)
                                            val versionId = db.getVersionId(version.name)
                                            project = Project(
                                                db.getLanguage(languageId),
                                                db.getAnthology(anthologyId),
                                                book,
                                                db.getVersion(versionId),
                                                mode
                                            )
                                            projectDirectories[project] = bookDir
                                        }
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
        val projectDirectories: MutableMap<Project, File> = hashMapOf()
        for (p in projects) {
            projectDirectories[p] = ProjectFileUtils.getProjectDirectory(p, directoryProvider)
        }
        return projectDirectories
    }

    private fun getDirectoriesMissingFromDb(
        fs: Map<Project, File>,
        db: Map<Project, File>
    ): Map<Project, File> {
        val missingDirectories: MutableMap<Project, File> = hashMapOf()
        for ((key, value) in fs) {
            if (!db.containsValue(value)) {
                missingDirectories[key] = value
            }
        }
        return missingDirectories
    }

    override fun run() {
        val directoriesOnFs = projectDirectoriesOnFileSystem
        //if the number of projects doesn't match up between the filesystem and the db, OR,
        //the projects themselves don't match an id in the db, then resync everything (only resyncing
        // projects missing won't remove dangling take references in the db)
        //NOTE: removing a project only removes dangling takes, not the project itself from the db
        val projectCountDiffers = directoriesOnFs.size != db.numProjects
        val projectsNeedResync = db.projectsNeedingResync(directoriesOnFs.keys).isNotEmpty()
        if (projectCountDiffers || projectsNeedResync) {
            fullResync(directoriesOnFs)
        }
        onTaskCompleteDelegator()
    }

    private fun fullResync(directoriesOnFs: Map<Project, File>) {
        val projects: List<Project> = db.allProjects
        val directoriesFromDb = getProjectDirectories(projects)
        val directoriesMissingFromFs =
            getDirectoriesMissingFromDb(directoriesOnFs, directoriesFromDb)

        //get directories of projects
        //check which directories are not in the list
        //for projects with directories, get their files and re-sync
        //for directories not in the list, try to find which pattern match succeeds
        for ((project, value) in directoriesOnFs) {
            val chapters = value.listFiles()
            if (chapters != null) {
                val takes = ResyncUtils.getFilesInDirectory(chapters)
                db.resyncDbWithFs(project, takes, this, this)

                try {
                    val chunkPlugin = project.getChunkPlugin(chunkPluginLoader)
                    // Recalculate project progress
                    val pp = ProjectProgress(project, db, chunkPlugin.chapters)
                    pp.updateProjectProgress()

                    // Recalculate chapters progress
                    pp.updateChaptersProgress()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        for ((key, value) in directoriesMissingFromFs) {
            val chapters = value.listFiles()
            if (chapters != null) {
                val takes = ResyncUtils.getFilesInDirectory(chapters)
                db.resyncDbWithFs(key, takes, this, this)
            }
        }
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
