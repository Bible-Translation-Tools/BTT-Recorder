package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
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
import javax.inject.Inject

/**
 * Created by leongv on 12/17/2015.
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener,
    BackupRestoreDialogListener {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private var parent: LanguageSelector? = null
    private lateinit var taskFragment: TaskFragment

    interface LanguageSelector {
        fun sourceLanguageSelected()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        parent = activity as LanguageSelector

        preferenceManager.createPreferenceScreen(requireContext())
        addPreferencesFromResource(R.xml.preference)

        val fm = parentFragmentManager
        taskFragment = (fm.findFragmentByTag(TAG_TASK_FRAGMENT) as? TaskFragment) ?: run {
            val fragment = TaskFragment()
            fm.beginTransaction().add(fragment, TAG_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
            fragment
        }

        setPreferenceSummaryFromValue(KEY_PREF_GLOBAL_LANG_SRC)
        setPreferenceSummaryFromValue(KEY_PREF_ADD_LANGUAGE)
        setPreferenceSummaryFromValue(KEY_PREF_UPDATE_LANGUAGES)
        setPreferenceSummaryFromValue(KEY_PREF_UPDATE_LANGUAGES_FROM_FILE)
        setPreferenceSummaryFromValue(KEY_PREF_LANGUAGES_URL)
        setPreferenceSummaryFromValue(KEY_PREF_UPLOAD_SERVER)
        setPreferenceSummaryFromValue(KEY_PREF_BACKUP_RESTORE)

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

    override fun onResume() {
        super.onResume()
        preferenceScreen
            .sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen
            .sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.getString(key, "")?.let { value ->
            when (key) {
                KEY_PREF_GLOBAL_LANG_SRC -> parent?.sourceLanguageSelected()
                KEY_PREF_ADD_LANGUAGE -> {
                    val add = AddTargetLanguageDialog()
                    add.show(parentFragmentManager, "add")
                }
                KEY_PREF_UPDATE_LANGUAGES -> {
                    val updateLanguagesUrlPref: Preference? = findPreference(KEY_PREF_UPDATE_LANGUAGES_URL)
                    val updateLanguagesUrl = updateLanguagesUrlPref?.summary?.toString()

                    updateLanguagesUrl?.let { url ->
                        taskFragment.executeRunnable(
                            ResyncLanguageNamesTask(
                                RESYNC_LANGUAGE_NAMES_TASK_TAG,
                                requireContext(),
                                db,
                                assetsProvider,
                                url
                            ),
                            getString(R.string.updating_languages),
                            getString(R.string.please_wait),
                            true
                        )
                    }
                }
                KEY_PREF_UPDATE_LANGUAGES_FROM_FILE -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.setType("application/octet-stream")
                    startActivityForResult(intent, FILE_GET_REQUEST_CODE)
                }
                KEY_PREF_LANGUAGES_URL -> {
                    val add = LanguagesUrlDialog()
                    add.show(parentFragmentManager, "save")
                }
                KEY_PREF_UPLOAD_SERVER -> {
                    val add = UploadServerDialog()
                    add.show(parentFragmentManager, "save")
                }
                KEY_PREF_BACKUP_RESTORE -> {
                    val dialog = BackupRestoreDialog()
                    dialog.setListener(this)
                    dialog.show(parentFragmentManager, "backup")
                }
                else -> {}
            }
        }

        key?.let { setPreferenceSummaryFromValue(it) }
    }

    /**
     * Sets a preference's summary based on its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The exact display format is
     * dependent on the type of preference.
     *
     */
    private fun setPreferenceSummaryFromValue(key: String) {
        val preference: Preference? = findPreference(key)

        preference?.let { pref ->
            val value = prefs.getDefaultPref(preference.key, "")

            if (pref is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = pref.findIndexOfValue(value)

                // Set the summary to reflect the new value.
                pref.setSummary(
                    if (index >= 0) pref.entries[index] else null
                )
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                pref.summary = value
            }
        }
    }

    private fun updateSummariesSetViaActivities(sharedPreferences: SharedPreferences) {
        var uriString = sharedPreferences.getString(
            KEY_PREF_GLOBAL_SOURCE_LOC,
            ""
        )
        val pref = findPreference(KEY_PREF_GLOBAL_SOURCE_LOC) as? PreferenceScreen
        val dir = Uri.parse(uriString).lastPathSegment
        if (dir != null) {
            uriString = dir
            pref?.summary = uriString
        } else {
            pref?.summary = sharedPreferences
                .getString(KEY_PREF_GLOBAL_SOURCE_LOC, "")
        }
    }

    private fun updateSummaryText(sharedPreferences: SharedPreferences, key: String) {
        try {
            updateSummariesSetViaActivities(sharedPreferences)
            val text = sharedPreferences.getString(key, "")
            val pref = findPreference(key) as? PreferenceScreen
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