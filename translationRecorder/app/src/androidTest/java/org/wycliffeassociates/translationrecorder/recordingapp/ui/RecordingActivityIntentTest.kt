package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkText
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 8/30/2017.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class RecordingActivityIntentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private lateinit var uiDevice: UiDevice

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var prefs: IPreferenceRepository

    @Before
    fun setup() {
        hiltRule.inject()
        initializeApp()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 3000)

        TestUtils.createTestUser(directoryProvider, db, prefs)
    }

    @Test
    fun testNewProjects() {
        var project = TestUtils.createBibleProject(db)
        testRecordingActivityDataFlow(project, 15, 1)
        println("Passed chapter 15 unit 1!")
        project = TestUtils.createBibleProject(db)
        testRecordingActivityDataFlow(project, 15, 2)
        println("Passed chapter 15 unit 2!")
        project = TestUtils.createBibleProject(db)
        testRecordingActivityDataFlow(project, 21, 1)
        println("Passed chapter 21 unit 1!")
        project = TestUtils.createBibleProject(db)
        testRecordingActivityDataFlow(project, 21, 2)
        println("Passed chapter 21 unit 2!")
    }

    private fun testRecordingActivityDataFlow(project: Project, chapter: Int, unit: Int) {
        //construct Intent to initialize playback activity based on parameters
        val intent = RecordingActivity.getNewRecordingIntent(
            context,
            project,
            chapter,
            unit
        )

        //launch our activity with this intent
        ActivityScenario.launch<RecordingActivity>(intent)

        testRecordingActivityInitialization(chapter, unit)
        testFlowToPlaybackActivity(chapter, unit)
    }

    private fun testRecordingActivityInitialization(chapter: Int, unit: Int) {
        try {
            //test initial chapter member variable is what we would expect
            checkText(chapter.toString(), true)

            //test initial chunk member variable is what we would expect
            checkText(unit.toString(), true)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun testFlowToPlaybackActivity(chapter: Int, unit: Int) {
        //Attach monitor to listen for the playback activity to launch
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(
            PlaybackActivity::class.java.name,
            null,
            false
        )

        uiDevice.findObject(UiSelector()
            .resourceId("${context.packageName}:id/btnRecording"))
            .click()

        Thread.sleep(1000)

        uiDevice.findObject(UiSelector()
            .resourceId("${context.packageName}:id/btnPauseRecording"))
            .click()
        uiDevice.findObject(UiSelector()
            .resourceId("${context.packageName}:id/btnStop"))
            .click()

        //try to get the playback activity now that the intent has fired
        val pba = instrumentation.waitForMonitorWithTimeout(
            monitor,
            TIME_OUT.toLong()
        ) as PlaybackActivity

        val intent = pba.intent

        val playbackChapter = intent.getIntExtra(PlaybackActivity.KEY_CHAPTER, -1)
        val playbackUnit = intent.getIntExtra(PlaybackActivity.KEY_UNIT, -1)
        Assert.assertEquals(
            "chapter and playback initialized chapter",
            chapter.toLong(),
            playbackChapter.toLong()
        )
        Assert.assertEquals(
            "unit and playback initialized unit",
            unit.toLong(),
            playbackUnit.toLong()
        )

        pba.finish()
    }

    val projectsList: List<Project>
        //constructs a list of all possible combinations of projects
        get() {
            val projects = ArrayList<Project>()
            val anthologies = db.anthologies
            val languages = arrayOf(Language("en", "English"))
            for (language in languages) {
                for (anthology in anthologies) {
                    val versions = db.getVersions(anthology.slug)
                    for (version in versions) {
                        val books = db.getBooks(anthology.slug)
                        for (book in books) {
                            val modes = db.getModes(anthology.slug)
                            for (mode in modes) {
                                val project = Project(
                                    language,
                                    anthology,
                                    book,
                                    version,
                                    mode
                                )
                                projects.add(project)
                            }
                        }
                    }
                }
            }
            return projects
        }

    @Throws(NoSuchFieldException::class, IOException::class, IllegalAccessException::class)
    fun getChunkPlugin(ctx: Context, project: Project): List<Chapter> {
        val chunkLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = project.getChunkPlugin(chunkLoader)
        val field = ChunkPlugin::class.java.getDeclaredField("mChapters")
        val chapters = field[plugin] as List<Chapter>
        return chapters
    }

    fun getChapterNumbersArray(chapters: List<Chapter>): IntArray {
        val chapterNumbers = IntArray(chapters.size)
        for (i in chapterNumbers.indices) {
            chapterNumbers[i] = chapters[i].number
        }
        return chapterNumbers
    }

    companion object {
        private const val TIME_OUT = 5000 /* miliseconds */
    }
}
