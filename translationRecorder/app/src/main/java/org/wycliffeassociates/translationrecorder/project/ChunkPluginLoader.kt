package org.wycliffeassociates.translationrecorder.project

import com.door43.tools.reporting.Logger
import dalvik.system.DexClassLoader
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin.TYPE
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project.ProjectPluginLoader
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Constructor

/**
 * Created by sarabiaj on 9/26/2017.
 */
class ChunkPluginLoader(
    private val directoryProvider: IDirectoryProvider,
    private val assetsProvider: AssetsProvider
) : ProjectPluginLoader {

    override fun loadChunkPlugin(anthology: Anthology, book: Book, type: TYPE): ChunkPlugin {
        var chunks: ChunkPlugin? = null
        val jarsDir = File(directoryProvider.pluginsDir, "jars")
        jarsDir.mkdirs()
        val jarFile = File(jarsDir, anthology.pluginFilename)
        val codeDir = File(directoryProvider.codeCacheDir, "dex/")
        codeDir.mkdirs()
        val optimizedDexOutputPath = File(codeDir, "biblechunkdex")
        try {
            optimizedDexOutputPath.createNewFile()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val classLoader = DexClassLoader(
            jarFile.absolutePath,
            optimizedDexOutputPath.absolutePath,
            null,
            javaClass.classLoader
        )
        try {
            val plugin = classLoader.loadClass(anthology.pluginClassName)
            val ctr = plugin
                .asSubclass(ChunkPlugin::class.java)
                .getConstructor(type.javaClass) as Constructor<ChunkPlugin>
            chunks = ctr.newInstance(type)
        } catch (e: Exception) {
            Logger.e(
                this.toString(),
                "Error loading plugin from jar for anthology: " + anthology.slug,
                e
            )
            e.printStackTrace()
        }
        chunksInputStream(anthology, book)?.use {
            chunks!!.parseChunks(it)
        }
        return chunks!!
    }

    override fun chunksInputStream(anthology: Anthology, book: Book): InputStream? {
        try {
            return assetsProvider.open("chunks/" + anthology.slug + "/" + book.slug + "/chunks.json")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}
