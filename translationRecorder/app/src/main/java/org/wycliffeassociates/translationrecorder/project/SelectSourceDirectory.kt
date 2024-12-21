package org.wycliffeassociates.translationrecorder.project

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/**
 * Created by Joe on 3/31/2016.
 */
@AndroidEntryPoint
class SelectSourceDirectory : AppCompatActivity() {

    @Inject lateinit var prefs: IPreferenceRepository

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType("*/*")
        startActivityForResult(intent, SRC_LOC)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, resultData)
        val intent = Intent()
        if (resultCode == RESULT_OK) {
            if (requestCode == SRC_LOC) {
                resultData?.data?.let { uri ->
                    applicationContext.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val uriString = uri.toString()
                    //check magic number of file to see if it matches "aoh!" in ascii. If so, the file is likely a tR file.
                    //safer than just assuming based on the extension
                    try {
                        this.contentResolver.openInputStream(uri).use { inputStream ->
                            inputStream?.let {
                                val magicNumber = ByteArray(4)
                                inputStream.read(magicNumber)
                                val header = String(magicNumber, StandardCharsets.US_ASCII)
                                //aoc was an accident in a previous version
                                if (header == "aoh!" || header == "aoc!") {
                                    prefs.setDefaultPref(Settings.KEY_PREF_GLOBAL_SOURCE_LOC, uriString)
                                    prefs.setDefaultPref(Settings.KEY_SDK_LEVEL, Build.VERSION.SDK_INT)
                                    intent.putExtra(SOURCE_LOCATION, uriString)
                                    intent.putExtra(SDK_LEVEL, Build.VERSION.SDK_INT)
                                }
                            }
                        }
                    } catch (_: FileNotFoundException) {
                    } catch (_: IOException) {
                    }
                }
            }
        }
        setResult(resultCode, intent)
        this.finish()
    }

    companion object {
        const val SOURCE_LOCATION: String = "result_path"
        const val SDK_LEVEL: String = "sdk_level"
        const val SRC_LOC = 42
        const val REQUEST_DIRECTORY = 43
    }
}
