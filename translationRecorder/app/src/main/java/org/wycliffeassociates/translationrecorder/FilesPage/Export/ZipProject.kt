package org.wycliffeassociates.translationrecorder.FilesPage.Export

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

/**
 * Created by sarabiaj on 1/24/2018.
 */
class ZipProject {
    val mFiles: List<File>?
    val mDirectory: File?

    //Arraylist explicitly specified because of zip4j dependency
    constructor(files: List<File>?) {
        mFiles = files
        mDirectory = null
    }

    constructor(directory: File?) {
        mDirectory = directory
        mFiles = null
    }

    fun zip(outFile: File, progressCallback: SimpleProgressCallback?) {
        val zipThread = Thread {
            try {
                // Trying to delete file if it exists,
                // because if for some reason file is corrupted
                // ZipFile will crash
                if (outFile.exists()) {
                    outFile.delete()
                }
                val zipper = ZipFile(outFile)
                val zp = ZipParameters()
                zipper.isRunInThread = true
                zp.compressionLevel = CompressionLevel.ULTRA
                //zip.addFiles(files, zp);
                val pm = zipper.progressMonitor
                if (mDirectory != null) {
                    zipper.addFolder(mDirectory, zp)
                } else if (mFiles != null) {
                    zipper.addFiles(mFiles, zp)
                }
                if (progressCallback != null) {
                    progressCallback.onStart(ZIP_PROJECT_ID)
                    while (pm.state == ProgressMonitor.State.BUSY) {
                        progressCallback.setCurrentFile(
                            ZIP_PROJECT_ID,
                            pm.fileName
                        )
                        progressCallback.setUploadProgress(
                            ZIP_PROJECT_ID,
                            pm.percentDone
                        )
                        Thread.sleep(PROGRESS_REFRESH_RATE.toLong())
                    }
                }
            } catch (e: ZipException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            progressCallback?.onComplete(ZIP_PROJECT_ID)
        }
        zipThread.start()
    }

    companion object {
        var ZIP_PROJECT_ID: Int = 2

        const val PROGRESS_REFRESH_RATE: Int =
            200 //200 ms refresh for progress dialog (arbitrary value)
    }
}
