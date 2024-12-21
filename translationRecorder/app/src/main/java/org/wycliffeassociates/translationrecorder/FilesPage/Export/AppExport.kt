package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.multidex.BuildConfig
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import java.io.File

/**
 * Created by sarabiaj on 12/10/2015.
 */
class AppExport(
    exportProject: File,
    project: Project,
    directoryProvider: IDirectoryProvider
) : Export(exportProject, project, directoryProvider) {

    override fun handleUserInput() {
        exportZipApplications(zipFile!!)
    }

    /**
     * Passes zip file URI to relevant audio applications.
     * @param zipFile a list of filenames to be exported
     */
    private fun exportZipApplications(zipFile: File) {
        val shareIntent = Intent(fragment.activity, ShareZipToApps::class.java)
        shareIntent.putExtra("zipPath", zipFile.absolutePath)
        fragment.startActivity(shareIntent)
    }

    class ShareZipToApps : Activity() {
        var mFile: File? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val path = intent.getStringExtra("zipPath")
            val sendIntent = Intent()
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            mFile = File(path)
            val audioUri = FileProvider.getUriForFile(
                this@ShareZipToApps,
                BuildConfig.APPLICATION_ID + ".provider",
                mFile!!
            )
            sendIntent.setAction(Intent.ACTION_SEND)
            //send individual URI
            sendIntent.putExtra(Intent.EXTRA_STREAM, audioUri)
            //open
            sendIntent.setType("application/zip")
            this.startActivityForResult(Intent.createChooser(sendIntent, "Export Zip"), 3)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
            super.onActivityResult(requestCode, resultCode, data)
            if (mFile != null) {
                mFile!!.delete()
            }
        }
    }
}
