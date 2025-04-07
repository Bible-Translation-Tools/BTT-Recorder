package org.wycliffeassociates.translationrecorder.FilesPage

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export
import org.wycliffeassociates.translationrecorder.FilesPage.Export.ExportTaskFragment
import org.wycliffeassociates.translationrecorder.FilesPage.Export.TranslationExchangeExport
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project

class TranslationExchangeExportTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var directoryProvider: IDirectoryProvider
    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var prefs: IPreferenceRepository
    @MockK lateinit var assetsProvider: AssetsProvider

    private val server = MockWebServer()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        server.start()

        every { directoryProvider.uploadDir }.returns(tempFolder.newFolder("uploads"))
        every { directoryProvider.createTempDir(any()) }.answers {
            val name = firstArg<String>()
            tempFolder.newFolder(name)
        }
        every { directoryProvider.translationsDir }.returns(tempFolder.newFolder("translations"))

        every { prefs.getDefaultPref(SettingsActivity.KEY_PREF_UPLOAD_SERVER, any<String>()) }
            .returns(server.url("").toString())
    }

    @After
    fun tearDown() {
        tempFolder.delete()
        server.shutdown()
    }

    @Test
    fun `test translation exchange export`() {
        server.enqueue(MockResponse().setBody("[]").setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))

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
            every { requireContext() }.returns(mockk {
                every { getString(any()) }.returns(server.url("/").toString())
            })
        }

        val exportDelegator: ProjectInfoDialog.ExportDelegator = mockk {
            every { delegateExport(any()) }.answers {
                val exp: Export = firstArg()
                exp.setFragmentContext(exportTaskFragment)
                exportTaskFragment.delegateExport(exp)
            }
        }

        val appExport = TranslationExchangeExport(
            project,
            db,
            directoryProvider,
            prefs,
            assetsProvider,
            server.url("/").toString()
        )
        exportDelegator.delegateExport(appExport)

        verify { exportTaskFragment.delegateExport(appExport) }
        verify { exportDelegator.delegateExport(appExport) }
    }
}