package org.wycliffeassociates.translationrecorder.recordingapp.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export
import org.wycliffeassociates.translationrecorder.FilesPage.Export.ExportTaskFragment
import org.wycliffeassociates.translationrecorder.FilesPage.Export.TranslationExchangeExport
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TranslationExchangeExportTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var assetsProvider: AssetsProvider
    @Inject lateinit var prefs: IPreferenceRepository

    private val server = MockWebServer()

    @Before
    fun setUp() {
        hiltRule.inject()

        initializeApp.run()
        server.start()

        prefs.setDefaultPref(
            SettingsActivity.KEY_PREF_UPLOAD_SERVER,
            server.url("").toString()
        )
    }

    @After
    fun tearDown() {
        directoryProvider.clearCache()
        server.shutdown()
    }

    @Test
    fun translationExchangeExport() {
        val project = TestUtils.createBibleProject(db)
        val projectDir = ProjectFileUtils.getProjectDirectory(project, directoryProvider)
        projectDir.mkdirs()

        val exportTaskFragment = spyk(ExportTaskFragment())
        every { exportTaskFragment.getString(any()) }.returns("test")
        every { exportTaskFragment.requireActivity() }.returns(
            mockk {
                every { applicationContext }.returns(context)
            }
        )

        val exportDelegator = object: ProjectInfoDialog.ExportDelegator {
            override fun delegateExport(exp: Export) {
                exp.setFragmentContext(exportTaskFragment)
                exportTaskFragment.delegateExport(exp)
            }
        }

        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))

        val tEExport = spyk(
            TranslationExchangeExport(
                project,
                db,
                directoryProvider,
                prefs,
                assetsProvider
            ),
            recordPrivateCalls = true
        )

        exportDelegator.delegateExport(tEExport)

        Thread.sleep(1000)

        val files = directoryProvider.uploadDir.listFiles()

        assertEquals(1, files?.size)

        val exportedFile = files?.get(0)

        assertNotNull(exportedFile)
        assertTrue(exportedFile!!.exists())
        assertTrue(exportedFile.length() > 0)

        verify { exportTaskFragment.getString(any()) }
        verify { exportTaskFragment.requireActivity() }
    }
}