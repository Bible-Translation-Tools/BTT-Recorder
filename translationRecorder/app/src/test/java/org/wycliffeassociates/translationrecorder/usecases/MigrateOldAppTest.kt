package org.wycliffeassociates.translationrecorder.usecases

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.net.Uri
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import java.io.File

class MigrateOldAppTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var context: Context
    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var directoryProvider: IDirectoryProvider
    @MockK lateinit var importProject: ImportProject
    @MockK lateinit var importProfile: ImportProfile

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testMigrationFromFileUri() {
        val appDataFolder = tempFolder.newFolder("BTTRecorder")
        val profilesDir = File(appDataFolder, "Profiles")
        profilesDir.mkdir()
        val profile = File(profilesDir, "test_profile")
        profile.createNewFile()
        val translationsDir = File(appDataFolder, "aa")
        translationsDir.mkdir()

        val appDataUri = mockk<Uri> {
            every { scheme }.returns(SCHEME_FILE)
            every { path }.returns(appDataFolder.absolutePath)
        }

        every { directoryProvider.createTempDir(any()) }
            .returns(tempFolder.newFolder("tmp"))
        justRun { importProfile.invoke(any()) }
        justRun { importProject.invoke(any()) }

        val migrateOldApp = MigrateOldApp(
            context,
            directoryProvider,
            importProject,
            importProfile
        )
        migrateOldApp(appDataUri)

        verify { directoryProvider.createTempDir(any()) }
        verify { importProject(any()) }
        verify { importProfile(any()) }
    }
}