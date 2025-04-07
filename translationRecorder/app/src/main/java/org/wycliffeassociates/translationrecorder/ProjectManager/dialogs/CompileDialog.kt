package org.wycliffeassociates.translationrecorder.ProjectManager.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.project.Project

/**
 * Created by leongv on 8/17/2016.
 */
class CompileDialog : DialogFragment() {
    interface DialogListener {
        fun onPositiveClick(dialog: CompileDialog)
        fun onNegativeClick(dialog: CompileDialog)
    }

    var mListener: DialogListener? = null
    var chapterIndicies: IntArray? = null
        private set
    var project: Project? = null
        private set
    private var mAlreadyCompiled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments

        chapterIndicies = args?.getIntArray(CHAPTERS_KEY)
        project = args?.getParcelable(PROJECT_KEY)
        val compiled = args?.getBooleanArray(COMPILED_KEY)
        mAlreadyCompiled = false
        for (i in compiled!!.indices) {
            if (compiled[i]) {
                mAlreadyCompiled = true
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // NOTE: This is commented out because we don't want the compile to execute without the user
        //    clicking "OK" on the dialog. If Joe agrees, he can delete this.
        // if(!mAlreadyCompiled){
        //     mListener.onPositiveClick(CompileDialog.this);
        // }
        val message = if (mAlreadyCompiled) {
            getString(R.string.recompile_chapters_message)
        } else {
            getString(R.string.compile_chapters_message)
        }
        return AlertDialog.Builder(activity)
            .setTitle(getString(R.string.warning))
            .setMessage(message)
            .setPositiveButton(
                getString(R.string.label_ok)
            ) { _, _ ->
                mListener!!.onPositiveClick(
                    this@CompileDialog
                )
            }
            .setNegativeButton(
                getString(R.string.title_cancel)
            ) { _, _ ->
                mListener!!.onNegativeClick(
                    this@CompileDialog
                )
            }
            .create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement CompileDialogListener")
        }
    }

    companion object {
        var CHAPTERS_KEY: String = "key_chapters"
        var COMPILED_KEY: String = "key_compiled"
        var PROJECT_KEY: String = "key_project"

        fun newInstance(
            project: Project?,
            chapterIndices: IntArray?,
            isCompiled: BooleanArray?
        ): CompileDialog {
            val args = Bundle()
            args.putIntArray(CHAPTERS_KEY, chapterIndices)
            args.putParcelable(PROJECT_KEY, project)
            args.putBooleanArray(COMPILED_KEY, isCompiled)
            val check = CompileDialog()
            check.arguments = args
            return check
        }

        fun newInstance(project: Project?, chapterIndex: Int, isCompiled: Boolean): CompileDialog {
            val chapterIndices = intArrayOf(chapterIndex)
            val compiled = booleanArrayOf(isCompiled)
            return newInstance(project, chapterIndices, compiled)
        }
    }
}