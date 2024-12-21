package org.wycliffeassociates.translationrecorder.project

import android.util.Pair
import org.json.JSONArray
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import java.io.IOException

/**
 * Created by Abi on 7/29/2015.
 */
class ParseJSON(private val assetsProvider: AssetsProvider) {

    private var mBooks: MutableList<Book> = arrayListOf()

    private var _booksMap: HashMap<String, Book> = hashMapOf()
    val booksMap: HashMap<String, Book>
        get() {
            try {
                pullBookInfo()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return _booksMap
        }

    private var _booksList: MutableList<String> = arrayListOf()
    val booksList: List<String>
        get() {
            try {
                pullBookInfo()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return _booksList
        }

    private var _languages: MutableList<String> = arrayListOf()
    val languages: List<String>
        get() {
            try {
                pullLangNames()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return _languages
        }

    private fun loadJSONFromAsset(filename: String): String? {
        val json: String?
        try {
            val `is` = assetsProvider.open(filename)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            json = String(buffer, charset("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    @Throws(JSONException::class)
    private fun pullBookInfo() {
        val books = ArrayList<Book>()
        val json = loadJSONFromAsset("note_books.json")
        val booksJSON = JSONArray(json)
        for (i in 0 until booksJSON.length()) {
            val bookObj = booksJSON.getJSONObject(i)
            val name = bookObj.getString("name")
            val slug = bookObj.getString("slug")
            val anthology = bookObj.getString("anth")
            val order = bookObj.getInt("num")
            val book = Book(slug, name, anthology, order)
            books.add(book)
        }
        val sortedBooks = books.sortedWith { lhs, rhs ->
            when {
                lhs.order > rhs.order -> 1
                lhs.order < rhs.order -> -1
                else -> 0
            }
        }
        _booksMap = HashMap()
        for (b in sortedBooks) {
            _booksMap[b.slug] = b
        }
        for ((i, b) in sortedBooks.withIndex()) {
            _booksList[i] = b.slug + " - " + b.name
        }
        mBooks.addAll(sortedBooks)
    }

    fun getNumChapters(bookCode: String): Int {
        try {
            val json = loadJSONFromAsset("chunks/$bookCode/en/udb/chunks.json")
            val arrayOfChunks = JSONArray(json)
            var numChapters = 1
            //loop through the all the chunks
            for (i in 0 until arrayOfChunks.length()) {
                val jsonChunk = arrayOfChunks.getJSONObject(i)
                val id = jsonChunk.getString("id")
                val chapter = id.substring(0, id.lastIndexOf('-')).toInt()
                if (chapter > numChapters) {
                    numChapters = chapter
                }
            }
            return numChapters
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return 1
    }

    /**
     * Generates a 2d ArrayList of chunks indexed by chapters
     * @param bookCode the code of the book to pull chunk info from
     * @return a 2d ArrayList of chunks
     * @throws JSONException
     */
    fun getChunks(bookCode: String, source: String): ArrayList<ArrayList<Pair<Int, Int>>>? {
        var source = source
        try {
            //FIXME: no folder for "reg"
            if (source.compareTo("reg") == 0) {
                source = "ulb"
            }
            val json = loadJSONFromAsset("chunks/$bookCode/en/$source/chunks.json")
            val arrayOfChunks = JSONArray(json)
            val chunksInBook = ArrayList<ArrayList<Pair<Int, Int>>>()
            //loop through the all the chunks
            for (i in 0 until arrayOfChunks.length()) {
                val jsonChunk = arrayOfChunks.getJSONObject(i)
                val id = jsonChunk.getString("id")
                val chapter = id.substring(0, id.lastIndexOf('-')).toInt()
                //if a chapter hasn't been appended yet, append it
                if (chunksInBook.size >= chapter - 1) {
                    chunksInBook.add(ArrayList())
                }
                //add the chunk to that chapter
                chunksInBook[chapter - 1].add(
                    Pair(
                        jsonChunk.getInt("firstvs"),
                        jsonChunk.getInt("lastvs")
                    )
                )
            }
            return chunksInBook
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Generates a 2d ArrayList of verses indexed by chapters
     * @param bookCode the code of the book to pull verse info from
     * @return a 2d ArrayList of verses
     * @throws JSONException
     */
    fun getVerses(bookCode: String, source: String): ArrayList<ArrayList<Pair<Int, Int>>>? {
        var source = source
        try {
            //FIXME: no folder for "reg"
            if (source.compareTo("reg") == 0) {
                source = "ulb"
            }
            val json = loadJSONFromAsset("chunks/$bookCode/en/$source/chunks.json")
            val arrayOfVerses = JSONArray(json)
            val versesInBook = ArrayList<ArrayList<Pair<Int, Int>>>()
            var lastChapter = 0
            //loop through the all the verses
            for (i in 0 until arrayOfVerses.length()) {
                val jsonChunk = arrayOfVerses.getJSONObject(i)
                val id = jsonChunk.getString("id")
                val chapter = id.substring(0, id.lastIndexOf('-')).toInt()
                //if a chapter hasn't been appended yet, append it
                if (versesInBook.size >= chapter - 1) {
                    versesInBook.add(ArrayList())
                }
                //add the chunk to that chapter
                versesInBook[chapter - 1].add(
                    Pair(
                        jsonChunk.getInt("lastvs"),
                        jsonChunk.getInt("lastvs")
                    )
                )
                if (chapter > lastChapter) {
                    lastChapter = chapter
                }
            }

            val verses = ArrayList<ArrayList<Pair<Int, Int>>>()
            for (idx in 0 until lastChapter) {
                if (idx >= verses.size) {
                    verses.add(ArrayList())
                }
                val numVerses = versesInBook[idx][versesInBook[idx].size - 1].first
                for (i in 1..numVerses) {
                    verses[idx].add(Pair(i, i))
                }
            }
            return verses
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun pullBooks(): List<Book> {
        booksMap
        return mBooks
    }

    @Throws(JSONException::class)
    fun pullLangNames(): List<Language> {
        val json = loadJSONFromAsset("langnames.json")
        val langArray = JSONArray(json)
        return pullLangNames(langArray)
    }

    @Throws(JSONException::class)
    fun pullLangNames(langArray: JSONArray): List<Language> {
        val languageList = ArrayList<Language>()
        for (i in 0 until langArray.length()) {
            val langObj = langArray.getJSONObject(i)
            val ln = Language(langObj.getString("lc"), langObj.getString("ln"))
            languageList.add(ln)
        }
        for (l in languageList) {
            _languages.add(l.slug + " - " + l.name)
        }
        return languageList
    }

    companion object {
        fun getLanguages(assetsProvider: AssetsProvider): List<Language> {
            val parse = ParseJSON(assetsProvider)
            return try {
                parse.pullLangNames()
            } catch (e: JSONException) {
                e.printStackTrace()
                arrayListOf()
            }
        }

        fun getBooks(assetsProvider: AssetsProvider, testament: String): List<Book> {
            val parse = ParseJSON(assetsProvider)
            val books = parse.pullBooks()
            return books.filter {
                when (testament) {
                    "nt" -> it.order > 40
                    "ot" -> it.order < 40
                    else -> true
                }
            }
        }
    }
}
