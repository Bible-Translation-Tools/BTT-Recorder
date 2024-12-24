package org.wycliffeassociates.translationrecorder.project

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import javax.inject.Inject

/**
 * Created by Joe on 3/31/2016.
 */
@AndroidEntryPoint
class SelectSourceDirectory : AppCompatActivity() {

    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider

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
                    Utils.copySourceAudio(this, directoryProvider, uri)?.let { target ->
                        prefs.setDefaultPref(Settings.KEY_PREF_GLOBAL_SOURCE_LOC, target.absolutePath)
                        prefs.setDefaultPref(Settings.KEY_SDK_LEVEL, Build.VERSION.SDK_INT)
                        intent.putExtra(SOURCE_LOCATION, target.absolutePath)
                        intent.putExtra(SDK_LEVEL, Build.VERSION.SDK_INT)
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
