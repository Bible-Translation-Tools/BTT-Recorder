package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import javax.inject.Inject

/**
 * Created by mxaln on 08/16/2023.
 */
@AndroidEntryPoint
class LanguagesUrlDialog : DialogFragment() {
    @Inject lateinit var pref: IPreferenceRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_languages_url, null)

        val currentServerName = pref.getDefaultPref(
            Settings.KEY_PREF_LANGUAGES_URL,
            getString(R.string.pref_languages_url)
        )

        val url = view.findViewById<EditText>(R.id.url)
        val saveButton = view.findViewById<Button>(R.id.save_button)
        val restoreButton = view.findViewById<Button>(R.id.restore_default)
        val cancelButton = view.findViewById<Button>(R.id.close_button)

        url.setText(currentServerName)

        saveButton.setOnClickListener {
            val name = url.text.toString()
            if (name.isNotEmpty()) {
                pref.setDefaultPref(Settings.KEY_PREF_LANGUAGES_URL, name)
                dismiss()
            }
        }

        restoreButton.setOnClickListener {
            pref.setDefaultPref(
                Settings.KEY_PREF_LANGUAGES_URL,
                getString(R.string.pref_languages_url)
            )
            dismiss()
        }

        cancelButton.setOnClickListener { dismiss() }

        builder.setView(view)
        return builder.create()
    }
}
