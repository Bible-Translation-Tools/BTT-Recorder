package org.wycliffeassociates.translationrecorder.ProjectManager.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.databinding.DialogLanguageNotFoundBinding
import java.util.concurrent.BlockingQueue

/**
 * Created by sarabiaj on 12/14/2016.
 */
class RequestLanguageNameDialog : DialogFragment() {
    private var mCode: String? = null
    private var mResponse: BlockingQueue<String>? = null

    private fun setLanguageCode(code: String) {
        mCode = code
    }

    private fun setResponseQueue(responseQueue: BlockingQueue<String>) {
        mResponse = responseQueue
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        val binding = DialogLanguageNotFoundBinding.inflate(layoutInflater)

        binding.languageCode.text = mCode
        binding.okButton.setOnClickListener {
            try {
                mResponse?.put(binding.languageName.text.toString())
                dismiss()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        builder.setView(view)
        return builder.create()
    }

    companion object {
        fun newInstance(
            languageCode: String,
            response: BlockingQueue<String>
        ): RequestLanguageNameDialog {
            val dialog = RequestLanguageNameDialog()
            dialog.setLanguageCode(languageCode)
            dialog.setResponseQueue(response)
            return dialog
        }
    }
}
