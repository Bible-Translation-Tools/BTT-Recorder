package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.CreateBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.RestoreBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ResyncLanguageNamesTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.BackupRestoreDialog.BackupRestoreDialogListener
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.BACKUP_TASK_TAG
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_ADD_LANGUAGE
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_BACKUP_RESTORE
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_GLOBAL_LANG_SRC
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_GLOBAL_SOURCE_LOC
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_LANGUAGES_URL
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_UPDATE_LANGUAGES
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_UPDATE_LANGUAGES_FROM_FILE
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_UPDATE_LANGUAGES_URL
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.KEY_PREF_UPLOAD_SERVER
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.RESTORE_TASK_TAG
import org.wycliffeassociates.translationrecorder.SettingsPage.Settings.Companion.RESYNC_LANGUAGE_NAMES_TASK_TAG
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment
import java.io.File
import javax.inject.Inject

/**
 * Created by leongv on 12/17/2015.
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(),
    OnSharedPreferenceChangeListener, BackupRestoreDialogListener {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private var parent: LanguageSelector? = null
    private lateinit var taskFragment: TaskFragment

    interface LanguageSelector {
        fun sourceLanguageSelected()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.fitsSystemWindows = true
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        parent = activity as LanguageSelector

        preferenceManager.createPreferenceScreen(requireContext())
        addPreferencesFromResource(R.xml.preference)

        taskFragment = (parentFragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT) as? TaskFragment) ?: run {
            val fragment = TaskFragment()
            parentFragmentManager.beginTransaction().add(fragment, TAG_TASK_FRAGMENT).commit()
            fragment
        }

        updateSummaryText(KEY_PREF_GLOBAL_LANG_SRC)
        updateSummaryText(KEY_PREF_ADD_LANGUAGE)
        updateSummaryText(KEY_PREF_UPDATE_LANGUAGES)
        updateSummaryText(KEY_PREF_UPDATE_LANGUAGES_FROM_FILE)
        updateSummaryText(KEY_PREF_LANGUAGES_URL)
        updateSummaryText(KEY_PREF_UPLOAD_SERVER)
        updateSummaryText(KEY_PREF_BACKUP_RESTORE)

        val globalLanguagePref: Preference? = findPreference(KEY_PREF_GLOBAL_LANG_SRC)
        globalLanguagePref?.setOnPreferenceClickListener {
            parent?.sourceLanguageSelected()
            true
        }

        val addLanguagePref: Preference? = findPreference(KEY_PREF_ADD_LANGUAGE)
        addLanguagePref?.setOnPreferenceClickListener {
            val add = AddTargetLanguageDialog()
            add.show(parentFragmentManager, "add")
            true
        }

        val updateLanguagesUrlPref: Preference? = findPreference(KEY_PREF_UPDATE_LANGUAGES)
        updateLanguagesUrlPref?.setOnPreferenceClickListener {
            val updateLanguageUrlPref: Preference? = findPreference(KEY_PREF_UPDATE_LANGUAGES_URL)
            val updateLanguagesUrl = updateLanguageUrlPref?.summary?.toString()
            updateLanguagesUrl?.let { url ->
                taskFragment.executeRunnable(
                    ResyncLanguageNamesTask(
                        RESYNC_LANGUAGE_NAMES_TASK_TAG,
                        requireContext(),
                        db,
                        assetsProvider,
                        requireContext(),
                        url
                    ),
                    getString(R.string.updating_languages),
                    getString(R.string.please_wait),
                    true
                )
            }
            true
        }

        val updateLanguagesFromFilePref: Preference? = findPreference(KEY_PREF_UPDATE_LANGUAGES_FROM_FILE)
        updateLanguagesFromFilePref?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.setType("*/*")
            startActivityForResult(intent, FILE_GET_REQUEST_CODE)
            true
        }

        val languagesUrlPref: Preference? = findPreference(KEY_PREF_LANGUAGES_URL)
        languagesUrlPref?.setOnPreferenceClickListener {
            val add = LanguagesUrlDialog()
            add.show(parentFragmentManager, "save")
            true
        }

        val uploadServerPref: Preference? = findPreference(KEY_PREF_UPLOAD_SERVER)
        uploadServerPref?.setOnPreferenceClickListener {
            val add = UploadServerDialog()
            add.show(parentFragmentManager, "save")
            true
        }

        val backupRestorePref: Preference? = findPreference(KEY_PREF_BACKUP_RESTORE)
        backupRestorePref?.setOnPreferenceClickListener {
            val dialog = BackupRestoreDialog()
            dialog.setListener(this)
            dialog.show(parentFragmentManager, "backup")
            true
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        key?.let { updateSummaryText(it) }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.let { preferences ->
            preferences.registerOnSharedPreferenceChangeListener(this)
            preferences.all.keys.forEach {
                updateSummaryText(it)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen
            .sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_GET_REQUEST_CODE) {
                val uri = data?.data
                taskFragment.executeRunnable(
                    ResyncLanguageNamesTask(
                        RESYNC_LANGUAGE_NAMES_TASK_TAG,
                        requireContext(),
                        db,
                        assetsProvider,
                        requireContext(),
                        uri
                    ),
                    getString(R.string.updating_languages),
                    getString(R.string.please_wait),
                    true
                )
            }
        }
    }

    override fun onCreateBackup(zipFileUri: Uri) {
        taskFragment.executeRunnable(
            CreateBackupTask(BACKUP_TASK_TAG, requireActivity(), zipFileUri, directoryProvider),
            getString(R.string.creating_backup),
            getString(R.string.please_wait),
            true
        )
    }

    override fun onRestoreBackup(zipFileUri: Uri) {
        taskFragment.executeRunnable(
            RestoreBackupTask(RESTORE_TASK_TAG, requireActivity(), zipFileUri, directoryProvider),
            getString(R.string.restoring_backup),
            getString(R.string.please_wait),
            true
        )
    }

    private fun updateSummariesSetViaActivities() {
        var fileString = prefs.getDefaultPref(
            KEY_PREF_GLOBAL_SOURCE_LOC,
            ""
        )
        val pref = findPreference(KEY_PREF_GLOBAL_SOURCE_LOC) as? Preference
        val file = File(fileString)
        if (file.exists()) {
            fileString = file.name
            pref?.summary = fileString
        } else {
            pref?.summary = prefs.getDefaultPref(
                KEY_PREF_GLOBAL_SOURCE_LOC,
                ""
            )
        }
    }

    private fun updateSummaryText(key: String) {
        try {
            updateSummariesSetViaActivities()
            val text = prefs.getDefaultPref(key, "")
            val pref = findPreference(key) as? Preference
            pref?.summary = text
        } catch (err: ClassCastException) {
            println("IGNORING SUMMARY UPDATE FOR $key")
        }
    }

    private companion object {
        const val FILE_GET_REQUEST_CODE = 45
        const val TAG_TASK_FRAGMENT = "tag_task_fragment"
    }
}