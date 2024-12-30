package org.wycliffeassociates.translationrecorder.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.concurrent.atomic.AtomicBoolean

abstract class PermissionActivity : AppCompatActivity() {

    protected val requestingPermission = AtomicBoolean(false)

    // Opens an activity to guide the user to enabling settings from the settings app
    // this is called when the user has checked "never ask again"
    fun presentPermissionDialog() {
        startActivity(Intent(this, PermissionsDialogActivity::class.java))
    }

    /**
     * This method should replace onResume for activities that subclass PermissionActivity.
     *
     * This is due to needing a callback in onResume before safely executing further code.
     */
    protected abstract fun onPermissionsAccepted()

    override fun onResume() {
        super.onResume()
        requestingPermission.set(true)

        val permissions = arrayListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val wasDenied = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            // Request POST_NOTIFICATIONS only once
            if (!wasDenied) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        Dexter.withActivity(this)
                .withPermissions(permissions)
                .withListener(
                        object : MultiplePermissionsListener {
                            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                                if (report.areAllPermissionsGranted()) {
                                    requestingPermission.set(false)
                                    onPermissionsAccepted()
                                } else {
                                    val denied = report.deniedPermissionResponses
                                        .map { it.permissionName }

                                    // Skip POST_NOTIFICATIONS if the user has denied it
                                    if (denied.size == 1 && denied.first() == Manifest.permission.POST_NOTIFICATIONS) {
                                        requestingPermission.set(false)
                                        onPermissionsAccepted()
                                    } else {
                                        requestingPermission.set(false)
                                        presentPermissionDialog()
                                    }
                                }
                            }
                            override fun onPermissionRationaleShouldBeShown(
                                    permissions: List<PermissionRequest>,
                                    token: PermissionToken)
                            {
                                token.continuePermissionRequest()
                            }
                        }
                ).check()
    }
}