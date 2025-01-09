package org.wycliffeassociates.translationrecorder.recordingapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChunkPluginLoaderTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        directoryProvider.clearCache()
    }

    @Test
    fun chunkPluginLoaderOldTestamentMark() {
        val anthology = Anthology(
            "ot",
            "Old Testament",
            "resource",
            0,
            "regex",
            "groups",
            "mask",
            "biblechunk.jar",
            "org.wycliffeassociates.translationrecorder.biblechunk.BibleChunkPlugin"
        )
        val book = Book("gen", "Genesis", "ot", 1)

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = pluginLoader.loadChunkPlugin(anthology, book, ChunkPlugin.TYPE.SINGLE)

        assertEquals(50, plugin.chapters.size)

        plugin.initialize(42, 7)

        assertEquals(42, plugin.chapter)
        assertEquals("7", plugin.chunkName)
    }

    @Test
    fun chunkPluginLoaderNewTestamentMark() {
        val anthology = Anthology(
            "nt",
            "New Testament",
            "resource",
            1,
            "regex",
            "groups",
            "mask",
            "biblechunk.jar",
            "org.wycliffeassociates.translationrecorder.biblechunk.BibleChunkPlugin"
        )
        val book = Book("mrk", "Mark", "nt", 42)

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = pluginLoader.loadChunkPlugin(anthology, book, ChunkPlugin.TYPE.SINGLE)

        assertEquals(16, plugin.chapters.size)

        plugin.initialize(16, 5)

        assertEquals(16, plugin.chapter)
        assertEquals("5", plugin.chunkName)
    }

    @Test
    fun chunkPluginLoaderNewTestamentMarkChunkMode() {
        val anthology = Anthology(
            "nt",
            "New Testament",
            "resource",
            1,
            "regex",
            "groups",
            "mask",
            "biblechunk.jar",
            "org.wycliffeassociates.translationrecorder.biblechunk.BibleChunkPlugin"
        )
        val book = Book("mrk", "Mark", "nt", 42)

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = pluginLoader.loadChunkPlugin(anthology, book, ChunkPlugin.TYPE.MULTI)

        assertEquals(16, plugin.chapters.size)

        plugin.initialize(16, 5)

        assertEquals(16, plugin.chapter)
        assertEquals("5-7", plugin.chunkName)
    }

    @Test
    fun chunkPluginLoaderObs() {
        val anthology = Anthology(
            "obs",
            "OBS",
            "resource",
            3,
            "regex",
            "groups",
            "mask",
            "obschunk.jar",
            "org.wycliffeassociates.translationrecorder.obschunk.ObsChunkPlugin"
        )
        val book = Book("33", "Book name", "obs", 33)

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = pluginLoader.loadChunkPlugin(anthology, book, ChunkPlugin.TYPE.SINGLE)

        assertEquals(1, plugin.chapters.size)

        plugin.initialize(1, 5)

        assertEquals(1, plugin.chapter)
        assertEquals("5", plugin.chunkName)
    }
}