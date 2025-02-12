package org.wycliffeassociates.translationrecorder.FilesPage

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.wycliffeassociates.translationrecorder.databinding.DialogFeedbackBinding


class FeedbackDialog : DialogFragment() {

    private var _binding: DialogFeedbackBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mTitle = requireArguments().getString(DIALOG_TITLE)
        val mMessage = requireArguments().getString(DIALOG_MESSAGE)

        _binding = DialogFeedbackBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireActivity())

        binding.okButton.setOnClickListener { dismiss() }
        binding.title.text = mTitle
        binding.message.text = mMessage

        builder.setView(binding.root)
        return builder.create()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }

    companion object {
        const val DIALOG_TITLE: String = "dialogTitle"
        const val DIALOG_MESSAGE: String = "dialogMessage"

        fun newInstance(title: String, message: String): FeedbackDialog {
            val dialog = FeedbackDialog()

            val args = Bundle()
            args.putString(DIALOG_TITLE, title)
            args.putString(DIALOG_MESSAGE, message)
            dialog.arguments = args

            return dialog
        }
    }
}
