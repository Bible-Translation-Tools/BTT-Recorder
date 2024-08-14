package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.CreateBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.RestoreBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ResyncLanguageNamesTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.BackupRestoreDialog.BackupRestoreDialogListener
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment

/**
 * Created by leongv on 12/17/2015.
 */
class SettingsFragment : PreferenceFragment(), OnSharedPreferenceChangeListener,
    BackupRestoreDialogListener {

    private companion object {
        const val FILE_GET_REQUEST_CODE = 45
        const val TAG_TASK_FRAGMENT = "tag_task_fragment"
    }

    private var mParent: LanguageSelector? = null
    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mTaskFragment: TaskFragment

    var db: ProjectDatabaseHelper? = null

    interface LanguageSelector {
        fun sourceLanguageSelected()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference)

        mSharedPreferences = preferenceScreen.sharedPreferences
        mParent = activity as LanguageSelector
        db = (activity.application as TranslationRecorderApp).database

        // Below is the code to clear the SharedPreferences. Use it wisely.
        // mSharedPreferences.edit().clear().commit();

        // Register listener(s)
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val fm = fragmentManager
        mTaskFragment = (fm.findFragmentByTag(TAG_TASK_FRAGMENT) as? TaskFragment) ?: run {
            val fragment = TaskFragment()
            fm.beginTransaction().add(fragment, TAG_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
            fragment
        }

        val sourceLanguageButton = findPreference(Settings.KEY_PREF_GLOBAL_LANG_SRC)
        sourceLanguageButton.onPreferenceClickListener = OnPreferenceClickListener {
            mParent!!.sourceLanguageSelected()
            true
        }

        val addTemporaryLanguageButton = findPreference(Settings.KEY_PREF_ADD_LANGUAGE)
        addTemporaryLanguageButton.onPreferenceClickListener = OnPreferenceClickListener {
            val add = AddTargetLanguageDialog()
            add.show(fragmentManager, "add")
            false
        }

        val updateLanguagesButton = findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES)
        updateLanguagesButton.onPreferenceClickListener = OnPreferenceClickListener {
            val updateLanguagesUrlPref = findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES_URL)
            val updateLanguagesUrl = updateLanguagesUrlPref.summary.toString()

            mTaskFragment.executeRunnable(
                ResyncLanguageNamesTask(
                    Settings.RESYNC_LANGUAGE_NAMES_TASK_TAG,
                    activity,
                    db,
                    updateLanguagesUrl
                ),
                getString(R.string.updating_languages),
                getString(R.string.please_wait),
                true
            )
            true
        }

        val updateLanguagesFromFileButton =
            findPreference(Settings.KEY_PREF_UPDATE_LANGUAGES_FROM_FILE)
        updateLanguagesFromFileButton.onPreferenceClickListener = OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.setType("application/octet-stream")
            startActivityForResult(intent, FILE_GET_REQUEST_CODE)
            true
        }

        val languagesUrlButton = findPreference(Settings.KEY_PREF_LANGUAGES_URL)
        languagesUrlButton.onPreferenceClickListener = OnPreferenceClickListener {
            val add = LanguagesUrlDialog()
            add.show(fragmentManager, "save")
            false
        }

        val uploadServerButton = findPreference(Settings.KEY_PREF_UPLOAD_SERVER)
        uploadServerButton.onPreferenceClickListener = OnPreferenceClickListener {
            val add = UploadServerDialog()
            add.show(fragmentManager, "save")
            false
        }

        val backupRestoreButton = findPreference(Settings.KEY_PREF_BACKUP_RESTORE)
        backupRestoreButton.onPreferenceClickListener = OnPreferenceClickListener {
            val dialog = BackupRestoreDialog()
            dialog.setListener(this)
            dialog.show(fragmentManager, "backup")
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_GET_REQUEST_CODE) {
                val uri = resultData.data
                mTaskFragment.executeRunnable(
                    ResyncLanguageNamesTask(
                        Settings.RESYNC_LANGUAGE_NAMES_TASK_TAG,
                        activity,
                        db,
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
        mTaskFragment.executeRunnable(
            CreateBackupTask(Settings.BACKUP_TASK_TAG, activity, zipFileUri),
            getString(R.string.creating_backup),
            getString(R.string.please_wait),
            true
        )
    }

    override fun onRestoreBackup(zipFileUri: Uri) {
        mTaskFragment.executeRunnable(
            RestoreBackupTask(Settings.RESTORE_TASK_TAG, activity, zipFileUri),
            getString(R.string.restoring_backup),
            getString(R.string.please_wait),
            true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get rid of the extra padding in the settings page body (where it loads this fragment)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (view != null) {
            val lv = view.findViewById<View>(android.R.id.list) as ListView
            lv.setPadding(0, 0, 0, 0)
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        for (k in mSharedPreferences.all.keys) {
            updateSummaryText(mSharedPreferences, k)
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(mSharedPreferences: SharedPreferences, key: String) {
        updateSummaryText(mSharedPreferences, key)
    }

    private fun updateSummariesSetViaActivities(mSharedPreferences: SharedPreferences) {
        var uristring = mSharedPreferences.getString(
            Settings.KEY_PREF_GLOBAL_SOURCE_LOC,
            ""
        )
        val dir = Uri.parse(uristring)
        if (dir != null) {
            uristring = dir.lastPathSegment
            findPreference(Settings.KEY_PREF_GLOBAL_SOURCE_LOC).summary = uristring
        } else {
            findPreference(Settings.KEY_PREF_GLOBAL_SOURCE_LOC).summary =
                mSharedPreferences.getString(
                    Settings.KEY_PREF_GLOBAL_SOURCE_LOC,
                    ""
                )
        }
    }

    private fun updateSummaryText(mSharedPreferences: SharedPreferences, key: String) {
        try {
            updateSummariesSetViaActivities(mSharedPreferences)
            val text = mSharedPreferences.getString(key, "")
            if (findPreference(key) != null) {
                findPreference(key).summary = text
            }
        } catch (err: ClassCastException) {
            println("IGNORING SUMMARY UPDATE FOR $key")
        }
    }
}