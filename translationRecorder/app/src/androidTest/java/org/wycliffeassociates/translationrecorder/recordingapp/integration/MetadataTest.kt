package org.wycliffeassociates.translationrecorder.recordingapp.integration

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.wycliffeassociates.translationrecorder.recordingapp.IntegrationTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.wav.WavFile
import org.wycliffeassociates.translationrecorder.wav.WavMetadata
import org.wycliffeassociates.translationrecorder.wav.WavOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@IntegrationTest
class MetadataTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var initializeApp: InitializeApp

    private lateinit var testFile: File
    private lateinit var project: Project
    private var chapter: String = "01"
    private var startVerse: String = "01"
    private var endVerse: String = "02"

    private lateinit var testFile2: File
    private lateinit var project2: Project
    private var chapter2: String = "014"
    private var startVerse2: String = "01"
    private var endVerse2: String = "01"

    @Before
    fun setUp() {
        hiltRule.inject()

        initializeApp()

        try {
            testFile = directoryProvider.createTempFile("test", ".wav")
            testFile2 = directoryProvider.createTempFile("test2", ".wav")

            project = TestUtils.createBibleProject("en", "ot", "gen", "ulb", "chunk", db)
            project2 = TestUtils.createBibleProject("cmn", "nt", "eph", "reg", "verse", db)
        } catch (e: IOException) {
            Assert.fail("Test failed : " + e.message)
            e.printStackTrace()
        }
    }

    @After
    fun tearDown() {
        directoryProvider.clearCache()
    }

    @Test
    fun testMetadata() {
        try {
            val meta = WavMetadata(project, "test", chapter, startVerse, endVerse)
            val preWav = meta.toJSON()
            var wav = WavFile(testFile, meta)
            Assert.assertEquals(wav.file.length(), 44)
            WavOutputStream(wav).use { wos ->
                for (i in 0..999) {
                    wos.write(i)
                }
            }
            val postWav = wav.metadata.toJSON()
            //Test: may need to modify the compare json method
            compareJson(preWav, postWav)
            wav.addMarker("1", 0)
            wav.addMarker("2", 500)

            //Test
            Assert.assertEquals(1000, wav.totalAudioLength.toLong())

            val parcel = Parcel.obtain()
            wav.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)

            val meta2 = WavMetadata(project2, "test", chapter2, startVerse2, endVerse2)
            wav = WavFile(testFile2, meta2)
            wav.addMarker("1", 0)

            //Test
            testParcel(parcel, project, chapter, startVerse, endVerse)
        } catch (e: IOException) {
        }
    }

    private fun compareJson(one: JSONObject, two: JSONObject) {
        try {
            Assert.assertEquals(one.getString("anthology"), two.getString("anthology"))
            Assert.assertEquals(one.getString("language"), two.getString("language"))
            Assert.assertEquals(one.getString("version"), two.getString("version"))
            Assert.assertEquals(one.getString("slug"), two.getString("slug"))
            Assert.assertEquals(one.getString("book_number"), two.getString("book_number"))
            Assert.assertEquals(one.getString("mode"), two.getString("mode"))
            Assert.assertEquals(one.getString("chapter"), two.getString("chapter"))
            Assert.assertEquals(one.getString("startv"), two.getString("startv"))
            Assert.assertEquals(one.getString("endv"), two.getString("endv"))
            Assert.assertEquals(one.has("markers"), two.has("markers"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun testParcel(
        parcel: Parcel,
        project: Project,
        chapter: String,
        startVerse: String,
        endVerse: String
    ) {
        val parceled = WavFile.CREATOR.createFromParcel(parcel)
        val metadata = parceled.metadata
        Assert.assertEquals(project.anthologySlug, metadata.anthology)
        Assert.assertEquals(project.modeSlug, metadata.modeSlug)
        Assert.assertEquals(project.bookSlug, metadata.slug)
        Assert.assertEquals(project.targetLanguageSlug, metadata.language)
        Assert.assertEquals(project.bookNumber, metadata.bookNumber)
        Assert.assertEquals(project.versionSlug, metadata.version)
        Assert.assertEquals(chapter, metadata.chapter)
        Assert.assertEquals(startVerse, metadata.startVerse)
        Assert.assertEquals(endVerse, metadata.endVerse)
    }

    fun getWordAlignmentPadding(length: Int): Int {
        var padding = length % 4
        if (padding != 0) {
            padding = 4 - padding
        }
        return padding
    }
}


