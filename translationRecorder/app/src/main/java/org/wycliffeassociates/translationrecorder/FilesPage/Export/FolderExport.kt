package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.gson.Gson
import org.wycliffeassociates.translationrecorder.FilesPage.Manifest
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getProjectDirectory
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException


/**
 * Created by sarabiaj on 12/10/2015.
 */
class FolderExport(
    project: Project,
    directoryProvider: IDirectoryProvider,
    db: IProjectDatabaseHelper,
    assetsProvider: AssetsProvider
) : Export(project, directoryProvider, db, assetsProvider) {

    override fun handleUserInput() {
        val i = Intent(fragment.activity, StorageAccess::class.java)
        try {
            i.putExtra("export_project", directoryToZip!!.canonicalPath)
            i.putExtra("zip_path", zipFile!!.canonicalPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        fragment.startActivity(i)
    }

    class StorageAccess : Activity() {
        private var mCurrentUri: Uri? = null
        private var mZipPath: File? = null
        private val SAVE_FILE = 43

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val intent = intent
            mZipPath = File(intent.getStringExtra("zip_path"))
            createFile("application/zip", mZipPath!!.name)
        }

        /**
         * Closes the activity on a back press to return back to the files page
         */
        override fun onBackPressed() {
            this.finish()
        }

        /**
         * Receives the user selected location to save to as a Uri
         * @param requestCode should be set to SAVE_FILE to continue with the export
         * @param resultCode equals RESULT_OK if chosing a location completed
         * @param resultData contains the Uri to save to
         */
        public override fun onActivityResult(
            requestCode: Int, resultCode: Int,
            resultData: Intent
        ) {
            mCurrentUri = null
            if (resultCode == RESULT_OK) {
                if (requestCode == SAVE_FILE) {
                    mCurrentUri = resultData.data
                    savefile(mCurrentUri!!, mZipPath!!)
                    mZipPath = null //reset
                }

                if (requestCode == 3) { //delete zip file, needs to be done after upload
                    mZipPath = null //set null for next time
                }
            }
            finish()
        }

        /**
         * Copies a file from a path to a uri
         * @param destUri The destination of the file
         * @param zippedProject The original path to the file
         */
        fun savefile(destUri: Uri, zippedProject: File) {
            try {
                contentResolver.openOutputStream(destUri).use { outputStream ->
                    FileInputStream(zippedProject).use { inputStream ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while ((inputStream.read(buffer).also { length = it }) > 0) {
                            checkNotNull(outputStream)
                            outputStream.write(buffer, 0, length)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            zippedProject.delete()
            this.finish()
        }

        /**
         * Creates a file in folder selected by user
         * @param mimeType Typically going to be "audio/ *" for this app
         * @param fileName The name of the file selected.
         */
        private fun createFile(mimeType: String, fileName: String) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType(mimeType)
            intent.putExtra(Intent.EXTRA_TITLE, fileName)
            startActivityForResult(intent, SAVE_FILE)
        }
    }
}
