package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.DialogAddTempLanguageBinding
import javax.inject.Inject

/**
 * Created by sarabiaj on 12/14/2016.
 */
@AndroidEntryPoint
class AddTargetLanguageDialog : DialogFragment() {

    @Inject lateinit var db: IProjectDatabaseHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())

        val binding = DialogAddTempLanguageBinding.inflate(layoutInflater)

        binding.okButton.setOnClickListener {
            var code = binding.languageCode.text.toString()
            val name = binding.languageName.text.toString()
            var error = false
            if (code.length < LANGUAGE_CODE_SIZE) {
                binding.errorCodeTooShort.visibility = View.VISIBLE
                error = true
            } else {
                binding.errorCodeTooShort.visibility = View.GONE
            }
            code = "qaa-x-tR$code"
            if (db.languageExists(code)) {
                binding.errorCodeExists.visibility = View.VISIBLE
                error = true
            } else {
                binding.errorCodeExists.visibility = View.GONE
            }
            if (!error) {
                db.addLanguage(code, name)
                dismiss()
            }
        }

        binding.closeButton.setOnClickListener { dismiss() }

        builder.setView(binding.root)
        return builder.create()
    }

    companion object {
        const val LANGUAGE_CODE_SIZE: Int = 6
    }
}
