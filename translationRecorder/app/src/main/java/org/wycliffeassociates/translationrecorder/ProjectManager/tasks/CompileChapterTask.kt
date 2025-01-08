package org.wycliffeassociates.translationrecorder.ProjectManager.tasks

import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getParentDirectory
import org.wycliffeassociates.translationrecorder.utilities.Task
import org.wycliffeassociates.translationrecorder.wav.WavFile
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard
import java.io.File

/**
 * Created by sarabiaj on 9/27/2016.
 */
class CompileChapterTask(
    taskTag: Int,
    private val mCardsToCompile: Map<ChapterCard, List<String>>,
    private val mProject: Project,
    private val directoryProvider: IDirectoryProvider
) : Task(taskTag) {

    override fun run() {
        var currentCard = 0
        val totalCards = mCardsToCompile.size
        for ((chapterCard, files) in mCardsToCompile) {
            val sortedFiles = sortFilesInChapter(files)
            val wavFiles = getWavFilesFromName(sortedFiles, chapterCard.chapterNumber)
            WavFile.compileChapter(
                mProject,
                chapterCard.chapterNumber,
                wavFiles,
                directoryProvider
            )
            onTaskProgressUpdateDelegator(((currentCard / totalCards.toFloat()) * 100).toInt())
            currentCard++
        }

        onTaskCompleteDelegator()
    }

    private fun sortFilesInChapter(files: List<String>): List<String> {
        return files.sortedWith { lhs, rhs ->
            val ppmLeft = mProject.patternMatcher
            ppmLeft.match(lhs)
            val takeInfoLeft = ppmLeft.takeInfo!!

            val ppmRight = mProject.patternMatcher
            ppmRight.match(rhs)
            val takeInfoRight = ppmRight.takeInfo!!

            val startLeft = takeInfoLeft.startVerse
            val startRight = takeInfoRight.startVerse
            startLeft.compareTo(startRight)
        }
    }

    private fun getWavFilesFromName(files: List<String>, chapterNumber: Int): List<WavFile> {
        val wavFiles: MutableList<WavFile> = ArrayList()
        val base = getParentDirectory(mProject, chapterNumber, directoryProvider)
        for (s in files) {
            val f = File(base, s)
            wavFiles.add(WavFile(f))
        }
        return wavFiles
    }
}
