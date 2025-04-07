package org.wycliffeassociates.translationrecorder.recordingapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.wycliffeassociates.translationrecorder.recordingapp.IntegrationTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.login.utils.ConvertAudio

@RunWith(AndroidJUnit4::class)
@IntegrationTest
class ConvertAudioTest {

    @get:Rule var tempFolder = TemporaryFolder()

    @After
    fun tearDown() {
        tempFolder.delete()
    }

    @Test
    fun convertAudio() {
        val inputFile = tempFolder.newFile("input.wav")
        inputFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        val outputFile = tempFolder.newFile("output.mp4")

        assertEquals(0, outputFile.length())

        val hash = ConvertAudio.convertWavToMp4(inputFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
        assertTrue(hash.isNotEmpty())
    }
}