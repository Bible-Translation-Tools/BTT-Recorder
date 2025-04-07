package org.wycliffeassociates.translationrecorder.ProjectManager.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.DialogCheckingBinding
import org.wycliffeassociates.translationrecorder.project.Project

/**
 * Created by leongv on 8/9/2016.
 */
class CheckingDialog : DialogFragment() {
    interface DialogListener {
        fun onPositiveClick(dialog: CheckingDialog)
        fun onNegativeClick(dialog: CheckingDialog)
    }

    private var mCheckingLevel = 0
    var mListener: DialogListener? = null
    var chapterIndicies: IntArray? = null
        private set
    var project: Project? = null
        private set
    private var mPositiveBtn: Button? = null

    private var _binding: DialogCheckingBinding? = null
    private val binding get() = _binding!!

    var checkingLevel: Int
        get() = mCheckingLevel
        private set(checkingLevel) {
            mCheckingLevel = checkingLevel
            mPositiveBtn!!.isEnabled = true
            binding.checkLevelZero.isActivated = checkingLevel == 0
            binding.checkLevelOne.isActivated = checkingLevel == 1
            binding.checkLevelTwo.isActivated = checkingLevel == 2
            binding.checkLevelThree.isActivated = checkingLevel == 3
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        chapterIndicies = args?.getIntArray(CHAPTERS_KEY)
        project = args?.getParcelable(PROJECT_KEY)
        mCheckingLevel = args?.getInt(CURRENT_LEVEL_KEY) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCheckingBinding.inflate(layoutInflater)

        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(getString(R.string.set_checking_level))
            .setView(binding.root)
            .setPositiveButton(
                getString(R.string.label_ok)
            ) { _, _ -> mListener!!.onPositiveClick(this@CheckingDialog) }
            .setNegativeButton(
                getString(R.string.title_cancel)
            ) { _, _ -> mListener!!.onNegativeClick(this@CheckingDialog) }
            .create()

        alertDialog.setOnShowListener {
            mPositiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

            binding.checkLevelZero.setOnClickListener { checkingLevel = 0 }
            binding.checkLevelOne.setOnClickListener { checkingLevel = 1 }
            binding.checkLevelTwo.setOnClickListener { checkingLevel = 2 }
            binding.checkLevelThree.setOnClickListener { checkingLevel = 3 }

            checkingLevel = mCheckingLevel

            if (mCheckingLevel == NO_LEVEL_SELECTED) {
                mPositiveBtn?.setEnabled(false)
            }
        }

        return alertDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement CheckingDialogListener")
        }
    }

    companion object {
        var CHAPTERS_KEY: String = "key_chapters"
        var PROJECT_KEY: String = "key_project"
        var CURRENT_LEVEL_KEY: String = "key_current_checking_level"
        var NO_LEVEL_SELECTED: Int = -1

        fun newInstance(
            project: Project?,
            chapterIndices: IntArray?,
            checkingLevel: Int
        ): CheckingDialog {
            val args = Bundle()
            args.putIntArray(CHAPTERS_KEY, chapterIndices)
            args.putParcelable(PROJECT_KEY, project)
            args.putInt(CURRENT_LEVEL_KEY, checkingLevel)
            val check = CheckingDialog()
            check.arguments = args
            return check
        }

        fun newInstance(project: Project?, chapterIndex: Int, checkingLevel: Int): CheckingDialog {
            val chapterIndices = intArrayOf(chapterIndex)
            return newInstance(project, chapterIndices, checkingLevel)
        }
    }
}