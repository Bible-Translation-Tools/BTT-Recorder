package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import android.content.Context
import android.net.Uri
import com.door43.tools.reporting.Logger
import org.apache.commons.io.FileUtils
import org.wycliffeassociates.io.ArchiveOfHolding
import org.wycliffeassociates.translationrecorder.Utils.deleteRecursive
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException

/**
 * Created by sarabiaj on 9/23/2016.
 */
class ExportSourceAudioTask(
    taskTag: Int,
    private val context: Context,
    private val mProject: Project,
    private val mBookFolder: File,
    private val mStagingRoot: File,
    private val mOutputUri: Uri
) : Task(taskTag) {
    private var mTotalStagingSize: Long = 0
    private var mStagingProgress: Long = 0

    override fun run() {
        mTotalStagingSize = getTotalProgressSize(mBookFolder)
        val input = stageFilesForArchive(mProject, mBookFolder, mStagingRoot)
        //just a guess of progress, giving credit for work done that doesn't have a progress updater
        onTaskProgressUpdateDelegator(10)
        createSourceAudio(input, mOutputUri)
        onTaskCompleteDelegator()
    }

    private fun createSourceAudio(input: File, outputUri: Uri) {
        try {
            context.contentResolver.openOutputStream(outputUri, "w").use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    val aoh = ArchiveOfHolding { i: Int ->
                        //Consider the staging of files to be half the work; so progress from the aoh
                        // needs to be divided in half and added to the half that is already done
                        onTaskProgressUpdateDelegator((i * .5).toInt() + 50)
                    }
                    aoh.createArchiveOfHolding(input, bos)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            deleteRecursive(input)
        }
    }

    private fun getTotalProgressSize(input: File): Long {
        var total: Long = 0
        if (input.isDirectory) {
            val files = input.listFiles()
            if (files != null) {
                for (f in files) {
                    total += getTotalProgressSize(f)
                }
            }
        }
        return total + input.length()
    }

    private fun stageFilesForArchive(project: Project, input: File, stagingRoot: File): File {
        val root = File(
            stagingRoot,
            project.targetLanguageSlug + "_" + project.versionSlug + "_" + project.bookSlug
        )
        val lang = File(root, project.targetLanguageSlug)
        val version = File(lang, project.versionSlug)
        val book = File(version, project.bookSlug)
        book.mkdirs()
        if (input.listFiles() != null) {
            val files = input.listFiles()
            if (files != null) {
                for (c in files) {
                    if (c.isDirectory) {
                        val chapter = File(book, c.name)
                        chapter.mkdir()
                        val chapterFiles = c.listFiles()
                        if (chapterFiles != null) {
                            for (f in chapterFiles) {
                                if (!f.isDirectory) {
                                    try {
                                        FileUtils.copyFileToDirectory(f, chapter)
                                        mStagingProgress += f.length()
                                        //this step accounts for half of the work, so it is multiplied by 50 instead of 100
                                        val progressPercentage =
                                            ((mStagingProgress / mTotalStagingSize.toDouble()) * 50).toInt()
                                        onTaskProgressUpdateDelegator(progressPercentage)
                                    } catch (e: IOException) {
                                        Logger.e(
                                            this.toString(),
                                            "IOException staging files for archive",
                                            e
                                        )
                                        e.printStackTrace()
                                    }
                                }
                            }
                        } else {
                            continue
                        }
                    }
                }
            }
        }
        return root
    }
}
