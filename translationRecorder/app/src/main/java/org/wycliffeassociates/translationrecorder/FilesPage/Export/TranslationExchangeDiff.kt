package org.wycliffeassociates.translationrecorder.FilesPage.Export

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.wycliffeassociates.translationrecorder.FilesPage.Manifest
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by sarabiaj on 1/24/2018.
 */
//Arraylist explicitly specified because of zip4j dependency
class TranslationExchangeDiff(
    private val project: Project,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider,
    private val assetsProvider: AssetsProvider
) {
    val diff: MutableList<File> = arrayListOf()

    private fun constructProjectQueryParameters(project: Project): String {
        return String.format(
            "lang=%s&book=%s&anth=%s&version=%s",
            project.targetLanguageSlug,
            project.bookSlug,
            project.anthologySlug,
            project.versionSlug
        )
    }

    fun getUploadedFilesList(project: Project): Map<String, String> {
        try {
            val query = constructProjectQueryParameters(project)
            val url = URL("http://opentranslationtools.org/api/exclude_files/?$query")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != 200) {
                throw RuntimeException("Failed : HTTP error code : ${conn.responseCode}")
            }
            val br = BufferedReader(
                InputStreamReader(conn.inputStream)
            )
            var output: String?
            val builder = StringBuilder()
            while ((br.readLine().also { output = it }) != null) {
                builder.append(output)
            }
            return parseJsonOutput(builder.toString())
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return HashMap()
    }

    @Throws(IOException::class)
    private fun stageNewFiles(existingFiles: Map<String, String>): ArrayList<File> {
        //get local takes for that project
        val filesInProject = ArrayList(
            FileUtils.listFiles(
                ProjectFileUtils.getProjectDirectory(project, directoryProvider),
                arrayOf("wav"),
                true
            )
        )
        val iterator = filesInProject.iterator()
        val ppm = project.patternMatcher
        while (iterator.hasNext()) {
            val f = iterator.next()
            //remove files already in tE, or files that don't match the file convention
            if (!ppm.match(f.name)) {
                iterator.remove()
            } else if (existingFiles.containsKey(f.name)) {
                //compute the md5 hash and convert to string
                val hash = String(Hex.encodeHex(DigestUtils.md5(FileInputStream(f))))
                //compare hash to hash received from tE
                if (hash == existingFiles[f.name]) {
                    iterator.remove()
                } else {
                    println(f.name)
                    println(hash)
                    println(existingFiles[f.name])
                }
            }
        }
        return filesInProject
    }

    //gets the map of filenames to their md5 hashes
    private fun parseJsonOutput(json: String): Map<String, String> {
        val map = HashMap<String, String>()
        val ja = JsonParser.parseString(json).asJsonArray
        val iterator: Iterator<JsonElement> = ja.iterator()
        while (iterator.hasNext()) {
            val jo = iterator.next().asJsonObject
            val file = jo["name"].asString
            val hash = jo["md5hash"].asString
            map[file] = hash
        }
        return map
    }

    fun computeDiff(progressCallback: SimpleProgressCallback) {
        val diff = Thread {
            try {
                progressCallback.onStart(DIFF_ID)
                diff.clear()
                diff.addAll(stageNewFiles(getUploadedFilesList(project)))
                val manifest = Manifest(
                    project,
                    ProjectFileUtils.getProjectDirectory(project, directoryProvider),
                    db,
                    directoryProvider,
                    assetsProvider
                )
                manifest.setProgressCallback(progressCallback)
                val mani = manifest.createManifestFile()
                diff.add(mani)
                val userFiles = manifest.userFiles
                for (file in userFiles) {
                    diff.add(file)
                }
                progressCallback.onComplete(DIFF_ID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        diff.start()
    }

    companion object {
        var DIFF_ID: Int = 1
    }
}
