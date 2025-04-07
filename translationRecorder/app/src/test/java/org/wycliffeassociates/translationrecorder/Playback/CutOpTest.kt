package org.wycliffeassociates.translationrecorder.Playback

import android.app.ProgressDialog
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.Playback.Editing.CutOp
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.nio.ShortBuffer

class CutOpTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var cutOp: CutOp

    @Before
    fun setUp() {
        cutOp = CutOp()
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun testCutAndUndo() {
        cutOp.cut(10, 20)
        assertEquals(1, cutOp.flattenedStack.size)
        assertEquals(10, cutOp.flattenedStack[0].first)
        assertEquals(20, cutOp.flattenedStack[0].second)

        cutOp.undo()
        assertTrue(cutOp.flattenedStack.isEmpty())
    }

    @Test
    fun testClear() {
        cutOp.cut(10, 20)
        cutOp.cut(30, 40)
        assertFalse(cutOp.flattenedStack.isEmpty())

        cutOp.clear()
        assertTrue(cutOp.flattenedStack.isEmpty())
        assertEquals(0, cutOp.sizeFrameCutCmp)
        assertEquals(0, cutOp.sizeFrameCutUncmp)
        assertEquals(0, cutOp.sizeTimeCut)
    }

    @Test
    fun testSkip() {
        cutOp.cut(10, 20)
        assertEquals(20, cutOp.skip(15))
        assertEquals(-1, cutOp.skip(5))
    }

    @Test
    fun testHasCut() {
        assertFalse(cutOp.hasCut())
        cutOp.cut(10, 20)
        assertTrue(cutOp.hasCut())
    }

    @Test
    fun testSkipReverse() {
        cutOp.cut(10, 20)
        assertEquals(10, cutOp.skipReverse(15))
        assertEquals(Integer.MAX_VALUE, cutOp.skipReverse(5))
    }

    @Test
    fun testTotalFramesRemoved_OverlappingCuts() {
        cutOp.cut(10, 20)
        cutOp.cut(15, 25)

        assertEquals(1, cutOp.flattenedStack.size)
        assertEquals(10, cutOp.flattenedStack[0].first)
        assertEquals(25, cutOp.flattenedStack[0].second)
        assertEquals(15, cutOp.sizeFrameCutUncmp)
    }

    @Test
    fun testTotalFramesRemoved_NonOverlappingCuts() {
        cutOp.cut(10, 20)
        cutOp.cut(30, 40)

        assertEquals(2, cutOp.flattenedStack.size)
        assertEquals(10, cutOp.flattenedStack[0].first)
        assertEquals(20, cutOp.flattenedStack[0].second)
        assertEquals(30, cutOp.flattenedStack[1].first)
        assertEquals(40, cutOp.flattenedStack[1].second)
        assertEquals(20, cutOp.sizeFrameCutUncmp)
    }

    @Test
    fun testUncompressedFrameToTime() {
        assertEquals(0, CutOp.uncompressedFrameToTime(0))
        assertEquals(1, CutOp.uncompressedFrameToTime(45))
        assertEquals(2, CutOp.uncompressedFrameToTime(89))
    }

    @Test
    fun testUncompressedToCompressed() {
        assertEquals(0, CutOp.uncompressedToCompressed(0))
        assertEquals(2, CutOp.uncompressedToCompressed(50))
    }

    @Test
    fun testSkipFrame() {
        cutOp.cut(10, 20)
        assertEquals(20, cutOp.skipFrame(15, false))
        assertEquals(-1, cutOp.skipFrame(5, false))
    }

    @Test
    fun testRelativeLocToAbsolute() {
        cutOp.cut(10, 20)
        assertEquals(25, cutOp.relativeLocToAbsolute(15, false))
        assertEquals(5, cutOp.relativeLocToAbsolute(5, false))
    }

    @Test
    fun testCutExistsInRange() {
        cutOp.cut(10, 20)
        assertTrue(cutOp.cutExistsInRange(15, 5))
        assertTrue(cutOp.cutExistsInRange(5, 10))
        assertFalse(cutOp.cutExistsInRange(1, 5))
        assertTrue(cutOp.cutExistsInRange(5, 15))
    }

    @Test
    fun testAbsoluteLocToRelative() {
        cutOp.cut(10, 20)
        assertEquals(5, cutOp.absoluteLocToRelative(5, false))
        assertEquals(15, cutOp.absoluteLocToRelative(15, false))
        assertEquals(25, cutOp.absoluteLocToRelative(35, false))
    }

    @Test
    fun testWriteCut() {
        val testFile = tempFolder.newFile("test2.wav")
        val mockWavFile = spyk(WavFile(testFile), recordPrivateCalls = true) {
            every { file }.returns(tempFolder.newFile("test.wav"))
            every { initializeWavFile() }.just(runs)
            every { totalAudioLength }.returns(1000)
            justRun { this@spyk["finishWrite"](any<Int>()) }
        }
        val mockBuffer = mockk<ShortBuffer>()
        val mockProgressDialog = mockk<ProgressDialog> {
            every { progress = any() }.just(runs)
            every { incrementProgressBy(any()) }.just(runs)
        }

        every { mockBuffer.capacity() } returns 100
        every { mockBuffer.get(any<Int>()) } returns 100.toShort() // Provide a default return value

        cutOp.cut(20, 30)

        cutOp.writeCut(mockWavFile, mockBuffer, mockProgressDialog)

        verify { mockBuffer.capacity() }
        verify(atLeast = 1) { mockBuffer.get(any<Int>()) } // Verify get was called at least once
    }

    @Test
    fun testGetSizes() {
        cutOp.cut(100000, 200000)

        assertNotEquals(0, cutOp.sizeFrameCutCmp)
        assertNotEquals(0, cutOp.sizeFrameCutUncmp)
        assertNotEquals(0, cutOp.sizeTimeCut)
    }
}