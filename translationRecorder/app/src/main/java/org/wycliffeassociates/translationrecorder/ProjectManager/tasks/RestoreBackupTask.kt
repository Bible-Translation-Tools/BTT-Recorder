package org.wycliffeassociates.translationrecorder.ProjectManager.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.door43.tools.reporting.Logger;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.wycliffeassociates.translationrecorder.R;
import org.wycliffeassociates.translationrecorder.utilities.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by sarabiaj on 9/27/2016.
 */
public class RestoreBackupTask extends Task {

    private static final String TAG = "RestoreBackupTask";

    private final Context context;

    private final Uri backupUri;
    private final File appDataDir;
    private final File userDataDir;

    public RestoreBackupTask(int taskTag, Context context, Uri backupUri) {
        super(taskTag);

        this.context = context;
        this.backupUri = backupUri;
        this.userDataDir = new File(
                Environment.getExternalStorageDirectory(),
                context.getResources().getString(R.string.folder_name)
        );
        this.appDataDir = new File(context.getApplicationInfo().dataDir);
    }

    @Override
    public void run() {
        String uuid = UUID.randomUUID().toString();
        File tempZipFile = new File(context.getCacheDir(), uuid + ".zip");

        try {
            try (InputStream inputStream = context.getContentResolver().openInputStream(backupUri)) {
                try (OutputStream outputStream = new FileOutputStream(tempZipFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while (true) {
                        assert inputStream != null;
                        if (!((length = inputStream.read(buffer)) > 0)) break;
                        outputStream.write(buffer, 0, length);
                    }
                }
            }

            try (ZipFile zipFile = new ZipFile(tempZipFile)) {
                extractDirectory(zipFile, "user_data/", userDataDir);
                extractDirectory(zipFile, "app_data/", appDataDir);
            }

            tempZipFile.delete();
        } catch (IOException e) {
            Logger.e(TAG, e.getMessage(), e);
        }

        onTaskCompleteDelegator();
    }

    private void extractDirectory(ZipFile zipFile, String dirToExtract, File destDir) throws IOException {
        for (FileHeader fileHeader : zipFile.getFileHeaders()) {
            // Check if the file is a child of the specified folder within the zip file
            String filePathInZip = fileHeader.getFileName();
            if (filePathInZip.startsWith(dirToExtract) && !filePathInZip.equals(dirToExtract)) {
                // Calculate the relative path of the file by removing the parent folder path
                String relativePath = filePathInZip.substring(dirToExtract.length());

                // Construct the destination file path
                File destFile = new File(destDir, relativePath);

                // Ensure the parent directories exist
                if (fileHeader.isDirectory()) {
                    destFile.mkdirs();
                } else {
                    destFile.getParentFile().mkdirs();
                    // Extract the file to the destination directory
                    zipFile.extractFile(fileHeader, destFile.getParent(), destFile.getName());
                }
            }
        }
    }
}
