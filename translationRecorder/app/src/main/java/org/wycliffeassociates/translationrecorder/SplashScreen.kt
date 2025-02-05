package org.wycliffeassociates.translationrecorder

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.databinding.ActivitySplashBinding
import org.wycliffeassociates.translationrecorder.login.UserActivity
import org.wycliffeassociates.translationrecorder.permissions.PermissionActivity
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 5/5/2016.
 */
@AndroidEntryPoint
class SplashScreen : PermissionActivity() {

    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var initializeApp: InitializeApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.max = 100
        binding.progressBar.isIndeterminate = true
        binding.progressBar.minimumHeight = 8
    }

    override fun onPermissionsAccepted() {
        val initApp = Thread {
            try {
                initializeApp()

                val profile = prefs.getDefaultPref(SettingsActivity.KEY_PROFILE, -1)
                if (profile == -1) {
                    val intent = Intent(this@SplashScreen, UserActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    startActivity(Intent(this@SplashScreen, MainMenu::class.java))
                    finish()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        initApp.start()
    }
}
