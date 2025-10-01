package org.wycliffeassociates.translationrecorder.usecases

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
import org.wycliffeassociates.translationrecorder.project.components.User
import java.io.File

class ImportProjectTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var directoryProvider: IDirectoryProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { directoryProvider.profilesDir }
            .returns(tempFolder.newFolder("profiles"))
        every { directoryProvider.translationsDir }
            .returns(tempFolder.newFolder("translations"))
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testImportProject() {
        val projectDir = tempFolder.newFolder("project")
        val audioFile = File(projectDir, "aa_reg_b01_gen_c01_v01_t01.wav")
        audioFile.createNewFile()

        val user = mockk<User>()
        every { user.id }.returns(1)
        every { db.allUsers }.returns(listOf(user))
        justRun { db.addTake(any(), any(), any(), any(), any(), any()) }
        every { db.getProject(any(), any(), any()) }.returns(null)
        every { db.getLanguageId(any()) }.returns(1)
        every { db.getLanguage(any()) }.returns(mockk {
            every { slug }.returns("aa")
        })
        every { db.getAnthologyId(any()) }.returns(1)
        every { db.getAnthology(any()) }.returns(mockk {
            every { mask }.returns("1001111111")
            every { regex }.returns("([a-zA-Z]{2,3}[-[\\d\\w]+]*)_([a-zA-Z]{3})_b([\\d]{2})_([1-3]*[a-zA-Z]+)_c([\\d]{2,3})_v([\\d]{2,3})(-([\\d]{2,3}))?(_t([\\d]{2}))?(.wav)?")
            every { matchGroups }.returns("1 2 3 4 5 6 8 10")
        })
        every { db.getBookId(any()) }.returns(1)
        every { db.getBook(any()) }.returns(mockk {
            every { order }.returns(1)
            every { slug }.returns("gen")
        })
        every { db.getVersionId(any()) }.returns(1)
        every { db.getVersion(any()) }.returns(mockk {
            every { slug }.returns("reg")
        })
        every { db.getModeId(any(), any()) }.returns(1)
        every { db.getMode(any()) }.returns(mockk())
        justRun { db.addProject(any()) }

        val importProject = ImportProject(db, directoryProvider)
        importProject(projectDir)

        verify { db.allUsers }
        verify { db.getProject(any(), any(), any()) }
        verify { db.getLanguageId(any()) }
        verify { db.getLanguage(any()) }
        verify { db.getAnthologyId(any()) }
        verify { db.getAnthology(any()) }
        verify { db.getBookId(any()) }
        verify { db.getBook(any()) }
        verify { db.getVersionId(any()) }
        verify { db.getVersion(any()) }
        verify { db.getModeId(any(), any()) }
        verify { db.getMode(any()) }
        verify { db.addProject(any()) }
        verify { db.addTake(any(), any(), any(), any(), any(), any()) }
        verify { directoryProvider.translationsDir }
    }

    @Test
    fun testImportProjectWithNoUserFails() {
        val projectDir = tempFolder.newFolder("project")
        val audioFile = File(projectDir, "aa_reg_b01_gen_c01_v01_t01.wav")
        audioFile.createNewFile()

        every { db.allUsers }.returns(listOf())

        val importProject = ImportProject(db, directoryProvider)
        importProject(projectDir)

        verify { db.allUsers }
        verify(exactly = 0) { db.addProject(any()) }
        verify(exactly = 0) { db.addTake(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun testImportProjectAsFilFails() {
        val projectDir = tempFolder.newFile("project.zip")

        val importProject = ImportProject(db, directoryProvider)
        importProject(projectDir)

        verify(exactly = 0) { db.allUsers }
        verify(exactly = 0) { db.addProject(any()) }
        verify(exactly = 0) { db.addTake(any(), any(), any(), any(), any(), any()) }
    }
}