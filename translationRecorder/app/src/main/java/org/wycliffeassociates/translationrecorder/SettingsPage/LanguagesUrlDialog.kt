package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.DialogLanguagesUrlBinding
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
        val binding = DialogLanguagesUrlBinding.inflate(layoutInflater)

        val currentServerName = pref.getDefaultPref(
            SettingsActivity.KEY_PREF_LANGUAGES_URL,
            getString(R.string.pref_languages_url)
        )

        binding.url.setText(currentServerName)

        binding.saveButton.setOnClickListener {
            val name = binding.url.text.toString()
            if (name.isNotEmpty()) {
                pref.setDefaultPref(SettingsActivity.KEY_PREF_LANGUAGES_URL, name)
                dismiss()
            }
        }

        binding.restoreDefault.setOnClickListener {
            pref.setDefaultPref(
                SettingsActivity.KEY_PREF_LANGUAGES_URL,
                getString(R.string.pref_languages_url)
            )
            dismiss()
        }

        binding.closeButton.setOnClickListener { dismiss() }

        builder.setView(binding.root)
        return builder.create()
    }
}
