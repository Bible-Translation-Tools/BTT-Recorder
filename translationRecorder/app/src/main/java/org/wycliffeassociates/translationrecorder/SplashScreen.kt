package org.wycliffeassociates.translationrecorder

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivitySplashBinding
import org.wycliffeassociates.translationrecorder.login.UserActivity
import org.wycliffeassociates.translationrecorder.permissions.PermissionActivity
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.ParseJSON
import org.wycliffeassociates.translationrecorder.project.ProjectPlugin
import org.wycliffeassociates.translationrecorder.project.components.User
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 5/5/2016.
 */
@AndroidEntryPoint
class SplashScreen : PermissionActivity() {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.max = 100
        binding.progressBar.isIndeterminate = true
        binding.progressBar.minimumHeight = 8
    }

    override fun onPermissionsAccepted() {
        val initDb = Thread {
            try {
                initializePlugins()
                initDatabase()
                val profile = prefs.getDefaultPref(Settings.KEY_PROFILE, -1)
                if (profile == -1) {
                    val intent = Intent(this@SplashScreen, UserActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    startActivity(Intent(this@SplashScreen, MainMenu::class.java))
                    finish()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        initDb.start()
    }

    private fun initDatabase() {
        val parse = ParseJSON(assetsProvider)
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
        val projectPlugin = ProjectPlugin(pluginsDir, pluginPath, db)
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
                val projectPlugin = ProjectPlugin(pluginsDir, pluginPath, db)
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
            assetsProvider.open("plugins/$prefix/$pluginName").use { inputStream ->
                FileOutputStream(File(outputDir, pluginName)).use { outputStream ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while ((inputStream.read(buf).also { len = it }) > 0) {
                        outputStream.write(buf, 0, len)
                    }
                }
            }
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
            if (!file.exists()) {
                db.deleteUser(profile.hash)
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

    companion object {
        private const val SPLASH_TIME_OUT = 3000
    }
}
