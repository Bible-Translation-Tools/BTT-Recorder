package org.wycliffeassociates.translationrecorder.recordingapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.awaits
import io.mockk.every
import io.mockk.just
import io.mockk.spyk
import io.mockk.verify
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
import org.wycliffeassociates.translationrecorder.FilesPage.Export.FolderExport
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FolderExportTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var initializeApp: InitializeApp

    @Before
    fun setUp() {
        hiltRule.inject()

        initializeApp()
    }

    @After
    fun tearDown() {
        directoryProvider.clearCache()
    }

    @Test
    fun folderExport() {
        val project = TestUtils.createBibleProject(db)
        val projectDir = ProjectFileUtils.getProjectDirectory(project, directoryProvider)
        projectDir.mkdirs()

        val exportTaskFragment = ExportTaskFragment()

        val exportDelegator = object: ProjectInfoDialog.ExportDelegator {
            override fun delegateExport(exp: Export) {
                exp.setFragmentContext(exportTaskFragment)
                exportTaskFragment.delegateExport(exp)
            }
        }

        val folderExport = spyk(FolderExport(project, directoryProvider), recordPrivateCalls = true)

        every { folderExport["handleUserInput"]() }.just(awaits)
        exportDelegator.delegateExport(folderExport)

        Thread.sleep(1000)

        val files = directoryProvider.uploadDir.listFiles()

        assertEquals(1, files?.size)

        val exportedFile = files?.get(0)

        assertNotNull(exportedFile)
        assertTrue(exportedFile!!.exists())
        assertTrue(exportedFile.length() > 0)

        verify { folderExport["handleUserInput"]() }
    }
}