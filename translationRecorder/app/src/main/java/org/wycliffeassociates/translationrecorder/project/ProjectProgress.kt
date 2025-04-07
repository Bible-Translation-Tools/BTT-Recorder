package org.wycliffeassociates.translationrecorder.project

import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import kotlin.math.ceil


/**
 * Created by mXaln on 7/31/2019.
 */
class ProjectProgress(
    private val project: Project,
    private val db: IProjectDatabaseHelper,
    private val chapters: List<Chapter>
) {
    fun updateProjectProgress() {
        val projectProgress = calculateProjectProgress()
        setProjectProgress(projectProgress)
    }

    private fun calculateProjectProgress(): Int {
        val numChapters = numChapters()
        var allChaptersProgress = 0

        for (chapter in chapters) {
            val chapterProgress = calculateChapterProgress(chapter)
            allChaptersProgress += chapterProgress
        }
        val projectProgress = ceil((allChaptersProgress.toFloat() / numChapters).toDouble()).toInt()

        return projectProgress
    }

    private fun setProjectProgress(progress: Int) {
        try {
            val projectId = db.getProjectId(project)
            db.setProjectProgress(projectId, progress)
        } catch (e: IllegalArgumentException) {
            Logger.i(this.toString(), e.message)
        }
    }

    private fun calculateChapterProgress(chapter: Chapter): Int {
        val unitCount = chapter.chunks.size
        val chapterNumber = chapter.number
        val unitsStarted: Map<Int, Int?> = db.getNumStartedUnitsInProject(project)
        if (unitsStarted[chapterNumber] != null) {
            val chapterProgress = calculateProgress(unitsStarted[chapterNumber]!!, unitCount)
            return chapterProgress
        }

        return 0
    }

    private fun setChapterProgress(chapter: Chapter, progress: Int) {
        try {
            val chapterId = db.getChapterId(project, chapter.number)
            db.setChapterProgress(chapterId, progress)
        } catch (e: IllegalArgumentException) {
            Logger.i(this.toString(), e.message)
        }
    }

    private fun updateChapterProgress(chapter: Chapter) {
        val chapterProgress = calculateChapterProgress(chapter)
        setChapterProgress(chapter, chapterProgress)
    }

    fun updateChaptersProgress() {
        for (chapter in chapters) {
            updateChapterProgress(chapter)
        }
    }

    private fun getChapter(chapterNumber: Int): Chapter? {
        for (chapter in chapters) {
            if (chapter.number == chapterNumber) {
                return chapter
            }
        }
        return null
    }

    private fun calculateProgress(current: Int, total: Int): Int {
        return Math.round((current.toFloat() / total) * 100)
    }

    private fun numChapters(): Int {
        return chapters.size
    }
}
