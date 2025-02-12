package org.wycliffeassociates.translationrecorder.Playback

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

object AudioMerger {

    /**
     * Merges a list of audio files into a single file.
     * @param inputFiles The list of audio files to merge.
     * @param output The file to write the merged audio to.
     */
    fun merge(
        inputFiles: List<File>,
        output: File
    ):Boolean {
        val filterComplex = StringBuilder()
        inputFiles.indices.forEach {
            filterComplex.append("[")
                .append(it)
                .append(":a:0]")
        }
        filterComplex.append("concat=n=")
            .append(inputFiles.size)
            .append(":v=0:a=1[out]")

        val command = arrayListOf("-y")
        inputFiles.forEach {
            command.add("-i")
            command.add(it.absolutePath)
        }
        command.add("-filter_complex")
        command.add(filterComplex.toString())
        command.add("-map")
        command.add("[out]")
        command.add(output.absolutePath)

        val execSession = FFmpegKit.executeWithArguments(command.toTypedArray())
        return ReturnCode.isSuccess(execSession.returnCode)
    }
}