package org.wycliffeassociates.translationrecorder.Recording

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getNameWithoutExtension
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.io.IOException

/**
 * Created by sarabiaj on 3/10/2016.
 */
class InsertTaskFragment : Fragment() {
    interface Insert {
        fun writeInsert(base: WavFile, insertClip: WavFile, insertLoc: Int)
    }

    private var mCtx: RecordingActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCtx = context as RecordingActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun writeInsert(base: WavFile, insertClip: WavFile, insertFrame: Int) {
        val write = Thread {
            try {
                val result = WavFile.insertWavFile(base, insertClip, insertFrame)
                insertClip.file.delete()
                val dir = File(mCtx!!.externalCacheDir, "Visualization")
                val vis = File(
                    dir,
                    getNameWithoutExtension(insertClip.file) + ".vis"
                )
                vis.delete()
                result.file.renameTo(insertClip.file)
                mCtx!!.insertCallback(WavFile(insertClip.file))
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
        write.start()
    }
}