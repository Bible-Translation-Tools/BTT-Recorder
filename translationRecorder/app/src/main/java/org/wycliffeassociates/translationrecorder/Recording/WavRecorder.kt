package org.wycliffeassociates.translationrecorder.Recording

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.AudioInfo
import kotlin.concurrent.Volatile


/**
 * Contains the ability to record audio and output to a .wav file.
 * The file is written to a temporary .raw file, then upon a stop call
 * the file is copied into a .wav file with a UUID name.
 *
 *
 * Recorded files can be renamed with a call to toSave()
 */
class WavRecorder : Service() {

    private lateinit var recorder: AudioRecord

    private var bufferSize = 0

    @Volatile
    private var isRecording = false
    private var mVolumeTest = false
    private var permissionsError = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mVolumeTest = intent.getBooleanExtra(KEY_VOLUME_TEST, false)
        if (!isRecording) {
            record()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRecording = false
        if (!permissionsError) {
            recorder.stop()
            recorder.release()
        }
        super.onDestroy()
    }

    private fun record() {
        try {
            val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                permissionsError = true
                startActivity(
                    Intent(this, PermissionsDeniedActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }

            bufferSize = AudioRecord.getMinBufferSize(
                AudioInfo.SAMPLERATE,
                AudioInfo.CHANNEL_TYPE,
                AudioInfo.ENCODING
            )
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioInfo.SAMPLERATE,
                AudioInfo.CHANNEL_TYPE,
                AudioInfo.ENCODING,
                bufferSize
            )
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
            }

            isRecording = true

            Thread(::feedToQueues, "AudioRecorder Thread").start()
        } catch (e: IllegalArgumentException) {
            //The lenovo tab 2 can deny app permissions in a weird way and will cause setting up the
            //AudioRecord object to throw an illegal argument exception, and crash the app. It will report
            //having the permission, so the only way to check for it being denied is to check for this exception
            //In this case, start the following activity to provide a dialog to the user
            permissionsError = true
            startActivity(
                Intent(this, PermissionsDeniedActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun feedToQueues() {
        val data = ByteArray(bufferSize)
        var read: Int
        while (isRecording) {
            read = recorder.read(data, 0, bufferSize)

            if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                val temp = RecordingMessage(data, false, false)
                try {
                    if (mVolumeTest) {
                        RecordingQueues.UIQueue.put(temp)
                    }
                    RecordingQueues.writingQueue.put(temp)
                    RecordingQueues.compressionQueue.put(temp)
                } catch (e: InterruptedException) {
                    Logger.e(this.toString(), "InterruptedException in feeding to queues", e)
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        var KEY_VOLUME_TEST: String = "key_volume_test"
    }
}