package org.wycliffeassociates.translationrecorder.project

import android.annotation.SuppressLint
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import kotlin.math.max

/**
 * Created by Joe on 3/31/2017.
 */
object ProjectFileUtils {
    @SuppressLint("DefaultLocale")
    fun createFile(
        project: Project,
        chapter: Int,
        startVerse: Int,
        endVerse: Int,
        directoryProvider: IDirectoryProvider
    ): File {
        val dir = getParentDirectory(project, chapter, directoryProvider)
        val nameWithoutTake = project.getFileName(chapter, startVerse, endVerse)
        val take = getLargestTake(project, dir, nameWithoutTake) + 1
        return File(dir, nameWithoutTake + "_t" + String.format("%02d", take) + ".wav")
    }

    private fun getLargestTake(project: Project, directory: File, nameWithoutTake: String): Int {
        val files = directory.listFiles() ?: return 0
        val ppm = project.patternMatcher
        ppm.match(nameWithoutTake)
        val baseTakeInfo = ppm.takeInfo
        var maxTake = max(baseTakeInfo.take.toDouble(), 0.0).toInt()
        for (f in files) {
            ppm.match(f)
            val ti = ppm.takeInfo
            if (baseTakeInfo == ti) {
                maxTake = if ((maxTake < ti.take)) ti.take else maxTake
            }
        }
        return maxTake
    }

    fun getLargestTake(project: Project, directory: File, filename: File): Int {
        val files = directory.listFiles() ?: return 0
        val ppm = project.patternMatcher
        ppm.match(filename)
        val takeInfo = ppm.takeInfo
        var maxTake = takeInfo.take
        for (f in files) {
            ppm.match(f)
            val ti = ppm.takeInfo
            if (takeInfo == ti) {
                maxTake = if ((maxTake < ti.take)) ti.take else maxTake
            }
        }
        return maxTake
    }

    fun getParentDirectory(takeInfo: TakeInfo, directoryProvider: IDirectoryProvider): File {
        val slugs = takeInfo.projectSlugs
        val path = String.format(
            "%s/%s/%s/%s",
            slugs.language,
            slugs.version,
            slugs.book,
            chapterIntToString(slugs.book, takeInfo.chapter)
        )
        val out = File(directoryProvider.translationsDir, path)
        return out
    }

    fun getParentDirectory(
        project: Project,
        file: File,
        directoryProvider: IDirectoryProvider
    ): File {
        val ppm = project.patternMatcher
        ppm.match(file)
        val takeInfo = ppm.takeInfo
        val slugs = takeInfo.projectSlugs
        val path = String.format(
            "%s/%s/%s/%s",
            slugs.language,
            slugs.version,
            slugs.book,
            chapterIntToString(slugs.book, takeInfo.chapter)
        )
        val out = File(directoryProvider.translationsDir, path)
        return out
    }

    fun getParentDirectory(
        project: Project,
        file: String,
        directoryProvider: IDirectoryProvider
    ): File {
        val ppm = project.patternMatcher
        ppm.match(file)
        val takeInfo = ppm.takeInfo
        val slugs = takeInfo.projectSlugs
        val path = String.format(
            "%s/%s/%s/%s",
            slugs.language,
            slugs.version,
            slugs.book,
            chapterIntToString(slugs.book, takeInfo.chapter)
        )
        val out = File(directoryProvider.translationsDir, path)
        return out
    }

    @JvmStatic
    fun getParentDirectory(
        project: Project,
        chapter: Int,
        directoryProvider: IDirectoryProvider
    ): File {
        val path = String.format(
            "%s/%s/%s/%s",
            project.targetLanguageSlug,
            project.versionSlug,
            project.bookSlug,
            chapterIntToString(project, chapter)
        )
        return File(directoryProvider.translationsDir, path)
    }

    @JvmStatic
    fun getProjectDirectory(project: Project, directoryProvider: IDirectoryProvider): File {
        val projectDir = File(
            getLanguageDirectory(project, directoryProvider),
            "${project.versionSlug}/${project.bookSlug}"
        )
        return projectDir
    }

    private fun getLanguageDirectory(project: Project, directoryProvider: IDirectoryProvider): File {
        val projectDir = File(directoryProvider.translationsDir, project.targetLanguageSlug)
        return projectDir
    }

    fun deleteProject(project: Project, directoryProvider: IDirectoryProvider, db: IProjectDatabaseHelper) {
        val dir = getProjectDirectory(project, directoryProvider)
        Utils.deleteRecursive(dir)
        val langDir = getLanguageDirectory(project, directoryProvider)
        val sourceDir = if (project.isOBS) {
            File(langDir, "obs")
        } else {
            File(langDir, project.versionSlug)
        }
        val sourceFiles = sourceDir.listFiles()
        if (sourceDir.exists() && sourceFiles != null && sourceFiles.isEmpty()) {
            sourceDir.delete()
            val langFiles = langDir.listFiles()
            if (langFiles != null && langFiles.isEmpty()) {
                langDir.delete()
            }
        }
        db.deleteProject(project)
    }

    @SuppressLint("DefaultLocale")
    fun chapterIntToString(bookSlug: String, chapter: Int): String {
        val result = if (bookSlug.compareTo("psa") == 0) {
            String.format("%03d", chapter)
        } else {
            String.format("%02d", chapter)
        }
        return result
    }

    @SuppressLint("DefaultLocale")
    @JvmStatic
    fun chapterIntToString(project: Project, chapter: Int): String {
        val result = if (project.bookSlug.compareTo("psa") == 0) {
            String.format("%03d", chapter)
        } else {
            String.format("%02d", chapter)
        }
        return result
    }

    @SuppressLint("DefaultLocale")
    fun unitIntToString(unit: Int): String {
        return String.format("%02d", unit)
    }

    fun getMode(file: WavFile): String {
        return file.metadata.modeSlug
    }

    @JvmStatic
    fun getNameWithoutExtension(file: File): String {
        var name = file.name
        if (name.contains(".wav")) {
            name = name.replace(".wav", "")
        }
        return name
    }

    @JvmStatic
    fun getNameWithoutTake(file: File): String {
        return getNameWithoutTake(file.name)
    }

    @JvmStatic
    fun getNameWithoutTake(file: String): String {
        return file.split("(_t(\\d{2}))?(.wav)?$".toRegex())[0]
    }
}
