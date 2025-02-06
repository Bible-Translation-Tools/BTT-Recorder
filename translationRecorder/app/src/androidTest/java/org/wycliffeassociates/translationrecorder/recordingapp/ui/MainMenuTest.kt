package org.wycliffeassociates.translationrecorder.recordingapp.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityProjectManager
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.ProjectWizardActivity
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.tryPerform
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainMenuTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var prefs: IPreferenceRepository

    @Before
    fun setup() {
        hiltRule.inject()

        initializeApp()

        Intents.init()

        TestUtils.createTestUser(directoryProvider, db, prefs)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testLaunchAudioFiles() {
        ActivityScenario.launch(MainMenu::class.java)

        // Touch the "files" button (hamburger icon)
        onView(ViewMatchers.withId(R.id.files)).tryPerform(click())
        // Verify that an Intent was sent to open the Project Wizard Screen
        Intents.intended(hasComponent(ActivityProjectManager::class.java.name))
    }

    @Test
    fun testLaunchRecordingScreen() {
        ActivityScenario.launch(MainMenu::class.java)

        // Touch the "new_record" button (the big microphone)
        Espresso.onView(ViewMatchers.withId(R.id.new_record)).perform(click())
        // Verify that an Intent was sent to open the Recording Screen
        Intents.intended(hasComponent(ProjectWizardActivity::class.java.name))

        onView(withText("aaa")).tryPerform(click())
        onView(withText("nt")).tryPerform(click())
        onView(withText("mrk")).tryPerform(click())
        onView(withText("reg")).tryPerform(click())
        onView(withText("verse")).tryPerform(click())
        onView(withText("SKIP")).tryPerform(click())

        Intents.intended(hasComponent(RecordingActivity::class.java.name))
    }
}
