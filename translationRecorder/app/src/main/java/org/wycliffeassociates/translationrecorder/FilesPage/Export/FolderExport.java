package org.wycliffeassociates.translationrecorder.FilesPage.Export;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.wycliffeassociates.translationrecorder.project.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by sarabiaj on 12/10/2015.
 */
public class FolderExport extends Export{

    public FolderExport(File projectToExport, Project project){
        super(projectToExport, project);
    }

    @Override
    protected void handleUserInput() {
        Intent i = new Intent(mCtx.getActivity(), StorageAccess.class);
        try {
            i.putExtra("export_project", mDirectoryToZip.getCanonicalPath());
            i.putExtra("zip_path", mZipFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCtx.startActivity(i);
    }

    public static class StorageAccess extends Activity{

        private Uri mCurrentUri;
        private File mZipPath;
        private final int SAVE_FILE = 43;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent intent = getIntent();
            mZipPath = new File(intent.getStringExtra("zip_path"));
            createFile("application/zip", mZipPath.getName());
        }

        /**
         * Closes the activity on a back press to return back to the files page
         */
        @Override
        public void onBackPressed(){
            this.finish();
        }

        /**
         * Receives the user selected location to save to as a Uri
         * @param requestCode should be set to SAVE_FILE to continue with the export
         * @param resultCode equals RESULT_OK if chosing a location completed
         * @param resultData contains the Uri to save to
         */
        public void onActivityResult(int requestCode, int resultCode,
                                     Intent resultData) {
            mCurrentUri = null;
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == SAVE_FILE) {
                    mCurrentUri = resultData.getData();
                        savefile(mCurrentUri, mZipPath);
                        mZipPath = null;//reset
                }

                if(requestCode == 3){//delete zip file, needs to be done after upload
                    mZipPath = null;//set null for next time
                }
            }
            finish();
        }

        /**
         * Copies a file from a path to a uri
         * @param destUri The destination of the file
         * @param zippedProject The original path to the file
         */
        public void savefile(Uri destUri, File zippedProject)
        {
            try {
                try (OutputStream outputStream = getContentResolver().openOutputStream(destUri)) {
                    try (InputStream inputStream = new FileInputStream(zippedProject)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            assert outputStream != null;
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            zippedProject.delete();
            this.finish();
        }

        /**
         * Creates a file in folder selected by user
         * @param mimeType Typically going to be "audio/*" for this app
         * @param fileName The name of the file selected.
         */
        private void createFile(String mimeType, String fileName) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, SAVE_FILE);
        }
    }
}
