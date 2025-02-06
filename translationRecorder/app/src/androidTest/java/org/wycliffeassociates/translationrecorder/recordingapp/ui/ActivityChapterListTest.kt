package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.app.Instrumentation.ActivityMonitor
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityChapterList
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ChapterCardAdapter.ViewHolder
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkContainsText
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkListViewHasItemsCount
import org.wycliffeassociates.translationrecorder.recordingapp.tryPerform
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 9/21/2017.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ActivityChapterListTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var initializeApp: InitializeApp

    @Before
    fun setup() {
        hiltRule.inject()
        initializeApp()
    }

    @Test
    @Throws(
        IllegalAccessException::class,
        NoSuchFieldException::class,
        IOException::class
    )
    fun activityChapterListTest() {
        val bibleProject = TestUtils.createBibleProject(db)
        testClickingChapterCard(bibleProject)
    }

    private fun testClickingChapterCard(project: Project) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val chapterListMonitor = ActivityMonitor(
            ActivityChapterList::class.java.name,
            null,
            false
        )
        instrumentation.addMonitor(chapterListMonitor)

        ActivityScenario.launch<ActivityChapterList>(
            ActivityChapterList.getActivityChapterListIntent(context, project)
        )

        val chapterListActivity = chapterListMonitor.waitForActivity()

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = project.getChunkPlugin(pluginLoader)

        val chapters = plugin.chapters

        checkListViewHasItemsCount(withId(R.id.chapter_list), chapters.size)

        chapters.forEach { chapter ->
            // limit to 10 chapters
            if (chapter.number > 10) {
                return@forEach
            }

            val activityMonitor = ActivityMonitor(ActivityUnitList::class.java.name, null, false)
            InstrumentationRegistry.getInstrumentation().addMonitor(activityMonitor)

            onView(withId(R.id.chapter_list)).perform(
                RecyclerViewActions.scrollToHolder(
                    withChapterNumber(chapter.number)
                )
            )

            onView(withText("Chapter ${chapter.number}")).tryPerform(click())

            val aul = activityMonitor.waitForActivity() as? ActivityUnitList

            checkContainsText("Chapter ${chapter.number}", true)

            aul?.finish()
            InstrumentationRegistry.getInstrumentation().removeMonitor(activityMonitor)
        }

        chapterListActivity.finish()
        instrumentation.removeMonitor(chapterListMonitor)
    }

    companion object {
        fun withChapterNumber(chapterNumber: Int): BoundedMatcher<RecyclerView.ViewHolder?, ViewHolder> {
            return object : BoundedMatcher<RecyclerView.ViewHolder?, ViewHolder>(ViewHolder::class.java) {
                override fun matchesSafely(item: ViewHolder): Boolean {
                    return item.chapterCard!!.chapterNumber == chapterNumber
                }

                override fun describeTo(description: Description) {
                    description.appendText("view holder with chapter number: $chapterNumber")
                }
            }
        }
    }
}
