package org.wycliffeassociates.translationrecorder.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SplashScreen
import java.util.concurrent.atomic.AtomicBoolean

abstract class PermissionActivity : AppCompatActivity() {

    protected val requestingPermission = AtomicBoolean(false)

    private val corePermissions = mutableSetOf(
        Manifest.permission.RECORD_AUDIO
    )
    private val optionalPermissions = mutableSetOf<String>()

    // Opens an activity to guide the user to enabling settings from the settings app
    // this is called when the user has checked "never ask again"
    private fun presentPermissionDialog() {
        startActivity(Intent(this, PermissionsDialogActivity::class.java))
    }

    /**
     * This method should replace onResume for activities that subclass PermissionActivity.
     *
     * This is due to needing a callback in onResume before safely executing further code.
     */
    protected abstract fun onPermissionsAccepted()

    override fun onStart() {
        super.onStart()

        requestingPermission.set(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!optionalPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                optionalPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val permissions = arrayListOf<String>()
        permissions.addAll(corePermissions)
        permissions.addAll(optionalPermissions)

        PermissionX.init(this)
            .permissions(permissions)
            .onForwardToSettings { scope, deniedList ->
                val deniedCorePermissions = deniedCorePermissions(deniedList)
                if (deniedCorePermissions.isNotEmpty()) {
                    scope.showForwardToSettingsDialog(
                        deniedCorePermissions,
                        getString(R.string.permissions_denied_message),
                        getString(R.string.open_permissions),
                        getString(R.string.title_cancel)
                    )
                } else {
                    permissionsAccepted()
                }
            }
            .request { _, _, _ ->
                val allGranted = allPermissionsGranted()
                val deniedList = deniedPermissions()
                when {
                    allGranted -> permissionsAccepted()
                    deniedOptionalPermissionsOnly(deniedList) -> permissionsAccepted()
                    else -> restart()
                }
            }
    }

    private fun deniedOptionalPermissionsOnly(deniedList: List<String>): Boolean {
        return deniedCorePermissions(deniedList).isEmpty()
    }

    private fun deniedCorePermissions(deniedList: List<String>): List<String> {
        return deniedList.subtract(optionalPermissions).toList()
    }

    private fun restart() {
        finishAffinity()
        val intent = Intent(this, SplashScreen::class.java)
        startActivity(intent)
    }

    private fun permissionsAccepted() {
        requestingPermission.set(false)
        onPermissionsAccepted()
    }

    private fun allPermissionsGranted(): Boolean {
        return corePermissions.plus(optionalPermissions).all { permission ->
            PermissionX.isGranted(this, permission)
        }
    }

    private fun deniedPermissions(): List<String> {
        return corePermissions.plus(optionalPermissions).filter {
            !PermissionX.isGranted(this, it)
        }
    }
}