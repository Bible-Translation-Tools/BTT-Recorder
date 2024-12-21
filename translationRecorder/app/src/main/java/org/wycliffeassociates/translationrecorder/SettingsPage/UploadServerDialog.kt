package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import android.view.View
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
 * Created by sarabiaj on 12/14/2016.
 */
@AndroidEntryPoint
class UploadServerDialog : DialogFragment() {
    @Inject lateinit var pref: IPreferenceRepository

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_upload_server, null)

        val currentServerName = pref.getDefaultPref(
            Settings.KEY_PREF_UPLOAD_SERVER,
            "opentranslationtools.org"
        )

        val serverName = view.findViewById<View>(R.id.server_name) as EditText
        val saveButton = view.findViewById<View>(R.id.save_button) as Button
        val restoreButton = view.findViewById<View>(R.id.restore_default) as Button
        val cancelButton = view.findViewById<View>(R.id.close_button) as Button

        serverName.setText(currentServerName)

        saveButton.setOnClickListener {
            val name = serverName.text.toString()
            if (name.isNotEmpty()) {
                pref.setDefaultPref(Settings.KEY_PREF_UPLOAD_SERVER, name)
                dismiss()
            }
        }

        restoreButton.setOnClickListener {
            pref.setDefaultPref(Settings.KEY_PREF_UPLOAD_SERVER, "opentranslationtools.org")
            dismiss()
        }

        cancelButton.setOnClickListener { dismiss() }

        builder.setView(view)
        return builder.create()
    }
}
