package org.wycliffeassociates.translationrecorder.FilesPage

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.FilesPage.Export.SimpleProgressCallback
import org.wycliffeassociates.translationrecorder.FilesPage.Export.ZipProject

class ZipProjectTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @MockK lateinit var pm: ProgressMonitor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkConstructor(ZipFile::class)

        every { pm.state }.returns(ProgressMonitor.State.BUSY)
        every { pm.fileName }.returns("test.wav")
        every { pm.percentDone }.returns(45)

        every { anyConstructed<ZipFile>().isRunInThread = any() }.just(runs)
        every { anyConstructed<ZipFile>().progressMonitor }.returns(pm)
        every { anyConstructed<ZipFile>().addFolder(any(), any()) }.just(runs)
        every { anyConstructed<ZipFile>().addFiles(any(), any()) }.just(runs)
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun `test zip project from directory`() {
        val dir = tempFolder.newFolder("project")
        val outFile = tempFolder.newFile("project.zip")

        val callback: SimpleProgressCallback = mockk {
            every { onStart(any()) }.just(runs)
            every { setCurrentFile(any(), any()) }.just(runs)
            every { setUploadProgress(any(), any()) }.just(runs)
        }

        val zipProject = ZipProject(dir)
        zipProject.zip(outFile, callback)

        Thread.sleep(1000)

        verify { callback.onStart(any()) }
        verify { callback.setCurrentFile(any(), any()) }
        verify { callback.setUploadProgress(any(), any()) }

        verify { pm.state }
        verify { pm.fileName }
        verify { pm.percentDone }

        verify { anyConstructed<ZipFile>().isRunInThread = any() }
        verify { anyConstructed<ZipFile>().progressMonitor }
        verify { anyConstructed<ZipFile>().addFolder(any(), any()) }
    }

    @Test
    fun `test zip project from files`() {
        val file1 = tempFolder.newFile("test1.wav")
        val file2 = tempFolder.newFile("test2.wav")
        val outFile = tempFolder.newFile("project.zip")

        val callback: SimpleProgressCallback = mockk {
            every { onStart(any()) }.just(runs)
            every { setCurrentFile(any(), any()) }.just(runs)
            every { setUploadProgress(any(), any()) }.just(runs)
        }

        val zipProject = ZipProject(listOf(file1, file2))
        zipProject.zip(outFile, callback)

        Thread.sleep(1000)

        verify { callback.onStart(any()) }
        verify { callback.setCurrentFile(any(), any()) }
        verify { callback.setUploadProgress(any(), any()) }

        verify { pm.state }
        verify { pm.fileName }
        verify { pm.percentDone }

        verify { anyConstructed<ZipFile>().isRunInThread = any() }
        verify { anyConstructed<ZipFile>().progressMonitor }
        verify { anyConstructed<ZipFile>().addFiles(any(), any()) }
    }
}