package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.app.Instrumentation.ActivityMonitor
import android.content.Context
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Description
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList.Companion.getActivityUnitListIntent
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.UnitCardAdapter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkListViewHasItemsCount
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.clickItemWithId
import org.wycliffeassociates.translationrecorder.recordingapp.tryPerform
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

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetProvider: AssetsProvider
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var prefs: IPreferenceRepository

    @Before
    fun setup() {
        hiltRule.inject()
        initializeApp.run()

        TestUtils.createTestUser(directoryProvider, db, prefs)
    }

    @Test
    fun testBibleProject() {
        val bibleProject = TestUtils.createBibleProject(db)
        testClickingUnitCard(bibleProject)
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

        ActivityScenario.launch<ActivityUnitList>(getActivityUnitListIntent(context, project, 1))

        val unitListActivity = unitListMonitor.waitForActivity()

        val pluginLoader = ChunkPluginLoader(directoryProvider, assetProvider)
        val plugin = project.getChunkPlugin(pluginLoader)

        val units = plugin.getChunks(1)

        checkListViewHasItemsCount(withId(R.id.unit_list), units.size)

        val activityMonitor = ActivityMonitor(RecordingActivity::class.java.name, null, false)
        InstrumentationRegistry.getInstrumentation().addMonitor(activityMonitor)
        for (i in units.indices) {
            val cc = units[i]

            // limit to 10 units
            if (i > 10) {
                return
            }

            //this hack seems to work? the sleep is necessary probably to give enough time for the data to bind to the view holder
            //fumbling around first with the scrolling seems to be necessary for it to not throw an exception saying one of the unit numbers can't match
            onView(withId(R.id.unit_list)).tryPerform(
                RecyclerViewActions.scrollToHolder(
                    withUnitNumber(cc.startVerse)
                )
            )
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            onView(withId(R.id.unit_list)).tryPerform(
                actionOnItemAtPosition<ViewHolder>(i, clickItemWithId(R.id.unitRecordBtn))
            )

            while (activityMonitor.hits < i + 1) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            var aul = activityMonitor.lastActivity as? RecordingActivity
            if (aul == null) {
                aul = activityMonitor.waitForActivity() as RecordingActivity
            }
            val unitListUnit = aul.intent.getIntExtra(RecordingActivity.KEY_UNIT, -1)
            println("units.get is " + units[i].startVerse)
            println("i is $i")
            println("unitListUnit is $unitListUnit")
            println("unitcard start verse is ${cc.startVerse}")
            Assert.assertEquals(unitListUnit.toLong(), units[i].startVerse.toLong())
            println("SUCCESS: UnitListUnit " + unitListUnit + " and Unit Number " + units[i].startVerse + " are the same!")
            aul.finish()
        }
        InstrumentationRegistry.getInstrumentation().removeMonitor(activityMonitor)
        unitListActivity.finish()
        instrumentation.removeMonitor(unitListMonitor)
    }

    private fun withUnitNumber(unitNumber: Int): BoundedMatcher<ViewHolder?, UnitCardAdapter.ViewHolder> {
        return object : BoundedMatcher<ViewHolder?, UnitCardAdapter.ViewHolder>(
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
