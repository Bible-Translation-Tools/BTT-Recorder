package org.wycliffeassociates.translationrecorder.Recording

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.AudioInfo
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.wav.WavFile
import org.wycliffeassociates.translationrecorder.wav.WavOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.LinkedList
import javax.inject.Inject

@AndroidEntryPoint
class WavFileWriter : Service() {

    @Inject lateinit var directoryProvider: IDirectoryProvider

    private var nameWithoutExtension: String? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val audioFile = intent.getParcelableExtra<WavFile>(KEY_WAV_FILE)!!
        nameWithoutExtension = audioFile.file.nameWithoutExtension

        val writingThread = Thread {
            var stopped = false

            try {
                WavOutputStream(audioFile, true).use { rawAudio ->
                    //WARNING DO NOT USE BUFFERED OUTPUT HERE, WILL CAUSE END LINE TO BE OFF IN PLAYBACK
                    while (!stopped) {
                        val message = RecordingQueues.writingQueue.take()
                        if (message.isStopped) {
                            Logger.w(
                                this.toString(),
                                "raw audio thread received a stop message"
                            )
                            stopped = true
                        } else {
                            if (!message.isPaused) {
                                rawAudio.write(message.data)
                            } else {
                                Logger.w(
                                    this.toString(),
                                    "raw audio thread received a onPauseRecording message"
                                )
                            }
                        }
                    }
                    Logger.w(this.toString(), "raw audio thread exited loop")
                    RecordingQueues.writingQueue.clear()
                    Logger.e(this.toString(), "raw audio queue finishing, sending done message")
                }
            } catch (e: FileNotFoundException) {
                Logger.e(this.toString(), "File not found exception in writing thread", e)
                e.printStackTrace()
            } catch (e: InterruptedException) {
                Logger.e(this.toString(), "Interrupted Exception in writing queue", e)
                e.printStackTrace()
            } catch (e: IOException) {
                Logger.e(this.toString(), "IO Exception in writing queue", e)
                e.printStackTrace()
            } finally {
                try {
                    RecordingQueues.doneWriting.put(java.lang.Boolean.TRUE)
                } catch (e: InterruptedException) {
                    Logger.e(
                        this.toString(),
                        "InterruptedException in finally of writing queue",
                        e
                    )
                    e.printStackTrace()
                }
            }
        }

        val compressionThread = Thread {
            Logger.w(this.toString(), "starting compression thread")

            var stopped = false
            val byteArrayList = LinkedList<Byte>()

            try {
                while (!stopped) {
                    val message = RecordingQueues.compressionQueue.take()
                    if (message.isStopped) {
                        Logger.w(this.toString(), "Compression thread received a stop message")
                        stopped = true
                        Logger.w(this.toString(), "Compression thread writing remaining data")
                        writeDataReceivedSoFar(byteArrayList, true)
                    } else {
                        if (!message.isPaused) {
                            val dataFromQueue = message.data
                            for (x in dataFromQueue) {
                                byteArrayList.add(x)
                            }
                            if (byteArrayList.size >= AudioInfo.COMPRESSION_RATE) {
                                writeDataReceivedSoFar(byteArrayList, false)
                            }
                        } else {
                            Logger.w(
                                this.toString(),
                                "Compression thread received a onPauseRecording message"
                            )
                        }
                    }
                }
                Logger.w(this.toString(), "exited compression thread loop")
                RecordingQueues.compressionQueue.clear()
                Logger.w(this.toString(), "Compression queue finishing, sending done message")
            } catch (e: FileNotFoundException) {
                Logger.e(this.toString(), "File not found exception in compression thread", e)
                e.printStackTrace()
            } catch (e: InterruptedException) {
                Logger.e(this.toString(), "Interrupted Exception in compression queue", e)
                e.printStackTrace()
            } catch (e: IOException) {
                Logger.e(this.toString(), "IO Exception in compression queue", e)
                e.printStackTrace()
            } finally {
                try {
                    RecordingQueues.doneCompressing.put(java.lang.Boolean.TRUE)
                } catch (e: InterruptedException) {
                    Logger.e(
                        this.toString(),
                        "InterruptedException in finally of Compression queue",
                        e
                    )
                    e.printStackTrace()
                }
            }
        }

        val compressionWriterThread = Thread {
            var stopped = false

            val file = File(directoryProvider.visualizationDir, "$nameWithoutExtension.vis")
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                Logger.w(this.toString(), "created a new vis file")
            }
            try {
                FileOutputStream(file).use { fos ->
                    BufferedOutputStream(fos).use { compressedFile ->
                        compressedFile.write(ByteArray(4))
                        while (!stopped) {
                            val message = RecordingQueues.compressionWriterQueue.take()
                            if (message.isStopped) {
                                Logger.w(
                                    this.toString(),
                                    "raw audio thread received a stop message"
                                )
                                stopped = true
                            } else {
                                if (!message.isPaused) {
                                    compressedFile.write(message.data)
                                } else {
                                    Logger.w(
                                        this.toString(),
                                        "raw audio thread received a onPauseRecording message"
                                    )
                                }
                            }
                        }
                        compressedFile.flush()
                        RandomAccessFile(file, "rw").use { raf ->
                            raf.seek(0)
                            raf.write('D'.code)
                            raf.write('O'.code)
                            raf.write('N'.code)
                            raf.write('E'.code)
                        }
                        Logger.w(this.toString(), "raw audio thread exited loop")
                        RecordingQueues.compressionWriterQueue.clear()
                        Logger.e(
                            this.toString(),
                            "raw audio queue finishing, sending done message"
                        )
                    }
                }
            } catch (e: FileNotFoundException) {
                Logger.e(this.toString(), "File not found exception in writing thread", e)
                e.printStackTrace()
            } catch (e: InterruptedException) {
                Logger.e(this.toString(), "Interrupted Exception in writing queue", e)
                e.printStackTrace()
            } catch (e: IOException) {
                Logger.e(this.toString(), "IO Exception in writing queue", e)
                e.printStackTrace()
            } finally {
                try {
                    RecordingQueues.doneWritingCompressed.put(java.lang.Boolean.TRUE)
                } catch (e: InterruptedException) {
                    Logger.e(
                        this.toString(),
                        "InterruptedException in finally of writing queue",
                        e
                    )
                    e.printStackTrace()
                }
            }
        }
        compressionThread.start()
        compressionWriterThread.start()
        writingThread.start()

        return START_STICKY
    }

    @Throws(IOException::class)
    private fun writeDataReceivedSoFar(list: MutableList<Byte>, stoppedRecording: Boolean) {
        val data = ByteArray(AudioInfo.COMPRESSION_RATE)
        val minAndMax = ByteArray(2 * AudioInfo.SIZE_OF_SHORT)
        //while there is more data in the arraylist than one increment
        while (list.size >= AudioInfo.COMPRESSION_RATE) {
            //remove that data and put it in an array for min/max computation
            for (i in 0 until AudioInfo.COMPRESSION_RATE) {
                data[i] = list.removeAt(0)
            }
            //write the min/max to the minAndMax array
            getMinAndMaxFromArray(data, minAndMax)
        }
        //if the recording was stopped and there is less data than a full increment, grab the remaining data
        if (stoppedRecording) {
            println("Stopped recording, writing some remaining data")
            val remaining = ByteArray(list.size)
            for (i in list.indices) {
                remaining[i] = list.removeAt(0)
            }
            getMinAndMaxFromArray(remaining, minAndMax)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun getMinAndMaxFromArray(data: ByteArray, minAndMax: ByteArray) {
        if (data.size < 4) {
            return
        }
        var max = Int.MIN_VALUE
        var min = Int.MAX_VALUE
        var minIdx = 0
        var maxIdx = 0
        var j = 0
        while (j < data.size) {
            if ((j + 1) < data.size) {
                val low = data[j]
                val hi = data[j + 1]
                val value =
                    (((hi.toInt() shl 8) and 0x0000FF00) or (low.toInt() and 0x000000FF)).toShort()
                if (max < value) {
                    max = value.toInt()
                    maxIdx = j
                    if (value > largest) {
                        largest = value.toInt()
                    }
                }
                if (min > value) {
                    min = value.toInt()
                    minIdx = j
                }
            }
            j += AudioInfo.SIZE_OF_SHORT
        }
        minAndMax[0] = data[minIdx]
        minAndMax[1] = data[minIdx + 1]
        minAndMax[2] = data[maxIdx]
        minAndMax[3] = data[maxIdx + 1]
        try {
            RecordingQueues.UIQueue.put(RecordingMessage(minAndMax, false, false))
            RecordingQueues.compressionWriterQueue.put(RecordingMessage(minAndMax, false, false))
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val KEY_WAV_FILE: String = "wavfile"
        var largest: Int = 0

        fun getIntent(ctx: Context?, wavFile: WavFile?): Intent {
            val intent = Intent(ctx, WavFileWriter::class.java)
            intent.putExtra(KEY_WAV_FILE, wavFile as Parcelable?)
            return intent
        }
    }
}
