package org.wycliffeassociates.translationrecorder.Recording

import android.content.Context
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONException
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getNameWithoutExtension
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 3/10/2016.
 */
@AndroidEntryPoint
class InsertTaskFragment : Fragment() {
    @Inject lateinit var directoryProvider: IDirectoryProvider

    interface Insert {
        fun writeInsert(base: WavFile, insertClip: WavFile, insertLoc: Int)
    }

    private var mCtx: RecordingActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCtx = context as RecordingActivity
    }

    fun writeInsert(base: WavFile, insertClip: WavFile, insertFrame: Int) {
        val write = Thread {
            try {
                val result = WavFile.insertWavFile(base, insertClip, insertFrame)
                insertClip.file.delete()
                val dir = File(directoryProvider.externalCacheDir, "Visualization")
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