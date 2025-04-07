package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasCategories
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.wycliffeassociates.translationrecorder.recordingapp.UITest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.DocumentationActivity
import org.wycliffeassociates.translationrecorder.FilesPage.Export.AppExport.ShareZipToApps
import org.wycliffeassociates.translationrecorder.FilesPage.Export.FolderExport.StorageAccess
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityChapterList
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityProjectManager
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.SplashScreen
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkDialogContainsText
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkDialogText
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkListViewHasItemsCount
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkRecyclerViewChild
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkText
import org.wycliffeassociates.translationrecorder.recordingapp.tryPerform
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UITest
class ActivityProjectManagerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var prefs: IPreferenceRepository

    private val server = MockWebServer()

    @Before
    fun setup() {
        hiltRule.inject()
        initializeApp()
        Intents.init()

        server.start()
        prefs.setDefaultPref(
            SettingsActivity.KEY_PREF_UPLOAD_SERVER,
            server.url("").toString()
        )

        TestUtils.createTestUser(directoryProvider, db, prefs)
    }

    @After
    fun tearDown() {
        Intents.release()
        server.shutdown()
    }

    @Test
    fun noProjects() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(R.string.project_management, true)
            checkText(R.string.new_project, true)
        }
    }

    @Test
    fun createNewProject() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            onView(withText(R.string.new_project)).tryPerform(click())
            onView(withText("aaa")).tryPerform(click())
            onView(withText("nt")).tryPerform(click())
            onView(withText("mrk")).tryPerform(click())
            onView(withText("reg")).tryPerform(click())
            onView(withText("verse")).tryPerform(click())
            onView(withText(R.string.label_skip)).tryPerform(click())

            Intents.intended(hasComponent(RecordingActivity::class.java.name))
        }
    }

    @Test
    fun createNewProjectWhenHasProject() {
        TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            onView(withId(R.id.new_project_fab)).tryPerform(click())
            onView(withText("aaa")).tryPerform(click())
            onView(withText("nt")).tryPerform(click())
            onView(withText("mrk")).tryPerform(click())
            onView(withText("reg")).tryPerform(click())
            onView(withText("verse")).tryPerform(click())
            onView(withText(R.string.label_skip)).tryPerform(click())

            Intents.intended(hasComponent(RecordingActivity::class.java.name))
        }
    }

    @Test
    fun hasProject() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)
        }
    }

    @Test
    fun openInfoDialog() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.info_button)).tryPerform(click())

            val book = "${project.bookName} (${project.bookSlug})"
            checkDialogText(book, true)

            val language = "${project.targetLanguage.name} (${project.targetLanguageSlug})"
            checkDialogText(language, true)

            val version = "(${project.versionSlug})"
            checkDialogContainsText(version, true)

            checkDialogText("Verse", true)
        }
    }

    @Test
    fun startProjectRecording() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.record_button)).tryPerform(click())

            Intents.intended(hasComponent(RecordingActivity::class.java.name))
        }
    }

    @Test
    fun openProjectChaptersList() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.text_layout)).tryPerform(click())

            Intents.intended(hasComponent(ActivityChapterList::class.java.name))
        }
    }

    @Test
    fun hasProjects() {
        val project1 = TestUtils.createBibleProject(db)
        val project2 = TestUtils.createBibleProject(
            "en",
            "nt",
            "mrk",
            "ulb",
            "verse",
            db
        )

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project1.bookName, true)

            // Even though there are two projects, one of them is moved out to a recent item
            checkListViewHasItemsCount(withId(R.id.project_list), 1)
            checkRecyclerViewChild(
                withId(R.id.project_list),
                withText(project2.bookName),
                0,
                true
            )
        }
    }

    @Test
    fun deleteProject() {
        val project = TestUtils.createBibleProject(db)
        val projectId = db.getProjectId(project)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.info_button)).tryPerform(click())
            onView(withId(R.id.delete_button)).tryPerform(click())

            checkDialogText(R.string.delete_project, true)
            checkDialogText(R.string.confirm_delete_project_alt, true)

            onView(withText(R.string.yes)).tryPerform(click())

            assertNull(db.getProject(projectId))
        }
    }

    @Test
    fun exportSourceAudio() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.info_button)).tryPerform(click())
            onView(withId(R.id.export_as_source_btn)).tryPerform(click())

            Intents.intended(allOf(
                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))),
                hasType("*/*")
            ))
        }
    }

    @Test
    fun exportToZip() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.info_button)).tryPerform(click())
            onView(withId(R.id.folder_button)).tryPerform(click())

            Intents.intended(hasComponent(StorageAccess::class.java.name))
        }
    }

    @Test
    fun exportToApp() {
        val project = TestUtils.createBibleProject(db)

        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            checkText(project.bookName, true)

            onView(withId(R.id.info_button)).tryPerform(click())
            onView(withId(R.id.other_button)).tryPerform(click())

            Intents.intended(hasComponent(ShareZipToApps::class.java.name))
        }
    }

    @Test
    fun openImport() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            onView(withId(R.id.action_more)).tryPerform(click())
            onView(withText(R.string.import_menu)).tryPerform(click())

            Intents.intended(hasAction(Intent.ACTION_GET_CONTENT))
            Intents.intended(hasCategories(hasItem(equalTo(Intent.CATEGORY_OPENABLE))))
            Intents.intended(hasType("application/zip"))
        }
    }

    @Test
    fun openSettings() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            onView(withId(R.id.action_more)).tryPerform(click())
            onView(withText(R.string.settings_menu)).tryPerform(click())

            Intents.intended(hasComponent(SettingsActivity::class.java.name))
        }
    }

    @Test
    fun openHelp() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            onView(withId(R.id.action_more)).tryPerform(click())
            onView(withText(R.string.help_menu)).tryPerform(click())

            Intents.intended(hasComponent(DocumentationActivity::class.java.name))
        }
    }

    @Test
    fun logout() {
        ActivityScenario.launch(ActivityProjectManager::class.java).use {
            val userIdBefore = prefs.getDefaultPref(SettingsActivity.KEY_PROFILE, -1)
            assertEquals(1, userIdBefore)

            onView(withId(R.id.action_more)).tryPerform(click())
            onView(withText(R.string.logout_menu)).tryPerform(click())

            Intents.intended(hasComponent(SplashScreen::class.java.name))

            val userIdAfter = prefs.getDefaultPref(SettingsActivity.KEY_PROFILE, -1)
            assertEquals(-1, userIdAfter)
        }
    }

//    @Test
//    fun exportToTranslationExchange() {
//        val project = TestUtils.createBibleProject(db)
//
//        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))
//        server.enqueue(MockResponse().setResponseCode(200))
//
//        ActivityScenario.launch(ActivityProjectManager::class.java).use {
//            checkText(project.bookName, true)
//
//            onView(withId(R.id.info_button)).tryPerform(click())
//            onView(withId(R.id.publish_button)).tryPerform(click())
//
//            waitFor(5000)
//
//            val files = directoryProvider.uploadDir.listFiles()
//            assertEquals(1, files?.size)
//
//            val exportedFile = files?.get(0)
//            assertNotNull(exportedFile)
//            assertTrue(exportedFile!!.exists())
//            assertTrue(exportedFile.length() > 0)
//        }
//    }
}