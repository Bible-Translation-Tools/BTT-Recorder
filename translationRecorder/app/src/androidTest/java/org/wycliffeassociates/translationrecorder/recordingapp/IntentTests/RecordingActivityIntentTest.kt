package org.wycliffeassociates.translationrecorder.recordingapp.IntentTests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.Recording.UnitPicker
import org.wycliffeassociates.translationrecorder.Recording.fragments.FragmentRecordingFileBar
import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.recordingapp.ProjectMockingUtil
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
    var mActivityRule: ActivityTestRule<RecordingActivity> = ActivityTestRule(
        RecordingActivity::class.java,
        true,
        false
    )

    @get:Rule
    var mSplashScreenRule: ActivityTestRule<MainMenu> = ActivityTestRule(
        MainMenu::class.java,
        true,
        false
    )

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testNewProjects() {
        var project: Project = ProjectMockingUtil.createBibleTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(project, 1, 1)
        println("Passed chapter 1 unit 1!")
        project = ProjectMockingUtil.createBibleTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(project, 1, 2)
        println("Passed chapter 1 unit 2!")
        project = ProjectMockingUtil.createBibleTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(project, 2, 1)
        println("Passed chapter 2 unit 1!")
        project = ProjectMockingUtil.createBibleTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(project, 2, 2)
        println("Passed chapter 2 unit 2!")

        //use chunk 3 since there is no chunk 2
        var projectNotes: Project = ProjectMockingUtil.createNotesTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(projectNotes, 1, 1)
        println("Passed chunk 1 text 1!")
        projectNotes = ProjectMockingUtil.createNotesTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(projectNotes, 1, 2)
        println("Passed chunk 1 ref 1!")
        projectNotes = ProjectMockingUtil.createNotesTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(projectNotes, 3, 1)
        println("Passed chunk 3 text 1!")
        projectNotes = ProjectMockingUtil.createNotesTestProject(
            mSplashScreenRule,
            directoryProvider
        )
        testRecordingActivityDataFlow(projectNotes, 3, 2)
        println("Passed chunk 3 ref 1!")
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
        mActivityRule.launchActivity(intent)
        val recordingActivity = mActivityRule.activity
        testRecordingActivityInitialization(recordingActivity, project, chapter, unit)
        testFlowToPlaybackActivity(recordingActivity, chapter, unit)
    }

    fun testRecordingActivityInitialization(
        recordingActivity: RecordingActivity,
        project: Project,
        chapter: Int,
        unit: Int
    ) {
        try {
            //test initial chapter member variable is what we would expect
            val chapterField = recordingActivity.javaClass.getDeclaredField("mInitialChapter")
            chapterField.isAccessible = true
            val mChapter = chapterField[recordingActivity] as Int
            Assert.assertEquals(
                "Chapter number used for intent vs recording activity member variable",
                chapter.toLong(),
                mChapter.toLong()
            )

            //test initial chunk member variable is what we would expect
            val chunkField = recordingActivity.javaClass.getDeclaredField("mInitialChunk")
            chunkField.isAccessible = true
            val mChunk = chapterField[recordingActivity] as Int
            Assert.assertEquals(unit.toLong(), mChunk.toLong())

            //test that these initial values correctly initialized the fragment field
            val fragmentField = recordingActivity
                .javaClass
                .getDeclaredField("mFragmentRecordingFileBar")
            fragmentField.isAccessible = true
            val frfb =
                fragmentField[recordingActivity] as FragmentRecordingFileBar
            Assert.assertEquals(chapter.toLong(), frfb.chapter.toLong())
            Assert.assertEquals(unit.toLong(), frfb.unit.toLong())

            val chapterPickerField = frfb.javaClass.getDeclaredField("mChapterPicker")
            chapterPickerField.isAccessible = true
            val mChapterPicker = chapterPickerField[frfb] as UnitPicker
            Assert.assertEquals(
                project.getFileName(
                    chapter,
                    frfb.chapter,
                    frfb.startVerse.toInt(),
                    frfb.endVerse.toInt()
                ),
                mChapterPicker.currentDisplayedValue
            )
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    fun testFlowToPlaybackActivity(
        recordingActivity: RecordingActivity,
        chapter: Int,
        unit: Int
    ) {
        //Attach monitor to listen for the playback activity to launch
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(
            PlaybackActivity::class.java.name,
            null,
            false
        )

        recordingActivity.runOnUiThread { //Fire intent from recording Activity
            recordingActivity.onStartRecording()
        }

        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        recordingActivity.onStopRecording()

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
