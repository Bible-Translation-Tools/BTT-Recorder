package org.wycliffeassociates.translationrecorder.SettingsPage

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
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

    companion object {
        const val CREATE_BACKUP_FILE = 1
        const val RESTORE_BACKUP_FILE = 2
    }

    var pref: SharedPreferences? = null

    lateinit var binding: DialogBackupRestoreBinding

    private var listener: BackupRestoreDialogListener? = null

    @SuppressLint("SimpleDateFormat")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogBackupRestoreBinding.inflate(requireActivity().layoutInflater)

        val builder = AlertDialog.Builder(requireActivity())

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

    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        resultData: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            resultData?.data?.let {
                when (requestCode) {
                    RESTORE_BACKUP_FILE -> {
                        listener?.onRestoreBackup(it)
                    }
                    CREATE_BACKUP_FILE -> {
                        listener?.onCreateBackup(it)

                    }
                    else -> {}
                }
            }
        }
        dismiss()
    }

    private fun chooseFileForBackup(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, CREATE_BACKUP_FILE)
    }

    private fun chooseFileForRestore() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        startActivityForResult(intent, RESTORE_BACKUP_FILE)
    }
}
