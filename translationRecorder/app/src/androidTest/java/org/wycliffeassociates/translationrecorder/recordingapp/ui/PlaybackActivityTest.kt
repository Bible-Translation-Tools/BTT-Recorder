package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.Manifest
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.InitializeApp
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.TestUtils
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.waitFor
import org.wycliffeassociates.translationrecorder.wav.WavFile
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PlaybackActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule var tempFolder = TemporaryFolder()

    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var initializeApp: InitializeApp
    @Inject lateinit var prefs: IPreferenceRepository

    private lateinit var project: Project

    @Before
    fun setup() {
        hiltRule.inject()
        initializeApp.run()

        TestUtils.createTestUser(directoryProvider, db, prefs)
        project = TestUtils.createBibleProject(db)
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testPlayback() {
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val filename = "aa_reg_b01_gen_c01_v01_t01.wav"
        val file = tempFolder.newFile(filename)
        testContext.assets.open(filename).use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val wavFile = WavFile(file)
        val intent = PlaybackActivity.getPlaybackIntent(context, wavFile, project, 1, 1)

        ActivityScenario.launch<PlaybackActivity>(intent).use {
            waitFor(5000)
        }
    }
}