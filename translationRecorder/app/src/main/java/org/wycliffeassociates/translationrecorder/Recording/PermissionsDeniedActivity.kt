package org.wycliffeassociates.translationrecorder.Recording

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Created by sarabiaj on 3/23/2017.
 */
class PermissionsDeniedActivity : Activity() {
    /**
     * This Activity is for the app permissions functionality of the Lenovo Tab 2.
     * App permissions for tablets with Marshmallow or higher will likely find this setting under the app's info page
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle("Record Audio Permission Denied")
            .setMessage("Could not start recording, check your Android security settings and enable permissions to Record Audio.")
            .setPositiveButton(
                "Click here to go to Security Settings"
            ) { _, _ ->
                val intent = Intent()
                intent.setAction(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
            }
            .setOnDismissListener { finish() }
            .create()
            .show()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}
