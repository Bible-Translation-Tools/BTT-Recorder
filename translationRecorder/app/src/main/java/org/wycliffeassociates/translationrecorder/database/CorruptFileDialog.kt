package org.wycliffeassociates.translationrecorder.database

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.DialogCorruptFileBinding
import java.io.File


class CorruptFileDialog : DialogFragment(), View.OnClickListener {
    private var mFile: File? = null

    fun setFile(file: File?) {
        mFile = file
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        val binding = DialogCorruptFileBinding.inflate(layoutInflater)
        binding.okButton.setOnClickListener(this)
        binding.ignoreButton.setOnClickListener(this)
        binding.filenameView.text = mFile!!.name

        builder.setView(binding.root)
        return builder.create()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ignore_button -> dismiss()
            R.id.ok_button -> {
                mFile?.delete()
                dismiss()
            }
            else -> {
                Logger.e(this.toString(), "Corrupt file dialog hit the default statement.")
            }
        }
        dismiss()
    }

    companion object {
        fun newInstance(file: File?): CorruptFileDialog {
            val dialog = CorruptFileDialog()
            dialog.setFile(file)
            return dialog
        }
    }
}
