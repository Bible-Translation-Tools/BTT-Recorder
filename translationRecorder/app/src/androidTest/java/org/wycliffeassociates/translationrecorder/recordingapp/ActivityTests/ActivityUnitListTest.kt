package org.wycliffeassociates.translationrecorder.recordingapp.ActivityTests

import android.app.Instrumentation.ActivityMonitor
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList.Companion.getActivityUnitListIntent
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.UnitCardAdapter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.TestUtils.FragmentTestActivity
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.ProjectMockingUtil
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 9/21/2017.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class ActivityUnitListTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var mActivityUnitListRule: ActivityTestRule<ActivityUnitList> = ActivityTestRule(
        ActivityUnitList::class.java,
        true,
        false
    )

    @get:Rule
    var mTestActivity: ActivityTestRule<FragmentTestActivity> = ActivityTestRule(
        FragmentTestActivity::class.java,
        true,
        false
    )

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetProvider: AssetsProvider

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testBibleProject() {
        val bibleProject: Project = ProjectMockingUtil.createBibleTestProject(
            mTestActivity,
            directoryProvider
        )
        testClickingUnitCard(bibleProject)
    }

    @Test
    fun testNotesProject() {
        val notesProject: Project = ProjectMockingUtil.createNotesTestProject(
            mTestActivity,
            directoryProvider
        )
        testClickingUnitCard(notesProject)
    }

    @Throws(
        IllegalAccessException::class,
        NoSuchFieldException::class,
        IOException::class
    )
    fun testClickingUnitCard(project: Project) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val unitListMonitor = ActivityMonitor(
            ActivityUnitList::class.java.name,
            null,
            false
        )
        instrumentation.addMonitor(unitListMonitor)
        mActivityUnitListRule.launchActivity(
            getActivityUnitListIntent(context, project, 1)
        )
        val unitListActivity = unitListMonitor.waitForActivity()
        val recyclerViewField = unitListActivity.javaClass.getDeclaredField("mUnitList")
        recyclerViewField.isAccessible = true
        val rv = recyclerViewField[unitListActivity] as RecyclerView

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetProvider)
        val plugin = project.getChunkPlugin(pluginLoader)

        val units = plugin.getChunks(1)
        //number of children in the recycler view should match the number of units
        Assert.assertEquals(
            "Number of units vs number in adapter", units.size.toLong(), rv.adapter!!
                .itemCount.toLong()
        )

        val activityMonitor = ActivityMonitor(RecordingActivity::class.java.name, null, false)
        InstrumentationRegistry.getInstrumentation().addMonitor(activityMonitor)
        for (i in units.indices) {
            val cc = (rv.adapter as UnitCardAdapter).getItem(i)
            Assert.assertEquals(cc.startVerse.toLong(), units[i].startVerse.toLong())

            //this hack seems to work? the sleep is necessary probably to give enough time for the data to bind to the view holder
            //fumbling around first with the scrolling seems to be necessary for it to not throw an exception saying one of the unit numbers can't match
            Espresso.onView(ViewMatchers.withId(R.id.unit_list)).perform(
                RecyclerViewActions.scrollToHolder(
                    withUnitNumber(cc.startVerse)
                )
            )
            Espresso.onView(ViewMatchers.withId(R.id.unit_list)).perform(
                RecyclerViewActions.scrollToHolder(
                    withUnitNumber(
                        units[0].startVerse
                    )
                )
            )
            Espresso.onView(ViewMatchers.withId(R.id.unit_list)).perform(
                RecyclerViewActions.scrollToHolder(
                    withUnitNumber(cc.startVerse)
                )
            )
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            val view: View =
                ((rv.findViewHolderForAdapterPosition(i)) as UnitCardAdapter.ViewHolder).binding.unitRecordBtn
            val unitCardStartVerse =
                ((rv.findViewHolderForAdapterPosition(i)) as UnitCardAdapter.ViewHolder).unitCard!!.startVerse
            view.callOnClick()

            while (activityMonitor.hits < i + 1) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            var aul: RecordingActivity? = activityMonitor.lastActivity as RecordingActivity
            if (aul == null) {
                aul = activityMonitor.waitForActivity() as RecordingActivity
            }
            //            Field unitField = RecordingActivity.class.getDeclaredField("mInitialUnit");
//            unitField.setAccessible(true);
            //int unitListUnit = unitField.getInt(aul);
            val unitListUnit = aul.intent.getIntExtra(RecordingActivity.KEY_UNIT, -1)
            println("units.get is " + units[i].startVerse)
            println("i is $i")
            println("unitListUnit is $unitListUnit")
            println("unitcard start verse is $unitCardStartVerse")
            Assert.assertEquals(unitListUnit.toLong(), units[i].startVerse.toLong())
            println("SUCCESS: UnitListUnit " + unitListUnit + " and Unit Number " + units[i].startVerse + " are the same!")
            aul.finish()
            aul = null
        }
        InstrumentationRegistry.getInstrumentation().removeMonitor(activityMonitor)
        unitListActivity.finish()
        instrumentation.removeMonitor(unitListMonitor)
    }

    companion object {
        fun withUnitNumber(unitNumber: Int): BoundedMatcher<RecyclerView.ViewHolder?, UnitCardAdapter.ViewHolder> {
            return object : BoundedMatcher<RecyclerView.ViewHolder?, UnitCardAdapter.ViewHolder>(
                UnitCardAdapter.ViewHolder::class.java
            ) {
                override fun matchesSafely(item: UnitCardAdapter.ViewHolder): Boolean {
                    return item.unitCard!!.startVerse == unitNumber
                }

                override fun describeTo(description: Description) {
                    description.appendText("view holder with unit number: $unitNumber")
                }
            }
        }
    }
}
