package org.wycliffeassociates.translationrecorder.login.utils

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils

import java.io.File
import java.io.FileInputStream

/**
 * Created by sarabiaj on 5/2/2018.
 */

object ConvertAudio {
    private const val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
    private const val COMPRESSED_AUDIO_FILE_BIT_RATE = 64000 // 64kbps
    private const val SAMPLING_RATE = 44100
    private const val CODEC_TIMEOUT_IN_MS = 5000
    private const val BUFFER_SIZE = 44100

    fun convertWavToMp4(inputFile: File, outputFile: File): String {
        FileInputStream(inputFile).use { fis ->
            if (outputFile.exists()) outputFile.delete()

            val mux = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLING_RATE, 1)
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE)

            val codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val outBuffInfo = MediaCodec.BufferInfo()

            val tempBuffer = ByteArray(BUFFER_SIZE)
            var hasMoreData = true
            var presentationTimeUs = 0.0
            var audioTrackIdx = 0
            var totalBytesRead = 0

            do {
                var inputBufIndex = 0
                while (inputBufIndex != -1 && hasMoreData) {
                    inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS.toLong())

                    if (inputBufIndex >= 0) {
                        val dstBuf = codec.getInputBuffer(inputBufIndex)
                        dstBuf?.clear()

                        val bytesRead = fis.read(tempBuffer, 0, dstBuf?.limit() ?: 0)
                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false
                            codec.queueInputBuffer(inputBufIndex, 0, 0, presentationTimeUs.toLong(), MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            totalBytesRead += bytesRead
                            dstBuf?.put(tempBuffer, 0, bytesRead)
                            codec.queueInputBuffer(inputBufIndex, 0, bytesRead, presentationTimeUs.toLong(), 0)
                            presentationTimeUs = (1000000L * (totalBytesRead / 2) / SAMPLING_RATE).toDouble()
                        }
                    }
                }

                // Drain audio
                var outputBufIndex = 0
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS.toLong())
                    if (outputBufIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufIndex)
                        encodedData?.position(outBuffInfo.offset)
                        encodedData?.limit(outBuffInfo.offset + outBuffInfo.size)

                        if (outBuffInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && outBuffInfo.size != 0) {
                            codec.releaseOutputBuffer(outputBufIndex, false)
                        } else {
                            mux.writeSampleData(audioTrackIdx, encodedData!!, outBuffInfo)
                            codec.releaseOutputBuffer(outputBufIndex, false)
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat = codec.outputFormat
                        audioTrackIdx = mux.addTrack(outputFormat)
                        mux.start()
                    }
                }
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM)

            mux.stop()
            mux.release()
        }

        return getHash(outputFile)
    }

    private fun getHash(file: File): String {
        return FileInputStream(file).use {
            String(Hex.encodeHex(DigestUtils.md5(it)))
        }
    }
}
