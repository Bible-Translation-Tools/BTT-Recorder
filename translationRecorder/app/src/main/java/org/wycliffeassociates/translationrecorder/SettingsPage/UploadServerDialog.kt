package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.DialogUploadServerBinding
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
        val binding = DialogUploadServerBinding.inflate(layoutInflater)

        val currentServerName = pref.getDefaultPref(
            SettingsActivity.KEY_PREF_UPLOAD_SERVER,
            getString(R.string.pref_upload_server)
        )

        binding.serverName.setText(currentServerName)

        binding.saveButton.setOnClickListener {
            val name = binding.serverName.text.toString()
            if (name.isNotEmpty()) {
                pref.setDefaultPref(SettingsActivity.KEY_PREF_UPLOAD_SERVER, name)
                dismiss()
            }
        }

        binding.restoreDefault.setOnClickListener {
            pref.setDefaultPref(
                SettingsActivity.KEY_PREF_UPLOAD_SERVER,
                getString(R.string.pref_upload_server)
            )
            dismiss()
        }

        binding.closeButton.setOnClickListener { dismiss() }

        builder.setView(binding.root)
        return builder.create()
    }
}
