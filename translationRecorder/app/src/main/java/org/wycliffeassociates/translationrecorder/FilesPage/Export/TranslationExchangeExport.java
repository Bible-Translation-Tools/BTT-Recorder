package org.wycliffeassociates.translationrecorder.FilesPage.Export;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import com.door43.tools.reporting.Logger;
import net.gotev.uploadservice.*;
import org.wycliffeassociates.translationrecorder.FilesPage.FeedbackDialog;
import org.wycliffeassociates.translationrecorder.R;
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings;
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp;
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper;
import org.wycliffeassociates.translationrecorder.project.Project;
import org.wycliffeassociates.translationrecorder.project.components.User;

import java.io.File;
import java.util.UUID;

/**
 * Created by sarabiaj on 11/16/2017.
 */

public class TranslationExchangeExport extends Export implements UploadStatusDelegate {

    public static int EXPORT_UPLOAD_ID = 3;

    TranslationExchangeDiff mDiffer;
    ProjectDatabaseHelper db;
    UploadServiceSingleBroadcastReceiver uploadReceiver;

    public TranslationExchangeExport(File projectToExport, Project project, ProjectDatabaseHelper db) {
        super(projectToExport, project);
        mDirectoryToZip = null;
        this.db = db;
        uploadReceiver = new UploadServiceSingleBroadcastReceiver(this);
    }

    @Override
    protected void initialize() {
        mDiffer = new TranslationExchangeDiff(
                (TranslationRecorderApp) mCtx.getActivity().getApplication(),
                mProject
        );
        mDiffer.computeDiff(outputFile(), this);
    }

    @Override
    public void onStart(final int id) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (id == TranslationExchangeDiff.DIFF_ID) {
                    mProgressCallback.setProgressTitle("Step 1/3: Generating manifest file");
                } else if(id == ZipProject.ZIP_PROJECT_ID) {
                    mProgressCallback.setProgressTitle("Step 2/3: Packaging files to export");
                } else if(id == TranslationExchangeExport.EXPORT_UPLOAD_ID) {
                    mProgressCallback.setProgressTitle("Step 3/3: Uploading");
                }

                mZipDone = false;
                mProgressCallback.setZipping(true);
                mProgressCallback.showProgress(ProgressUpdateCallback.ZIP);
            }
        });
    }

    @Override
    public void onComplete(int id) {
        super.onComplete(id);
        if(id == TranslationExchangeDiff.DIFF_ID) {
            mFilesToZip = mDiffer.getDiff();
            super.initialize();
        } else if(id == EXPORT_UPLOAD_ID) {
            mProgressCallback.setProgressTitle(null);
            mProgressCallback.setZipping(false);
        }
    }

    @Override
    protected void handleUserInput() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Context ctx = mCtx.getActivity().getApplicationContext();
                uploadBinary(ctx, outputFile());
            }
        });
        thread.start();
    }

    public void uploadBinary(Context context, File file) {
        try {
            this.onStart(EXPORT_UPLOAD_ID);

            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            String filePath = file.getAbsolutePath();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int userId = pref.getInt(Settings.KEY_USER, 1);
            User user = db.getUser(userId);
            String hash = user.getHash();

            String uploadId = UUID.randomUUID().toString();
            uploadReceiver.setUploadID(uploadId);
            uploadReceiver.register(context);

            new BinaryUploadRequest(context, uploadId, "http://opentranslationtools.org/api/upload/zip")
                            .addHeader("tr-user-hash", hash)
                            .addHeader("tr-file-name", file.getName())
                            .setFileToUpload(filePath)
                            .setNotificationConfig(getNotificationConfig())
                            .setDelegate(null)
                            .setAutoDeleteFilesAfterSuccessfulUpload(true)
                            .setMaxRetries(30)
                            .startUpload();

        } catch (Exception exc) {
            Log.e("AndroidUploadService", exc.getMessage(), exc);
        }
    }

    protected UploadNotificationConfig getNotificationConfig() {
        UploadNotificationConfig config = new UploadNotificationConfig();

        config.getProgress().iconResourceID = R.drawable.ic_upload;
        config.getProgress().iconColorResourceID = Color.BLUE;

        config.getCompleted().iconResourceID = R.drawable.ic_upload_success;
        config.getCompleted().iconColorResourceID = Color.GREEN;

        config.getError().iconResourceID = R.drawable.ic_upload_error;
        config.getError().iconColorResourceID = Color.RED;

        config.getCancelled().iconResourceID = R.drawable.ic_cancelled;
        config.getCancelled().iconColorResourceID = Color.YELLOW;

        return config;
    }

    @Override
    public void onProgress(Context context, UploadInfo uploadInfo) {
        this.setUploadProgress(EXPORT_UPLOAD_ID, uploadInfo.getProgressPercent());
    }

    @Override
    public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
        FeedbackDialog fd = FeedbackDialog.newInstance(
                "Project upload",
                "Project has been successfully uploaded."
        );
        fd.show(mCtx.getFragmentManager(), "title");

        mZipFile.delete();

        Logger.e(
                TranslationExchangeExport.class.toString(),
                "code: " + serverResponse.getHttpCode() + " " + serverResponse.getBodyAsString()
        );
        this.onComplete(EXPORT_UPLOAD_ID);
    }

    @Override
    public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
        String message;
        if (serverResponse != null) {
            message = String.format("code: %s: %s",
                    serverResponse.getHttpCode(),
                    serverResponse.getBodyAsString()
            );
            Logger.e(TranslationExchangeExport.class.toString(), message, exception);
        } else if (exception != null) {
            message = exception.getMessage();
            Logger.e(TranslationExchangeExport.class.toString(), "Error: " + message, exception);
        } else {
            message = "An error occurred without a response or exception, upload percent is "
                    + uploadInfo.getProgressPercent();
            Logger.e(TranslationExchangeExport.class.toString(), message);
        }

        FeedbackDialog fd = FeedbackDialog.newInstance(
                "Project upload",
                "Project upload failed: " + message
        );
        fd.show(mCtx.getFragmentManager(), "UPLOAD_FEEDBACK");
        this.onComplete(EXPORT_UPLOAD_ID);
    }

    @Override
    public void onCancelled(Context context, UploadInfo uploadInfo) {
        Logger.e(TranslationExchangeExport.class.toString(), "Cancelled upload");
        if (uploadInfo != null) {
            Logger.e(
                    TranslationExchangeExport.class.toString(),
                    "Upload percent was " + uploadInfo.getProgressPercent()
            );
        }
        this.onComplete(EXPORT_UPLOAD_ID);
    }
}
