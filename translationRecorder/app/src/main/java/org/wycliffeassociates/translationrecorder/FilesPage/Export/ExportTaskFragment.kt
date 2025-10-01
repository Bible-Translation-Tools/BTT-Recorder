package org.wycliffeassociates.translationrecorder.FilesPage.Export

import android.content.Context
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export.ProgressUpdateCallback

/**
 * Created by sarabiaj on 2/19/2016.
 */
class ExportTaskFragment : Fragment(), ProgressUpdateCallback {
    private var mProgressUpdateCallback: ProgressUpdateCallback? = null
    private var mExp: Export? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mProgressUpdateCallback = context as ProgressUpdateCallback
    }

    override fun onDetach() {
        super.onDetach()
        mProgressUpdateCallback = null
    }

    fun delegateExport(exp: Export?) {
        mExp = exp?.apply { export() }
    }

    override fun showProgress(mode: Boolean) {
        mProgressUpdateCallback?.showProgress(mode)
    }

    override fun setProgressTitle(title: String?) {
        mProgressUpdateCallback?.setProgressTitle(title)
    }

    override fun incrementProgress(progress: Int) {
        mProgressUpdateCallback?.incrementProgress(progress)
    }

    override fun setUploadProgress(progress: Int) {
        mProgressUpdateCallback?.setUploadProgress(progress)
    }

    override fun dismissProgress() {
        mProgressUpdateCallback?.dismissProgress()
    }

    override fun setZipping(zipping: Boolean) {
        mProgressUpdateCallback?.setZipping(zipping)
    }

    override fun setExporting(exporting: Boolean) {
        mProgressUpdateCallback?.setExporting(exporting)
    }

    override fun setCurrentFile(currentFile: String?) {
        mProgressUpdateCallback?.setCurrentFile(currentFile)
    }
}
