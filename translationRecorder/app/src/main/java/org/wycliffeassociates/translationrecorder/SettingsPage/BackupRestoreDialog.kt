package org.wycliffeassociates.translationrecorder.SettingsPage

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.databinding.DialogBackupRestoreBinding
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by sarabiaj on 12/14/2016.
 */
@AndroidEntryPoint
class BackupRestoreDialog : DialogFragment() {
    interface BackupRestoreDialogListener {
        fun onCreateBackup(zipFileUri: Uri)
        fun onRestoreBackup(zipFileUri: Uri)
    }

    private var listener: BackupRestoreDialogListener? = null

    private lateinit var createBackup: ActivityResultLauncher<String>
    private lateinit var restoreBackup: ActivityResultLauncher<String>

    @SuppressLint("SimpleDateFormat")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogBackupRestoreBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireActivity())

        createBackup = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let {
                listener?.onCreateBackup(it)
                dismiss()
            }
        }

        restoreBackup = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                listener?.onRestoreBackup(it)
                dismiss()
            }
        }

        with(binding) {
            backupButton.setOnClickListener {
                val date = Date()
                val dateString = SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(date)
                chooseFileForBackup("$dateString.zip")
            }

            restoreButton.setOnClickListener {
                chooseFileForRestore()
            }

            closeButton.setOnClickListener { dismiss() }

            builder.setView(binding.root)
        }

        return builder.create()
    }

    fun setListener(listener: BackupRestoreDialogListener?) {
        this.listener = listener
    }

    private fun chooseFileForBackup(fileName: String) {
        createBackup.launch(fileName)
    }

    private fun chooseFileForRestore() {
        restoreBackup.launch("application/zip")
    }
}
