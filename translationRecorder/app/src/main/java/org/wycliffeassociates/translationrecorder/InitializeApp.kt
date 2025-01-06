package org.wycliffeassociates.translationrecorder

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.door43.tools.reporting.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ParseJSON
import org.wycliffeassociates.translationrecorder.project.ProjectPlugin
import org.wycliffeassociates.translationrecorder.project.components.User
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class InitializeApp @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryProvider: IDirectoryProvider,
    private val assetsProvider: AssetsProvider,
    private val db: IProjectDatabaseHelper
) {

    fun run() {
        initializePlugins()
        initializeDatabase()
    }

    private fun initializeDatabase() {
        val parse = ParseJSON(assetsProvider, context)
        try {
            //Book[] books = parse.pullBooks();
            val languages = parse.pullLangNames()
            //db.addBooks(books);
            db.addLanguages(languages)
            println("Proof: en is " + db.getLanguageName("en"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        importProfiles()
        deleteDanglingProfiles()
    }

    @Throws(IOException::class)
    private fun initializePlugins() {
        val assetPlugins = assetsProvider.list("plugins/anthologies")
        val pluginsDir = directoryProvider.pluginsDir
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
        }
        val anthologiesDir = directoryProvider.anthologiesDir
        if (!anthologiesDir.exists()) {
            anthologiesDir.mkdirs()
        }
        copyPluginsFromAssets(pluginsDir, anthologiesDir, assetPlugins)

        val plugins = anthologiesDir.list()
        if (plugins != null && plugins.isNotEmpty()) {
            for (s in plugins) {
                importPlugin(pluginsDir, anthologiesDir, s)
            }
        }
    }

    @Throws(IOException::class)
    private fun importPlugin(
        pluginsDir: File,
        anthologiesDir: File,
        plugin: String
    ) {
        val pluginPath = File(anthologiesDir, plugin)
        val projectPlugin = ProjectPlugin(pluginsDir, pluginPath, db, context)
        copyPluginContentFromAssets(pluginsDir, "anthologies", plugin)
        copyPluginContentFromAssets(pluginsDir, "books", projectPlugin.booksPath)
        copyPluginContentFromAssets(pluginsDir, "versions", projectPlugin.versionsPath)
        copyPluginContentFromAssets(pluginsDir, "jars", projectPlugin.jarPath)

        //copyPluginContentFromAssets(am, pluginPath, "Chunks", projectPlugin.getChunksPath());
        projectPlugin.importProjectPlugin()
    }

    @Throws(IOException::class)
    private fun copyPluginsFromAssets(
        pluginsDir: File,
        anthologiesDir: File,
        plugins: Array<String>?
    ) {
        if (!plugins.isNullOrEmpty()) {
            for (plugin in plugins) {
                copyPluginContentFromAssets(pluginsDir, "anthologies", plugin)
                val pluginPath = File(anthologiesDir, plugin)
                val projectPlugin = ProjectPlugin(pluginsDir, pluginPath, db, context)
                copyPluginContentFromAssets(pluginsDir, "books", projectPlugin.booksPath)
                copyPluginContentFromAssets(pluginsDir, "versions", projectPlugin.versionsPath)
                copyPluginContentFromAssets(pluginsDir, "jars", projectPlugin.jarPath)
            }
        }
    }

    private fun copyPluginContentFromAssets(
        outputRoot: File,
        prefix: String,
        pluginName: String?
    ) {
        if (pluginName == null) {
            Logger.e(this.toString(), "Plugin name is invalid.")
            return
        }

        val outputDir = File(outputRoot, prefix)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        try {
            val plugin = File(outputDir, pluginName)
            setWritable(plugin, true)

            assetsProvider.open("plugins/$prefix/$pluginName").use { inputStream ->
                FileOutputStream(plugin).use { outputStream ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while ((inputStream.read(buf).also { len = it }) > 0) {
                        outputStream.write(buf, 0, len)
                    }
                }
            }

            // Starting from Android 14 (api 34) it is required to mark files that
            // load code dynamically to bew readonly
            // See https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
            setWritable(plugin, false)
        } catch (e: IOException) {
            Logger.e(this.toString(), "Exception copying $pluginName from assets", e)
        }
    }

    private fun importProfiles() {
        val profilesDir = directoryProvider.profilesDir
        if (!profilesDir.exists()) {
            profilesDir.mkdirs()
        }
        val profileFiles = profilesDir.listFiles()
        if (profileFiles != null) {
            for (profile in profileFiles) {
                val hash = getHash(profile)
                var mimeType: String? = null

                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(profile.absolutePath)
                    mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                } catch (e: Exception) {
                    Log.i("PROFILE", "File is not a media file")
                }

                if (hash != null && mimeType != null && mimeType == "audio/mp4") {
                    db.addUser(User(profile, hash))
                }
            }
        }
    }

    private fun deleteDanglingProfiles() {
        val profiles = db.allUsers
        for (profile in profiles) {
            val file = profile.audio
            if (!file!!.exists()) {
                db.deleteUser(profile.hash!!)
            }
        }
    }

    private fun getHash(file: File): String? {
        return try {
            String(Hex.encodeHex(DigestUtils.md5(FileInputStream(file))))
        } catch (e: IOException) {
            null
        }
    }

    private fun setWritable(file: File, writable: Boolean) {
        file.setWritable(writable)
    }
}