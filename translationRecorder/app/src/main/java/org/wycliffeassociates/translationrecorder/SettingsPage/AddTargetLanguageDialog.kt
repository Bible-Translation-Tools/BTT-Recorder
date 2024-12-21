package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import javax.inject.Inject

/**
 * Created by sarabiaj on 12/14/2016.
 */
@AndroidEntryPoint
class AddTargetLanguageDialog : DialogFragment() {

    @Inject lateinit var db: IProjectDatabaseHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_add_temp_language, null)

        val languageCode = view.findViewById<View>(R.id.language_code) as EditText
        val languageName = view.findViewById<View>(R.id.language_name) as EditText
        val errorCodeExists = view.findViewById<View>(R.id.error_code_exists) as TextView
        val errorCodeTooShort = view.findViewById<View>(R.id.error_code_too_short) as TextView

        val addButton = view.findViewById<View>(R.id.ok_button) as Button
        val cancelButton = view.findViewById<View>(R.id.close_button) as Button

        addButton.setOnClickListener {
            var code = languageCode.text.toString()
            val name = languageName.text.toString()
            var error = false
            if (code.length < LANGUAGE_CODE_SIZE) {
                errorCodeTooShort.visibility = View.VISIBLE
                error = true
            } else {
                errorCodeTooShort.visibility = View.GONE
            }
            code = "qaa-x-tR$code"
            if (db.languageExists(code)) {
                errorCodeExists.visibility = View.VISIBLE
                error = true
            } else {
                errorCodeExists.visibility = View.GONE
            }
            if (!error) {
                db.addLanguage(code, name)
                dismiss()
            }
        }

        cancelButton.setOnClickListener { dismiss() }

        builder.setView(view)
        return builder.create()
    }

    companion object {
        const val LANGUAGE_CODE_SIZE: Int = 6
    }
}
