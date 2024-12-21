package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.door43.tools.reporting.Logger
import net.gotev.uploadservice.BinaryUploadRequest
import net.gotev.uploadservice.ServerResponse
import net.gotev.uploadservice.UploadInfo
import net.gotev.uploadservice.UploadNotificationConfig
import net.gotev.uploadservice.UploadServiceSingleBroadcastReceiver
import net.gotev.uploadservice.UploadStatusDelegate
import org.wycliffeassociates.translationrecorder.FilesPage.FeedbackDialog
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project
import java.io.File
import java.util.UUID

/**
 * Created by sarabiaj on 11/16/2017.
 */
class TranslationExchangeExport(
    projectToExport: File,
    project: Project,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider,
    private val prefs: IPreferenceRepository,
    private val assetsProvider: AssetsProvider
) : Export(projectToExport, project, directoryProvider), UploadStatusDelegate {

    private var differ: TranslationExchangeDiff? = null
    private var uploadReceiver: UploadServiceSingleBroadcastReceiver

    init {
        directoryToZip = null
        uploadReceiver = UploadServiceSingleBroadcastReceiver(this)
    }

    override fun initialize() {
        differ = TranslationExchangeDiff(
            project,
            db,
            directoryProvider,
            assetsProvider
        ).apply {
            computeDiff(outputFile(), this@TranslationExchangeExport)
        }
    }

    override fun onStart(id: Int) {
        handler.post {
            if (id == TranslationExchangeDiff.DIFF_ID) {
                progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_one))
            } else if (id == ZipProject.ZIP_PROJECT_ID) {
                progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_two))
            } else if (id == EXPORT_UPLOAD_ID) {
                progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_three))
            }
            zipDone = false
            progressCallback?.setZipping(true)
            progressCallback?.showProgress(ProgressUpdateCallback.ZIP)
        }
    }

    override fun onComplete(id: Int) {
        super.onComplete(id)
        if (id == TranslationExchangeDiff.DIFF_ID) {
            filesToZip = differ?.diff
            super.initialize()
        } else if (id == EXPORT_UPLOAD_ID) {
            progressCallback?.setProgressTitle(null)
            progressCallback?.setZipping(false)
        }
    }

    override fun handleUserInput() {
        val thread = Thread {
            val ctx = fragment.requireActivity().applicationContext
            uploadBinary(ctx, outputFile())
        }
        thread.start()
    }

    fun uploadBinary(context: Context, file: File) {
        try {
            this.onStart(EXPORT_UPLOAD_ID)

            val uploadServer = prefs.getDefaultPref(
                Settings.KEY_PREF_UPLOAD_SERVER,
                "opentranslationtools.org"
            )

            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            val filePath = file.absolutePath
            val userId = prefs.getDefaultPref(Settings.KEY_USER, 1)
            val user = db.getUser(userId)
            val hash = user.hash

            val uploadId = UUID.randomUUID().toString()
            uploadReceiver.setUploadID(uploadId)
            uploadReceiver.register(context)

            BinaryUploadRequest(context, uploadId, "http://$uploadServer/api/upload/zip")
                .addHeader("tr-user-hash", hash)
                .addHeader("tr-file-name", file.name)
                .setFileToUpload(filePath)
                .setNotificationConfig(notificationConfig)
                .setDelegate(null)
                .setAutoDeleteFilesAfterSuccessfulUpload(true)
                .setMaxRetries(30)
                .startUpload()
        } catch (exc: Exception) {
            Log.e("AndroidUploadService", exc.message, exc)
        }
    }

    protected val notificationConfig: UploadNotificationConfig
        get() {
            val config = UploadNotificationConfig()

            config.progress.iconResourceID = R.drawable.ic_upload
            config.progress.iconColorResourceID = Color.BLUE

            config.completed.iconResourceID = R.drawable.ic_upload_success
            config.completed.iconColorResourceID = Color.GREEN

            config.error.iconResourceID = R.drawable.ic_upload_error
            config.error.iconColorResourceID = Color.RED

            config.cancelled.iconResourceID = R.drawable.ic_cancelled
            config.cancelled.iconColorResourceID = Color.YELLOW

            return config
        }

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        this.setUploadProgress(EXPORT_UPLOAD_ID, uploadInfo.progressPercent)
    }

    override fun onCompleted(
        context: Context,
        uploadInfo: UploadInfo,
        serverResponse: ServerResponse
    ) {
        val fd = FeedbackDialog.newInstance(
            this.fragment.getString(R.string.project_upload),
            this.fragment.getString(R.string.project_uploaded)
        )
        fd.show(this.fragment.parentFragmentManager, "title")

        zipFile?.delete()

        Logger.e(
            TranslationExchangeExport::class.java.toString(),
            "code: " + serverResponse.httpCode + " " + serverResponse.bodyAsString
        )
        this.onComplete(EXPORT_UPLOAD_ID)
    }

    override fun onError(
        context: Context,
        uploadInfo: UploadInfo,
        serverResponse: ServerResponse?,
        exception: Exception?
    ) {
        val message: String?
        if (serverResponse != null) {
            message = String.format(
                "code: %s: %s",
                serverResponse.httpCode,
                serverResponse.bodyAsString
            )
            Logger.e(TranslationExchangeExport::class.java.toString(), message, exception)
        } else if (exception != null) {
            message = exception.message
            Logger.e(
                TranslationExchangeExport::class.java.toString(),
                "Error: $message", exception
            )
        } else {
            message = ("An error occurred without a response or exception, upload percent is "
                    + uploadInfo.progressPercent)
            Logger.e(TranslationExchangeExport::class.java.toString(), message)
        }

        val fd = FeedbackDialog.newInstance(
            this.fragment.getString(R.string.project_upload),
            this.fragment.getString(R.string.project_upload_failed, message)
        )
        fd.show(this.fragment.parentFragmentManager, "UPLOAD_FEEDBACK")
        this.onComplete(EXPORT_UPLOAD_ID)
    }

    override fun onCancelled(context: Context, uploadInfo: UploadInfo?) {
        Logger.e(TranslationExchangeExport::class.java.toString(), "Cancelled upload")
        if (uploadInfo != null) {
            Logger.e(
                TranslationExchangeExport::class.java.toString(),
                "Upload percent was " + uploadInfo.progressPercent
            )
        }
        this.onComplete(EXPORT_UPLOAD_ID)
    }

    companion object {
        var EXPORT_UPLOAD_ID: Int = 3
    }
}
