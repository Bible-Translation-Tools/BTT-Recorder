package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.project.ParseJSON
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.utilities.Task
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by sarabiaj on 12/14/2016.
 */
class ResyncLanguageNamesTask private constructor(
    taskTag: Int,
    private val mCtx: Context,
    private val db: IProjectDatabaseHelper,
    private val assetsProvider: AssetsProvider,
    private val context: Context
) : Task(taskTag) {
    private var isLocal: Boolean = false
    private var localFile: Uri? = null
    private var remoteUrl: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    constructor(
        taskTag: Int,
        ctx: Context,
        db: IProjectDatabaseHelper,
        assetsProvider: AssetsProvider,
        context: Context,
        url: String?
    ) : this(taskTag, ctx, db, assetsProvider, context) {
        this.remoteUrl = url
    }

    constructor(
        taskTag: Int,
        ctx: Context,
        db: IProjectDatabaseHelper,
        assetsProvider: AssetsProvider,
        context: Context,
        uri: Uri?
    ) : this(
        taskTag,
        ctx,
        db,
        assetsProvider,
        context
    ) {
        this.isLocal = true
        this.localFile = uri
    }

    override fun run() {
        val json = if (isLocal) loadJsonFromFile() else loadJsonFromUrl()
        try {
            val jsonObject = JSONArray(json)
            val parseJSON = ParseJSON(assetsProvider, context)
            val languages: List<Language> = parseJSON.pullLangNames(jsonObject)
            db.addLanguages(languages)
            onTaskCompleteDelegator()
            handler.post {
                Toast.makeText(
                    mCtx,
                    mCtx.getString(R.string.languages_updated),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            onTaskErrorDelegator()
            handler.post {
                Toast.makeText(
                    mCtx,
                    mCtx.getString(R.string.invalid_json),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadJsonFromUrl(): String {
        try {
            val url = URL(this.remoteUrl)
            var urlConnection: HttpURLConnection? = null

            try {
                urlConnection = url.openConnection() as HttpURLConnection
                val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                val reader = BufferedReader(InputStreamReader(`in`))
                val json = StringBuilder()
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    json.append(line)
                }
                reader.close()
                return json.toString()
            } catch (e: IOException) {
                e.printStackTrace()
                handler.post { Toast.makeText(mCtx, e.message, Toast.LENGTH_SHORT).show() }
                return ""
            } finally {
                urlConnection!!.disconnect()
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return ""
        }
    }

    private fun loadJsonFromFile(): String {
        try {
            mCtx.contentResolver.openInputStream(localFile!!).use { `is` ->
                val reader = BufferedReader(InputStreamReader(`is`))
                val json = StringBuilder()
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    json.append(line)
                }
                reader.close()
                return json.toString()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            handler.post { Toast.makeText(mCtx, e.message, Toast.LENGTH_SHORT).show() }
            return ""
        } catch (e: IOException) {
            e.printStackTrace()
            onTaskErrorDelegator()
            return ""
        }
    }
}
