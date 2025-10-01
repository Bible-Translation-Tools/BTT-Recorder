package org.wycliffeassociates.translationrecorder.login

import android.media.MediaCodec
import android.media.MediaFormat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wycliffeassociates.translationrecorder.login.utils.ConvertAudio
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer

class ConvertAudioTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(MediaFormat::class)
        mockkStatic(MediaCodec::class)
    }

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun `test convert audio`() {
        val inputFile = tempFolder.newFile("test.wav")
        val outputFile = File("test.mp4")

        val mf: MediaFormat = mockk {
            every { setInteger(any(), any()) }.just(runs)
        }
        every { MediaFormat.createAudioFormat(any(), any(), any()) }.returns(mf)

        val inputBuffer = ByteBuffer.allocate(1)
        val outputBuffer = ByteBuffer.allocate(1)

        var dequeInputCount = 0
        var dequeOutputCount = 0

        val mc: MediaCodec = mockk {
            every { configure(any(), null, null, any()) }.just(runs)
            every { start() }.just(runs)
            every { getInputBuffer(any()) }.returns(inputBuffer)
            every { getOutputBuffer(any()) }.returns(outputBuffer)
            every { dequeueInputBuffer(any()) }.answers {
                when (dequeInputCount) {
                    0 -> {
                        dequeInputCount++
                        0
                    }
                    else -> {
                        -1
                    }
                }
            }
            every { queueInputBuffer(any<Int>(), any<Int>(), any<Int>(), any(), any<Int>()) }.just(runs)
            every { dequeueOutputBuffer(any(), any()) }.answers {
                when (dequeOutputCount) {
                    0 -> {
                        dequeOutputCount++
                        0
                    }
                    else -> {
                        val info = firstArg<MediaCodec.BufferInfo>()
                        info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        -1
                    }
                }
            }
            every { releaseOutputBuffer(0, false) }.just(runs)
        }
        every { MediaCodec.createEncoderByType(any()) }.returns(mc)

        assertThrows(FileNotFoundException::class.java) {
            ConvertAudio.convertWavToMp4(inputFile, outputFile)
        }

        verify { mf.setInteger(any(), any()) }

        verify { MediaFormat.createAudioFormat(any(), any(), any()) }

        verify { mc.configure(any(), null, null, any()) }
        verify { mc.start() }
        verify { mc.getInputBuffer(any()) }
        verify { mc.getOutputBuffer(any()) }
        verify { mc.dequeueInputBuffer(any()) }
        verify { mc.queueInputBuffer(any<Int>(), any<Int>(), any<Int>(), any(), any<Int>()) }
        verify { mc.dequeueOutputBuffer(any(), any()) }
        verify { mc.releaseOutputBuffer(0, false) }

        verify { MediaCodec.createEncoderByType(any()) }
    }
}