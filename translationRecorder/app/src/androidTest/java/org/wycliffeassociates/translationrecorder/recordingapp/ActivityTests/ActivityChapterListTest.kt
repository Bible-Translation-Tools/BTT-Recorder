package org.wycliffeassociates.translationrecorder.recordingapp.ActivityTests

import android.app.Instrumentation.ActivityMonitor
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityChapterList
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ChapterCardAdapter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 9/21/2017.
 */
@HiltAndroidTest
@RunWith(Parameterized::class)
@LargeTest
class ActivityChapterListTest(private val i: Int, private val project: Project) {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var mActivityChapterListRule: ActivityTestRule<ActivityChapterList> = ActivityTestRule(
        ActivityChapterList::class.java,
        true,
        false
    )

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    @Before
    fun setup() {
        hiltRule.inject()
    }

    //    @Test
    //    public void ActivityChapterListTest() throws IllegalAccessException, NoSuchFieldException, IOException {
    //        Project bibleProject = createBibleTestProject(mTestActivity);
    //        Project notesProject = createNotesTestProject(mTestActivity);
    //
    //        testClickingChapterCard(bibleProject);
    //        testClickingChapterCard(notesProject);
    //    }
    @Test
    @Throws(
        IllegalAccessException::class,
        NoSuchFieldException::class,
        IOException::class
    )
    fun testClickingChapterCard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val chapterListMonitor = ActivityMonitor(
            ActivityChapterList::class.java.name,
            null,
            false
        )
        instrumentation.addMonitor(chapterListMonitor)
        mActivityChapterListRule.launchActivity(
            ActivityChapterList.getActivityUnitListIntent(context, project)
        )
        val chapterListActivity = chapterListMonitor.waitForActivity()
        val recyclerViewField = chapterListActivity.javaClass.getDeclaredField("mChapterList")
        recyclerViewField.isAccessible = true
        val rv = recyclerViewField[chapterListActivity] as RecyclerView

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = project.getChunkPlugin(pluginLoader)

        val chapters = plugin.chapters
        //number of children in the recycler view should match the number of chapters
        Assert.assertEquals(
            "Number of chapters vs number in adapter", chapters.size.toLong(), rv.adapter!!
                .itemCount.toLong()
        )


        //for(int i = 0; i < chapters.size(); i++) {
        val cc = (rv.adapter as ChapterCardAdapter).getItem(i - 1)
        Assert.assertEquals(cc.chapterNumber.toLong(), chapters[i - 1].number.toLong())
        val activityMonitor = ActivityMonitor(ActivityUnitList::class.java.name, null, false)
        InstrumentationRegistry.getInstrumentation().addMonitor(activityMonitor)


        //this hack seems to work? the sleep is necessary probably to give enough time for the data to bind to the view holder
        //fumbling around first with the scrolling seems to be necessary for it to not throw an exception saying one of the chapter numbers can't match
        Espresso.onView(ViewMatchers.withId(R.id.chapter_list)).perform(
            RecyclerViewActions.scrollToHolder(
                withChapterNumber(cc.chapterNumber)
            )
        )

        //onView(withId(R.id.chapter_list)).perform(RecyclerViewActions.scrollToHolder(withChapterNumber(chapters.get(0).getNumber())));
        //onView(withId(R.id.chapter_list)).perform(RecyclerViewActions.scrollToHolder(withChapterNumber(cc.getChapterNumber())));
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        val view = rv.findViewHolderForAdapterPosition(i - 1) as ChapterCardAdapter.ViewHolder?
        view?.onClick(view.binding.cardHeader)

        //onView(withId(R.id.chapter_list)).perform(RecyclerViewActions.actionOnHolderItem(withChapterNumber(cc.getChapterNumber()), click()));
        val aul = activityMonitor.waitForActivity() as ActivityUnitList


        val chapterField = ActivityUnitList::class.java.getDeclaredField("mChapterNum")
        chapterField.isAccessible = true
        val unitListChapter = chapterField.getInt(aul)
        println("chapters.get is " + chapters[i - 1].number)
        println("i is $i")
        println("unitListChapter is $unitListChapter")
        Assert.assertEquals(unitListChapter.toLong(), chapters[i - 1].number.toLong())
        println("SUCCESS: UnitListChapter " + unitListChapter + " and Chapter Number " + chapters[i - 1].number + " are the same!")
        aul.finish()
        InstrumentationRegistry.getInstrumentation().removeMonitor(activityMonitor)
        //}
        chapterListActivity.finish()
        instrumentation.removeMonitor(chapterListMonitor)
    }

    companion object {
        fun withChapterNumber(chapterNumber: Int): BoundedMatcher<RecyclerView.ViewHolder?, ChapterCardAdapter.ViewHolder> {
            return object : BoundedMatcher<RecyclerView.ViewHolder?, ChapterCardAdapter.ViewHolder>(
                ChapterCardAdapter.ViewHolder::class.java
            ) {
                override fun matchesSafely(item: ChapterCardAdapter.ViewHolder): Boolean {
                    return item.chapterCard!!.chapterNumber == chapterNumber
                }

                override fun describeTo(description: Description) {
                    description.appendText("view holder with chapter number: $chapterNumber")
                }
            }
        }
    }
}
