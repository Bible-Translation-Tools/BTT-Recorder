package org.wycliffeassociates.translationrecorder.usecases

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider

class RestoreBackupTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var context: Context
    @MockK lateinit var directoryProvider: IDirectoryProvider
    @MockK private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { directoryProvider.internalCacheDir }.returns(tempFolder.newFolder("cache"))
        every { directoryProvider.internalAppDir }.returns(tempFolder.newFolder("internal"))
        every { directoryProvider.externalAppDir }.returns(tempFolder.newFolder("external"))
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testRestoreBackup() {
        val backupFile = tempFolder.newFile("backup.zip")
        val backupUri = mockk<Uri>()

        every { context.contentResolver }.returns(contentResolver)
        every { contentResolver.openInputStream(any()) }
            .returns(backupFile.inputStream())

        val restoreBackup = RestoreBackup(context, directoryProvider)
        restoreBackup(backupUri)

        verify { directoryProvider.internalAppDir }
        verify { directoryProvider.externalAppDir }
        verify { directoryProvider.internalCacheDir }
        verify { contentResolver.openInputStream(any()) }
    }
}