package org.wycliffeassociates.translationrecorder.usecases

import com.door43.sysutils.FileUtilities
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.util.Locale
import javax.inject.Inject

class ImportProject @Inject constructor(
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider
) {

    operator fun invoke(projectDir: File) {
        try {
            if (!projectDir.isDirectory) throw IllegalArgumentException("Invalid project folder.")

            val userId = db.allUsers.firstOrNull()?.id ?: -1
            if (userId < 0) throw IllegalArgumentException("No users found in database.")

            val files = projectDir.walkTopDown().toList()
                .filter { it.extension.lowercase(Locale.getDefault()) == "wav" }
            val audio = files.firstOrNull()

            if (audio != null) {
                val metadata = WavFile(audio).metadata
                val languageSlug = metadata.language
                val anthologySlug = metadata.anthology
                val versionSlug = metadata.version
                val bookSlug = metadata.slug
                val modeSlug = metadata.modeSlug

                val existentProject = db.getProject(languageSlug, versionSlug, bookSlug)

                if (existentProject == null || existentProject.modeSlug != modeSlug) {
                    val languageId = db.getLanguageId(languageSlug)
                    if (languageId >=0 ) {
                        val language = db.getLanguage(languageId)
                        val anthologyId = db.getAnthologyId(anthologySlug)
                        val anthology = db.getAnthology(anthologyId)
                        val bookId = db.getBookId(bookSlug)
                        val book = db.getBook(bookId)
                        val versionId = db.getVersionId(versionSlug)
                        val version = db.getVersion(versionId)
                        val modeId = db.getModeId(modeSlug, anthologySlug)
                        val mode = db.getMode(modeId)

                        val project = Project(
                            language,
                            anthology,
                            book,
                            version,
                            mode
                        )
                        db.addProject(project)

                        val targetProjectDir = File(directoryProvider.translationsDir, projectDir.name)
                        FileUtilities.copyDirectory(projectDir, targetProjectDir, null)

                        val targetFiles = targetProjectDir.walkTopDown().toList()
                            .filter { it.extension.lowercase(Locale.getDefault()) == "wav" }

                        targetFiles.forEach { file ->
                            val fileMetadata = WavFile(file).metadata
                            val ppm = project.patternMatcher
                            ppm.match(file)
                            db.addTake(
                                ppm.takeInfo!!,
                                file.name,
                                fileMetadata.modeSlug,
                                file.lastModified(),
                                0,
                                userId
                            )
                        }
                    } else {
                        Logger.w(this::javaClass.name, "Language $languageSlug doesn't exist")
                    }
                } else {
                    Logger.w(this::javaClass.name, "Project ${projectDir.name} exists")
                }
            }
        } catch (e: Exception) {
            Logger.e(this::javaClass.name, "Could not import project ${projectDir.name}", e)
        }
    }
}