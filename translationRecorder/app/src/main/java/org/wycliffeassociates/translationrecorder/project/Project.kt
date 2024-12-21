package org.wycliffeassociates.translationrecorder.project

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin.TYPE
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.Version
import java.io.IOException
import java.io.InputStream

/**
 * Created by sarabiaj on 5/10/2016.
 */
class Project(
    val targetLanguage: Language,
    val anthology: Anthology,
    val book: Book,
    val version: Version,
    val mode: Mode,
    val contributors: String? = null,
    var sourceLanguage: Language? = null,
    var sourceAudioPath: String? = null
) : Parcelable {

    private val fileName: FileName = FileName(targetLanguage, anthology, version, book)

    interface ProjectPluginLoader {
        fun loadChunkPlugin(anthology: Anthology, book: Book, type: TYPE): ChunkPlugin
        fun chunksInputStream(anthology: Anthology, book: Book): InputStream?
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Language::class.java.classLoader)!!,
        parcel.readParcelable(Anthology::class.java.classLoader)!!,
        parcel.readParcelable(Book::class.java.classLoader)!!,
        parcel.readParcelable(Version::class.java.classLoader)!!,
        parcel.readParcelable(Mode::class.java.classLoader)!!,
        parcel.readString(),
        parcel.readParcelable(Language::class.java.classLoader),
        parcel.readString()
    )

    val isOBS: Boolean get() = anthologySlug == "obs"

    val projectSlugs: ProjectSlugs
        get() = ProjectSlugs(
            targetLanguageSlug,
            versionSlug,
            bookNumber.toInt(),
            bookSlug
        )

    val targetLanguageSlug: String
        get() = targetLanguage.slug

    val sourceLanguageSlug: String
        get() = sourceLanguage?.slug ?: ""

    val anthologySlug: String
        get() = anthology.slug

    val bookSlug: String
        get() = book.slug

    val bookName: String
        get() = book.name

    val versionSlug: String
        get() = version.slug

    val modeSlug: String
        get() = mode.slug

    val modeType: TYPE
        get() = mode.type

    val modeName: String
        get() = mode.name

    val bookNumber: String
        get() = book.order.toString()

    val patternMatcher: ProjectPatternMatcher
        get() = ProjectPatternMatcher(anthology.regex, anthology.matchGroups)

    @Throws(IOException::class)
    fun getChunkPlugin(pluginLoader: ProjectPluginLoader): ChunkPlugin {
        return pluginLoader.loadChunkPlugin(anthology, book, modeType)
    }

    fun loadProjectIntoPreferences(
        db: IProjectDatabaseHelper,
        prefs: IPreferenceRepository
    ) {
        if (db.projectExists(this)) {
            val projectId: Int = db.getProjectId(this)
            prefs.setDefaultPref(Settings.KEY_RECENT_PROJECT_ID, projectId)
        }
    }

    @SuppressLint("DefaultLocale")
    fun getChapterFileName(chapter: Int): String {
        val chapterFileName: String = targetLanguage.slug +
                "_" + anthology.slug +
                "_" + version.slug +
                "_" + book.slug +
                "_c" + String.format("%02d", chapter) +
                ".wav"
        return chapterFileName
    }

    fun getFileName(chapter: Int, vararg verses: Int): String {
        return fileName.getFileName(chapter, *verses)
    }

    fun getLocalizedModeName(ctx: Context): String {
        val chunk: String = ctx.getString(R.string.chunk_title)
        val verse: String = ctx.getString(R.string.title_verse)

        return when (modeName) {
            Mode.CHUNK -> chunk
            Mode.VERSE -> verse
            else -> modeName
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(targetLanguage, flags)
        dest.writeParcelable(anthology, flags)
        dest.writeParcelable(book, flags)
        dest.writeParcelable(version, flags)
        dest.writeParcelable(mode, flags)
        dest.writeString(contributors)
        dest.writeParcelable(sourceLanguage, flags)
        dest.writeString(sourceAudioPath)
    }

    override fun equals(other: Any?): Boolean {
        if (other != null && other is Project) {
            if (targetLanguage.slug == other.targetLanguageSlug
                && book.slug == other.bookSlug
                && version.slug == other.versionSlug
                && mode.slug == other.modeSlug
            ) {
                return true
            }
        }
        return false
    }

    override fun hashCode(): Int {
        var result = targetLanguage.hashCode()
        result = 31 * result + anthology.hashCode()
        result = 31 * result + book.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + mode.hashCode()
        return result
    }

    companion object {
        const val PROJECT_EXTRA: String = "project_extra"
        const val SOURCE_LANGUAGE_EXTRA: String = "source_language_extra"
        const val SOURCE_LOCATION_EXTRA: String = "source_location_extra"

        fun getProjectFromPreferences(
            db: IProjectDatabaseHelper,
            prefs: IPreferenceRepository
        ): Project? {
            val projectId: Int = prefs.getDefaultPref(Settings.KEY_RECENT_PROJECT_ID, -1)
            val project: Project? = db.getProject(projectId)
            return project
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Project?> = object : Parcelable.Creator<Project?> {
            override fun createFromParcel(parcel: Parcel): Project {
                return Project(parcel)
            }
            override fun newArray(size: Int): Array<Project?> {
                return arrayOfNulls(size)
            }
        }
    }
}
