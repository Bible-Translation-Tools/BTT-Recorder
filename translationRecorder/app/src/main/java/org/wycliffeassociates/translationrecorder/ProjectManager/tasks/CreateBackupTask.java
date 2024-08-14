package org.wycliffeassociates.translationrecorder.ProjectManager.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.door43.tools.reporting.Logger;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;

import org.wycliffeassociates.translationrecorder.R;
import org.wycliffeassociates.translationrecorder.utilities.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by sarabiaj on 9/27/2016.
 */
public class CreateBackupTask extends Task {

    private static final String TAG = "CreateBackupTask";

    private final Context context;

    private final Uri backupUri;
    private final File appDataDir;
    private final File userDataDir;

    public CreateBackupTask(
            int taskTag,
            Context context,
            Uri backupUri
    ) {
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
        try {
            String uuid = UUID.randomUUID().toString();
            File tempZipFile = new File(context.getCacheDir(), uuid + ".zip");

            try (ZipFile zipper = new ZipFile(tempZipFile)) {
                ZipParameters zp = new ZipParameters();
                zp.setCompressionLevel(CompressionLevel.ULTRA);
                zp.setExcludeFileFilter(
                        file -> file.getName().equals("cache") ||
                                file.getName().equals("code_cache"));

                zipper.addFolder(appDataDir, zp);
                zipper.renameFile(appDataDir.getName() + "/", "app_data");

                zipper.addFolder(userDataDir, zp);
                zipper.renameFile(userDataDir.getName() + "/", "user_data");
            }

            try (OutputStream outputStream = context.getContentResolver().openOutputStream(backupUri)) {
                try (InputStream inputStream = new FileInputStream(tempZipFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        assert outputStream != null;
                        outputStream.write(buffer, 0, length);
                    }
                }
            }

            tempZipFile.delete();
        } catch (IOException | NullPointerException e) {
            Logger.e(TAG, e.getMessage(), e);
        }

        onTaskCompleteDelegator();
    }
}
