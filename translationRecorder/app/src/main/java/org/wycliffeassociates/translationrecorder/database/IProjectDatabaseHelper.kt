package org.wycliffeassociates.translationrecorder.database

import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ProjectListResyncTask
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.User
import org.wycliffeassociates.translationrecorder.project.components.Version
import java.io.File

interface IProjectDatabaseHelper {

    interface OnLanguageNotFound {
        fun requestLanguageName(languageCode: String): String
    }

    interface OnCorruptFile {
        fun onCorruptFile(file: File)
    }

    val languages: List<Language>
    val allUsers: List<User>
    val projectPatternMatchers: List<ProjectPatternMatcher>
    val allProjects: List<Project>
    val numProjects: Int
    val anthologies: List<Anthology>

    fun updateSourceAudio(projectId: Int, projectContainingUpdatedSource: Project)

    fun projectsNeedingResync(allProjects: Set<Project>): List<Project>

    fun deleteAllTables()

    fun languageExists(languageSlug: String): Boolean

    fun bookExists(bookSlug: String): Boolean

    fun projectExists(project: Project): Boolean

    fun projectExists(languageSlug: String, bookSlug: String, versionSlug: String): Boolean

    fun chapterExists(project: Project, chapter: Int): Boolean

    fun chapterExists(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int
    ): Boolean

    fun unitExists(project: Project, chapter: Int, startVerse: Int): Boolean

    fun unitExists(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int,
        startVerse: Int
    ): Boolean

    fun takeExists(project: Project, chapter: Int, startVerse: Int, take: Int): Boolean

    fun takeExists(takeInfo: TakeInfo): Boolean

    fun getLanguageId(languageSlug: String): Int

    fun getVersionId(versionSlug: String): Int

    fun getAnthologyId(anthologySlug: String): Int

    fun getBookId(bookSlug: String): Int

    fun getProjectId(project: Project): Int

    fun getProjectId(languageSlug: String, bookSlug: String, versionSlug: String): Int

    fun getChapterId(project: Project, chapter: Int): Int

    fun getProjectId(fileName: String): Int

    fun getChapterId(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int
    ): Int

    fun getUnitId(project: Project, chapter: Int, startVerse: Int): Int

    fun getUnitId(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int,
        startVerse: Int
    ): Int

    fun getUnitId(fileName: String): Int

    fun getTakeId(takeInfo: TakeInfo): Int

    fun getModeId(modeSlug: String, anthologySlug: String): Int

    fun getTakeCount(unitId: Int): Int

    fun getLanguageName(languageSlug: String): String

    fun getLanguageCode(id: Int): String

    fun getLanguage(id: Int): Language

    fun getUser(id: Int): User?

    fun addUser(user: User)

    fun deleteUser(hash: String): Int

    fun getBookName(bookSlug: String): String

    fun getBookSlug(id: Int): String

    fun getMode(id: Int): Mode

    fun getBook(id: Int): Book

    fun getVersionName(id: Int): String

    fun getVersionSlug(id: Int): String

    fun getVersion(id: Int): Version

    fun getAnthologySlug(id: Int): String

    fun getAnthologySlug(bookSlug: String): String

    fun getAnthology(id: Int): Anthology

    fun getBookNumber(bookSlug: String): Int

    fun addLanguage(languageSlug: String?, name: String?)

    fun addLanguages(languages: List<Language>)

    fun addAnthology(
        anthologySlug: String?,
        name: String?,
        resource: String?,
        sort: Int,
        regex: String?,
        groups: String?,
        mask: String?,
        jarName: String?,
        className: String?
    )

    fun addBook(bookSlug: String?, bookName: String?, anthologySlug: String, bookNumber: Int)

    fun addBooks(books: List<Book>)

    fun addMode(slug: String?, name: String?, type: String?, anthologySlug: String)

    fun addModes(modes: List<Mode>, anthologySlug: String)

    fun addVersion(versionSlug: String?, versionName: String?)

    fun addVersions(versions: List<Version>)

    fun addVersionRelationships(anthologySlug: String, versions: List<Version>)

    fun addProject(p: Project)

    fun addProject(languageSlug: String, bookSlug: String, versionSlug: String, modeSlug: String)

    fun addChapter(project: Project, chapter: Int)

    fun addChapter(languageSlug: String, bookSlug: String, versionSlug: String, chapter: Int)

    fun addUnit(project: Project, chapter: Int, startVerse: Int)

    fun addUnit(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int,
        startVerse: Int
    )

    fun addTake(
        takeInfo: TakeInfo,
        takeFilename: String?,
        modeSlug: String,
        timestamp: Long,
        rating: Int,
        userId: Int
    )

    fun getProject(projectId: Int): Project?

    fun getProject(languageSlug: String, versionSlug: String, bookSlug: String): Project?

    fun getChapterCheckingLevel(project: Project, chapter: Int): Int

    fun getTakeRating(takeInfo: TakeInfo): Int

    fun getTakeUser(takeInfo: TakeInfo): User?

    fun getSelectedTakeId(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int,
        startVerse: Int
    ): Int

    fun getSelectedTakeNumber(
        languageSlug: String,
        bookSlug: String,
        versionSlug: String,
        chapter: Int,
        startVerse: Int
    ): Int

    fun getSelectedTakeNumber(takeInfo: TakeInfo): Int

    fun setSelectedTake(takeInfo: TakeInfo)

    fun setSelectedTake(unitId: Int, takeId: Int)

    fun setTakeRating(takeInfo: TakeInfo, rating: Int)

    fun setCheckingLevel(project: Project, chapter: Int, checkingLevel: Int)

    fun setChapterProgress(chapterId: Int, progress: Int)

    fun getChapterProgress(chapterId: Int): Int

    fun getProjectProgressSum(projectId: Int): Int

    fun getProjectProgress(projectId: Int): Int

    fun setProjectProgress(projectId: Int, progress: Int)

    fun removeSelectedTake(takeInfo: TakeInfo)

    fun deleteProject(p: Project)

    fun deleteTake(takeInfo: TakeInfo)

    fun getNumStartedUnitsInProject(project: Project): Map<Int, Int>

    fun getTakesForChapterCompilation(project: Project, chapter: Int): List<String>

    fun resyncProjectWithFilesystem(
        project: Project,
        takes: List<File>,
        onCorruptFile: OnCorruptFile,
        onLanguageNotFound: OnLanguageNotFound?
    )

    fun resyncChapterWithFilesystem(
        project: Project,
        chapter: Int,
        takes: List<File>,
        onCorruptFile: OnCorruptFile,
        onLanguageNotFound: OnLanguageNotFound?
    )

    fun resyncDbWithFs(
        project: Project,
        takes: List<File>,
        onCorruptFile: OnCorruptFile,
        onLanguageNotFound: OnLanguageNotFound?
    )

    fun resyncBookWithFs(
        project: Project,
        takes: List<File>,
        languageNotFoundCallback: OnLanguageNotFound?
    )

    fun resyncProjectsWithFs(
        allProjects: List<Project>,
        projectLevelResync: ProjectListResyncTask
    ): List<Project>

    fun autoSelectTake(unitId: Int)

    fun getBooks(anthologySlug: String): List<Book>

    fun getVersions(anthologySlug: String): List<Version>

    fun getModes(anthologySlug: String): List<Mode>

    fun updateProject(projectId: Int, project: Project)
}