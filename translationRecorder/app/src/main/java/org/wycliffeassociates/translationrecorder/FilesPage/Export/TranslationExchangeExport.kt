package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.door43.tools.reporting.Logger
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import org.wycliffeassociates.translationrecorder.FilesPage.FeedbackDialog
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project
import java.io.File

/**
 * Created by sarabiaj on 11/16/2017.
 */
class TranslationExchangeExport(
    project: Project,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider,
    private val prefs: IPreferenceRepository,
    private val assetsProvider: AssetsProvider,
) : Export(project, directoryProvider), RequestObserverDelegate {

    private var differ: TranslationExchangeDiff? = null

    init {
        directoryToZip = null
    }

    override fun initialize() {
        differ = TranslationExchangeDiff(
            project,
            db,
            directoryProvider,
            assetsProvider,
            prefs
        ).apply {
            outputFile()
            computeDiff(this@TranslationExchangeExport)
        }
    }

    override fun onStart(id: Int) {
        handler.post {
            when (id) {
                TranslationExchangeDiff.DIFF_ID -> {
                    progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_one))
                }
                ZipProject.ZIP_PROJECT_ID -> {
                    progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_two))
                }
                EXPORT_UPLOAD_ID -> {
                    progressCallback?.setProgressTitle(fragment.getString(R.string.upload_step_three))
                }
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

    private fun uploadBinary(context: Context, file: File) {
        try {
            this.onStart(EXPORT_UPLOAD_ID)

            val uploadServer = prefs.getDefaultPref(
                SettingsActivity.KEY_PREF_UPLOAD_SERVER,
                "http://opentranslationtools.org"
            )

            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            val filePath = file.absolutePath
            val userId = prefs.getDefaultPref(SettingsActivity.KEY_PROFILE, 1)
            val user = db.getUser(userId)!!
            val hash = user.hash!!

            val request = BinaryUploadRequest(context, "$uploadServer/api/upload/zip")
                .addHeader("tr-user-hash", hash)
                .addHeader("tr-file-name", file.name)
                .setFileToUpload(filePath)
                .setNotificationConfig { _, _ ->  notificationConfig}
                .setAutoDeleteFilesAfterSuccessfulUpload(true)
                .setMaxRetries(30)

            Handler(Looper.getMainLooper()).post {
                request.subscribe(fragment.requireContext(), fragment, this)
            }
        } catch (exc: Exception) {
            Log.e("AndroidUploadService", exc.message, exc)
        }
    }

    private val notificationConfig: UploadNotificationConfig
        get() {
            val progressConfig = UploadNotificationStatusConfig(
                "Progress",
                "Upload progress.",
                R.drawable.ic_upload,
                Color.BLUE
            )
            val successConfig = UploadNotificationStatusConfig(
                "Success",
                "Successfully uploaded.",
                R.drawable.ic_upload_success,
                Color.GREEN
            )
            val errorConfig = UploadNotificationStatusConfig(
                "Error",
                "An error occurred.",
                R.drawable.ic_upload_error,
                Color.RED
            )
            val cancelConfig = UploadNotificationStatusConfig(
                "Canceled",
                "Canceled by the user.",
                R.drawable.ic_cancelled,
                Color.YELLOW
            )

            val config = UploadNotificationConfig(
                TranslationRecorderApp.NOTIFICATION_CHANNEL_ID,
                true,
                progressConfig,
                successConfig,
                errorConfig,
                cancelConfig
            )
            return config
        }

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        this.setUploadProgress(EXPORT_UPLOAD_ID, uploadInfo.progressPercent)
    }

    override fun onSuccess(
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
            "code: " + serverResponse.code + " " + serverResponse.bodyString
        )
        this.onComplete(EXPORT_UPLOAD_ID)
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
    }

    override fun onCompletedWhileNotObserving() {
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        val message: String
        when (exception) {
            is UserCancelledUploadException -> {
                Logger.e(TranslationExchangeExport::class.java.toString(), "Cancelled upload")
                Logger.e(
                    TranslationExchangeExport::class.java.toString(),
                    "Upload percent was " + uploadInfo.progressPercent
                )
                message = "Upload was cancelled by user: ${exception.message ?: ""}"
                this.onComplete(EXPORT_UPLOAD_ID)
            }
            is UploadError -> {
                message = String.format(
                    "code: %s: %s",
                    exception.serverResponse.code,
                    exception.serverResponse.bodyString
                )
                Logger.e(TranslationExchangeExport::class.java.toString(), message, exception)
                Logger.e(
                    TranslationExchangeExport::class.java.toString(),
                    "Error: $exception.message", exception
                )
            }
            else -> {
                Logger.e(
                    TranslationExchangeExport::class.java.toString(),
                    "Error: $uploadInfo", exception
                )
                message = "Error: ${exception.message ?: ""}"
            }
        }

        val fd = FeedbackDialog.newInstance(
            this.fragment.getString(R.string.project_upload),
            this.fragment.getString(R.string.project_upload_failed, message)
        )
        fd.show(this.fragment.parentFragmentManager, "UPLOAD_FEEDBACK")
        this.onComplete(EXPORT_UPLOAD_ID)
    }

    companion object {
        var EXPORT_UPLOAD_ID: Int = 3
    }
}
