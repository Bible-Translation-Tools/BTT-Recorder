package org.wycliffeassociates.translationrecorder.project

import android.content.Context
import android.util.JsonReader
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.Version
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.Reader

/**
 * Created by sarabiaj on 3/20/2017.
 */
class ProjectPlugin(
    private val pluginDir: File,
    plugin: File,
    private val db: IProjectDatabaseHelper,
    private val context: Context
) {
    var resource: String? = null
    var slug: String? = null
    var name: String? = null
    var regex: String? = null
    var groups: String? = null
    var mask: String? = null
    var booksPath: String? = null
    var chunksPath: String? = null
    var jarPath: String? = null
    var className: String? = null
    var versionsPath: String? = null
    var sort: Int = 0

    //groups
    var language: Int = -1
    var version: Int = -1
    var bookNumber: Int = -1
    var book: Int = -1
    var chapter: Int = -1
    var startVerse: Int = -1

    var endVerse: Int = -1
    var take: Int = -1
    var modes: MutableList<Mode> = ArrayList()


    init {
        val reader: Reader = FileReader(plugin)
        init(reader)
    }

    @Throws(IOException::class)
    fun importProjectPlugin() {
        val bookReader: Reader = FileReader(
            File(pluginDir, "books/$booksPath")
        )
        val books = readBooks(JsonReader(bookReader))
        val versionReader: Reader = FileReader(
            File(pluginDir, "versions/$versionsPath")
        )
        val versions = readVersions(JsonReader(versionReader))

//        Reader chunksReader = new FileReader(new File(pluginDir, "Chunks/" + chunksPath));
//        readChunks(new JsonReader(chunksReader));
        importPluginToDatabase(
            books,
            versions,
            modes,
            db
        )
    }

    @Throws(IOException::class)
    private fun init(reader: Reader) {
        val jsonReader = JsonReader(reader)
        readPlugin(jsonReader)
        groups = createMatchGroups()
    }

    @Throws(IOException::class)
    private fun readBooks(jsonReader: JsonReader): List<Book> {
        val bookList: MutableList<Book> = ArrayList()
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            jsonReader.beginObject()
            var slug: String? = null
            var num = 0
            var anth: String? = null
            var name: String? = null
            while (jsonReader.hasNext()) {
                val key = jsonReader.nextName()
                when (key) {
                    "slug" -> slug = jsonReader.nextString()
                    "num" -> num = jsonReader.nextInt()
                    "anth" -> anth = jsonReader.nextString()
                    "name" -> name = jsonReader.nextString()
                }
            }
            val localizedName = Book.getLocalizedName(context, slug, name, anth)
            bookList.add(Book(slug, localizedName, anth, num))
            jsonReader.endObject()
        }
        jsonReader.endArray()
        return bookList
    }

    @Throws(IOException::class)
    private fun readVersions(jsonReader: JsonReader): List<Version> {
        val versionList: MutableList<Version> = ArrayList()
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            jsonReader.beginObject()
            var slug: String? = null
            var name: String? = null
            while (jsonReader.hasNext()) {
                val key = jsonReader.nextName()
                if (key == "slug") {
                    slug = jsonReader.nextString()
                } else if (key == "name") {
                    name = jsonReader.nextString()
                }
            }
            versionList.add(Version(slug, name))
            jsonReader.endObject()
        }
        jsonReader.endArray()
        return versionList
    }

    private fun importPluginToDatabase(
        books: List<Book>,
        versions: List<Version>,
        modes: List<Mode>,
        db: IProjectDatabaseHelper
    ) {
        db.addAnthology(slug, name, resource, sort, regex, groups, mask, jarPath, className)
        db.addBooks(books)
        db.addVersions(versions)
        db.addModes(modes, slug!!)
        db.addVersionRelationships(slug!!, versions)
        //db.addModeRelationships(slug, modes);
    }

    private fun createMatchGroups(): String {
        val groups = StringBuilder()
        groups.append(language)
        groups.append(" ")
        groups.append(version)
        groups.append(" ")
        groups.append(bookNumber)
        groups.append(" ")
        groups.append(book)
        groups.append(" ")
        groups.append(chapter)
        groups.append(" ")
        groups.append(startVerse)
        groups.append(" ")
        groups.append(endVerse)
        groups.append(" ")
        groups.append(take)
        return groups.toString()
    }

    @Throws(IOException::class)
    private fun readPlugin(jsonReader: JsonReader) {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            when (key) {
                "resource" -> resource = jsonReader.nextString()
                "books" -> booksPath = jsonReader.nextString()
                "chunks" -> chunksPath = jsonReader.nextString()
                "versions" -> versionsPath = jsonReader.nextString()
                "anthology" -> readAnthologySection(jsonReader)
                "modes" -> readModesSection(jsonReader)
                "chunk_plugin" -> readChunkSection(jsonReader)
                "sort" -> sort = jsonReader.nextInt()
            }
        }
        jsonReader.endObject()
    }

    @Throws(IOException::class)
    private fun readAnthologySection(jsonReader: JsonReader) {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            when (key) {
                "slug" -> slug = jsonReader.nextString()
                "name" -> name = jsonReader.nextString()
                "file_conv" -> mask = jsonReader.nextString()
                "parser" -> readParserSection(jsonReader)
            }
        }
        jsonReader.endObject()
    }

    @Throws(IOException::class)
    private fun readParserSection(jsonReader: JsonReader) {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            if (key == "regex") {
                regex = jsonReader.nextString()
            } else if (key == "groups") {
                readGroupsSection(jsonReader)
            }
        }
        jsonReader.endObject()
    }

    @Throws(IOException::class)
    private fun readGroupsSection(jsonReader: JsonReader) {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            when (key) {
                "language" -> language = jsonReader.nextInt()
                "version" -> version = jsonReader.nextInt()
                "book_number" -> bookNumber = jsonReader.nextInt()
                "book" -> book = jsonReader.nextInt()
                "chapter" -> chapter = jsonReader.nextInt()
                "start_verse" -> startVerse = jsonReader.nextInt()
                "end_verse" -> endVerse = jsonReader.nextInt()
                "take" -> take = jsonReader.nextInt()
            }
        }
        jsonReader.endObject()
    }

    @Throws(IOException::class)
    fun readModesSection(jsonReader: JsonReader) {
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            jsonReader.beginObject()
            var name: String? = null
            var type: String? = null
            while (jsonReader.hasNext()) {
                val key = jsonReader.nextName()
                if (key == "name") {
                    name = jsonReader.nextString()
                } else if (key == "type") {
                    type = jsonReader.nextString()
                }
            }
            jsonReader.endObject()
            modes.add(Mode(name, name, type))
        }
        jsonReader.endArray()
    }

    @Throws(IOException::class)
    fun readChunkSection(jsonReader: JsonReader) {
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val key = jsonReader.nextName()
            if (key == "jar") {
                jarPath = jsonReader.nextString()
            } else if (key == "class") {
                className = jsonReader.nextString()
            }
        }
        jsonReader.endObject()
    }
}
