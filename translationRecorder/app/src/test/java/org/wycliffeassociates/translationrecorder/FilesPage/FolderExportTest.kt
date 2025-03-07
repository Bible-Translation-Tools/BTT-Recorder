package org.wycliffeassociates.translationrecorder.FilesPage

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export
import org.wycliffeassociates.translationrecorder.FilesPage.Export.ExportTaskFragment
import org.wycliffeassociates.translationrecorder.FilesPage.Export.FolderExport
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project

class FolderExportTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var directoryProvider: IDirectoryProvider
    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var assetsProvider: AssetsProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { directoryProvider.uploadDir }.returns(tempFolder.newFolder("uploads"))
        every { directoryProvider.createTempDir(any()) }.answers {
            val name = firstArg<String>()
            tempFolder.newFolder(name)
        }
        every { directoryProvider.translationsDir }.returns(tempFolder.newFolder("translations"))
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun `test folder export`() {
        val project: Project = mockk(relaxed = true) {
            every { targetLanguageSlug }.returns("aa")
            every { anthologySlug }.returns("nt")
            every { versionSlug }.returns("reg")
            every { bookSlug }.returns("mrk")
        }

        val exportTaskFragment: ExportTaskFragment = mockk {
            every { delegateExport(any()) }.answers {
                val exp: Export = firstArg()
                exp.export()
            }
        }

        val exportDelegator: ProjectInfoDialog.ExportDelegator = mockk {
            every { delegateExport(any()) }.answers {
                val exp: Export = firstArg()
                exp.setFragmentContext(exportTaskFragment)
                exportTaskFragment.delegateExport(exp)
            }
        }

        val appExport = FolderExport(project, directoryProvider, db, assetsProvider).apply {
            exportDelegator.delegateExport(this)
        }

        verify { exportTaskFragment.delegateExport(appExport) }
        verify { exportDelegator.delegateExport(appExport) }
    }
}