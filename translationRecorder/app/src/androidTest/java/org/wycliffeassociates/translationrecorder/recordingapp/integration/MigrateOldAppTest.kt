package org.wycliffeassociates.translationrecorder.recordingapp.integration

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.wycliffeassociates.translationrecorder.recordingapp.IntegrationTest
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.usecases.ImportProfile
import org.wycliffeassociates.translationrecorder.usecases.ImportProject
import org.wycliffeassociates.translationrecorder.usecases.MigrateOldApp
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@IntegrationTest
class MigrateOldAppTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var tempFolder = TemporaryFolder()

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var importProject: ImportProject
    @Inject lateinit var importProfile: ImportProfile
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var assetsProvider: AssetsProvider

    @Before
    fun setUp() {
        hiltRule.inject()
        initializeApp()
    }

    @After
    fun tearDown() {
        directoryProvider.clearCache()
        tempFolder.delete()
    }

    @Test
    fun migrateFromEmptyOldAppData() {
        val oldAppDataFolder = tempFolder.newFolder("BTTRecorder")

        val migrate = MigrateOldApp(
            context,
            directoryProvider,
            importProject,
            importProfile
        )
        migrate(Uri.fromFile(oldAppDataFolder))

        assertTrue(directoryProvider.translationsDir.listFiles().isNullOrEmpty())
        assertTrue(directoryProvider.profilesDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun migrateFromOldAppDataNoProfilesDoesNotImport() {
        val oldAppDataFolder = tempFolder.newFolder("BTTRecorder")

        val projectDir = File(oldAppDataFolder, "aa/reg/gen")
        projectDir.mkdirs()
        createTestFile(projectDir, "aa_reg_b01_gen_c33_v12_t01.wav")

        val migrate = MigrateOldApp(
            context,
            directoryProvider,
            importProject,
            importProfile
        )
        migrate(Uri.fromFile(oldAppDataFolder))

        assertTrue(directoryProvider.translationsDir.listFiles().isNullOrEmpty())
        assertTrue(directoryProvider.profilesDir.listFiles().isNullOrEmpty())
    }

    @Test
    fun migrateFromOldAppData() {
        val oldAppDataFolder = tempFolder.newFolder("BTTRecorder")

        val projectDir = File(oldAppDataFolder, "aa/reg/gen")
        projectDir.mkdirs()
        createTestFile(projectDir, "aa_reg_b01_gen_c33_v12_t01.wav")

        val profilesDir = File(oldAppDataFolder, "Profiles")
        profilesDir.mkdirs()
        createTestFile(profilesDir, "341c51b9-980a-4977-9797-2115ea4173eb")

        val migrate = MigrateOldApp(
            context,
            directoryProvider,
            importProject,
            importProfile
        )
        migrate(Uri.fromFile(oldAppDataFolder))

        assertEquals(1, directoryProvider.translationsDir.listFiles()?.size)
        assertEquals(1, directoryProvider.profilesDir.listFiles()?.size)
    }

    private fun createTestFile(parent: File, filename: String) {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val file = File(parent, filename)
        testContext.assets.open(filename).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}