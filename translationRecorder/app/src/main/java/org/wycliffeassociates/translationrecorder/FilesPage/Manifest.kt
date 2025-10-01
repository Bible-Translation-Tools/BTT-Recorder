package org.wycliffeassociates.translationrecorder.FilesPage

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import org.apache.commons.io.FileUtils
import org.wycliffeassociates.translationrecorder.FilesPage.Export.SimpleProgressCallback
import org.wycliffeassociates.translationrecorder.FilesPage.Export.TranslationExchangeDiff
import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter
import org.wycliffeassociates.translationrecorder.chunkplugin.Chunk
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import org.wycliffeassociates.translationrecorder.project.components.User
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Created by sarabiaj on 11/15/2017.
 */
class Manifest(
    private val project: Project,
    private val projectDirectory: File,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider,
    private val assetsProvider: AssetsProvider
) {
    private val takes: MutableList<File> = ArrayList()
    private val users: MutableMap<Int, User> = HashMap()
    private val projectFiles: Collection<File> = FileUtils.listFiles(
        projectDirectory,
        arrayOf("wav"),
        true
    )
    private var progressCallback: SimpleProgressCallback? = null
    private var chunksWritten: Int = 0
    private var totalChunks: Int = 0

    @Throws(IOException::class)
    fun createManifestFile(): File {
        val plugin = project.getChunkPlugin(ChunkPluginLoader(directoryProvider, assetsProvider))
        val chapters = plugin.chapters
        totalChunks = getTotalChunks(chapters)
        val gson = Gson()
        val output = File(projectDirectory, "manifest.json")
        gson.newJsonWriter(FileWriter(output)).use { jw ->
            jw.beginObject()
            writeTargetLanguage(jw)
            writeSourceLanguage(jw)
            writeBook(jw)
            writeVersion(jw)
            writeAnthology(jw)
            writeMode(jw)
            writeChapters(chapters, jw)
            writeUsers(jw)
            jw.endObject()
        }
        return output
    }

    val takesInManifest: List<File>
        get() = takes

    fun setProgressCallback(progressCallback: SimpleProgressCallback?) {
        this.progressCallback = progressCallback
    }

    @Throws(IOException::class)
    private fun writeTargetLanguage(jw: JsonWriter) {
        jw.name("language")
        jw.beginObject()
        jw.name("slug").value(project.targetLanguageSlug)
        jw.name("name").value(project.targetLanguage.name)
        jw.endObject()
    }

    @Throws(IOException::class)
    private fun writeSourceLanguage(jw: JsonWriter) {
        if (project.sourceLanguage != null) {
            jw.name("source_language")
            jw.beginObject()
            jw.name("slug").value(project.sourceLanguageSlug)
            jw.name("name").value(project.sourceLanguage!!.name)
            jw.endObject()
        }
    }

    @Throws(IOException::class)
    private fun writeBook(jw: JsonWriter) {
        jw.name("book")
        jw.beginObject()
        jw.name("name").value(project.bookName)
        jw.name("slug").value(project.bookSlug)
        jw.name("number").value(project.bookNumber)
        jw.endObject()
    }

    @Throws(IOException::class)
    private fun writeMode(jw: JsonWriter) {
        jw.name("mode")
        jw.beginObject()
        jw.name("name").value(project.modeName)
        jw.name("slug").value(project.modeSlug)
        jw.name("type").value(project.modeType.toString())
        jw.endObject()
    }

    @Throws(IOException::class)
    private fun writeVersion(jw: JsonWriter) {
        jw.name("version")
        jw.beginObject()
        jw.name("slug").value(project.versionSlug)
        jw.name("name").value(project.version.name)
        jw.endObject()
    }

    @Throws(IOException::class)
    private fun writeAnthology(jw: JsonWriter) {
        jw.name("anthology")
        jw.beginObject()
        jw.name("slug").value(project.anthologySlug)
        jw.name("name").value(project.anthology.name)
        jw.endObject()
    }

    @Throws(IOException::class)
    private fun writeChapters(chapters: List<Chapter>, jw: JsonWriter) {
        jw.name("manifest")
        jw.beginArray()
        for (chapter in chapters) {
            val number = chapter.number
            var checkingLevel = 0
            if (db.chapterExists(project, number)) {
                checkingLevel = db.getChapterCheckingLevel(project, number)
            }
            jw.beginObject()
            jw.name("chapter").value(number.toLong())
            jw.name("checking_level").value(checkingLevel.toLong())
            writeChunks(chapter.chunks, number, jw)
            jw.endObject()
        }
        jw.endArray()
    }

    @Throws(IOException::class)
    private fun writeChunks(
        chunks: List<Chunk>,
        chapter: Int,
        jw: JsonWriter
    ) {
        jw.name("chunks")
        jw.beginArray()
        for (chunk in chunks) {
            val startv = chunk.startVerse
            val endv = chunk.endVerse
            jw.beginObject()
            jw.name("startv").value(startv.toLong())
            jw.name("endv").value(endv.toLong())
            writeTakes(chapter, startv, endv, jw)
            jw.endObject()

            chunksWritten++

            progressCallback?.setUploadProgress(
                TranslationExchangeDiff.DIFF_ID,
                manifestProgress
            )
        }
        jw.endArray()
    }

    @Throws(IOException::class)
    private fun writeTakes(
        chapter: Int,
        startv: Int,
        endv: Int,
        jw: JsonWriter
    ) {
        val takes = getTakesList(chapter, startv, endv)
        jw.name("takes")
        jw.beginArray()
        val i = takes.iterator()
        while (i.hasNext()) {
            val take = i.next()
            val ppm = project.patternMatcher
            ppm.match(take)
            if (ppm.matched()) {
                jw.beginObject()
                jw.name("name").value(take.name)
                ppm.takeInfo?.let { info ->
                    val rating = db.getTakeRating(info)
                    val user = db.getTakeUser(info)?.apply {
                        if (!users.containsKey(id)) {
                            users[id] = this
                        }
                    }
                    jw.name("rating").value(rating.toLong())
                    jw.name("user_id").value(user?.id?.toLong())
                }
                jw.endObject()
            } else {
                i.remove()
            }
        }
        jw.endArray()
        this.takes.addAll(takes)
    }

    @Throws(IOException::class)
    private fun writeUsers(jw: JsonWriter) {
        jw.name("users")
        jw.beginArray()
        for (user in users.values) {
            jw.beginObject()
            jw.name("name_audio").value(user.audio!!.name)
            jw.name("icon_hash").value(user.hash)
            jw.name("id").value(user.id.toLong())
            jw.endObject()
        }
        jw.endArray()
    }

    private fun getTotalChunks(chapters: List<Chapter>): Int {
        var total = 0
        for (chapter in chapters) {
            total += chapter.chunks.size
        }
        return total
    }

    private val manifestProgress: Int
        get() {
            if (totalChunks <= 0) return 0
            return Math.round(chunksWritten.toFloat() / totalChunks.toFloat() * 100)
        }

    private fun getTakesList(chapter: Int, startv: Int, endv: Int): MutableList<File> {
        var ppm: ProjectPatternMatcher
        //Get only the files of the appropriate unit
        val resultFiles: MutableList<File> = ArrayList()
        for (file in projectFiles) {
            ppm = project.patternMatcher
            ppm.match(file)
            ppm.takeInfo?.let { takeInfo ->
                if (takeInfo.chapter == chapter && takeInfo.startVerse == startv && takeInfo.endVerse == endv) {
                    resultFiles.add(file)
                }
            }
        }
        return resultFiles
    }

    val userFiles: List<File>
        get() {
            val userAudioFiles: MutableList<File> = arrayListOf()
            for (user in users.values) {
                userAudioFiles.add(user.audio!!)
            }
            return userAudioFiles
        }
}
