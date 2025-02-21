package org.wycliffeassociates.translationrecorder.usecases

import android.media.MediaMetadataRetriever
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider

class ImportProfileTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var directoryProvider: IDirectoryProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkConstructor(MediaMetadataRetriever::class)
        justRun { db.addUser(any()) }

        every { directoryProvider.profilesDir }.returns(tempFolder.newFolder("profiles"))
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testImportProfile() {
        val profile = tempFolder.newFile("profile")

        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(any()) }
            .returns("audio/mp4")

        val importProfile = ImportProfile(db, directoryProvider)
        importProfile(profile)

        verify { directoryProvider.profilesDir }
        verify { db.addUser(any()) }
    }

    @Test
    fun testImportInvalidProfile() {
        val profile = tempFolder.newFile("profile")

        every { anyConstructed<MediaMetadataRetriever>().extractMetadata(any()) }
            .returns("application/pdf")

        val importProfile = ImportProfile(db, directoryProvider)
        importProfile(profile)

        verify(exactly = 0) { directoryProvider.profilesDir }
        verify(exactly = 0) { db.addUser(any()) }
    }
}