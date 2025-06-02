package org.wycliffeassociates.translationrecorder.SettingsPage

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityProjectManager.Companion.DATABASE_RESYNC_TASK
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.CreateBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.MigrateAppFolderTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.RestoreBackupTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ProjectListResyncTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ResyncLanguageNamesTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.BackupRestoreDialog.BackupRestoreDialogListener
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.BACKUP_TASK_TAG
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_ADD_LANGUAGE
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_BACKUP_RESTORE
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_GLOBAL_LANG_SRC
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_GLOBAL_SOURCE_LOC
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_LANGUAGES_URL
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_MIGRATE_OLD_APP
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_UPDATE_LANGUAGES
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_UPDATE_LANGUAGES_FROM_FILE
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_UPDATE_LANGUAGES_URL
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.KEY_PREF_UPLOAD_SERVER
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.MIGRATE_TASK_TAG
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.RESTORE_TASK_TAG
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity.Companion.RESYNC_LANGUAGE_NAMES_TASK_TAG
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.usecases.CreateBackup
import org.wycliffeassociates.translationrecorder.usecases.MigrateOldApp
import org.wycliffeassociates.translationrecorder.usecases.RestoreBackup
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
    @Inject lateinit var migrateOldApp: MigrateOldApp
    @Inject lateinit var createBackup: CreateBackup
    @Inject lateinit var restoreBackup: RestoreBackup

    private var parent: LanguageSelector? = null
    private lateinit var taskFragment: TaskFragment

    private lateinit var openDirectory: ActivityResultLauncher<Uri?>
    private lateinit var openLanguagesFile: ActivityResultLauncher<String>

    interface LanguageSelector {
        fun sourceLanguageSelected()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskFragment = (parentFragmentManager.findFragmentByTag(TAG_TASK_FRAGMENT) as? TaskFragment) ?: run {
            val fragment = TaskFragment()
            parentFragmentManager.beginTransaction().add(fragment, TAG_TASK_FRAGMENT).commit()
            fragment
        }

        val handler = Handler(Looper.getMainLooper())
        activity?.intent?.let { handler.post { handleIntent(it) } }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.fitsSystemWindows = true

        openDirectory = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.let(::migrate)
        }

        openLanguagesFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                taskFragment.executeRunnable(
                    ResyncLanguageNamesTask(
                        RESYNC_LANGUAGE_NAMES_TASK_TAG,
                        requireContext(),
                        db,
                        assetsProvider,
                        requireContext(),
                        it
                    ),
                    getString(R.string.updating_languages),
                    getString(R.string.please_wait),
                    true
                )
            }
        }

        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        parent = activity as LanguageSelector

        preferenceManager.createPreferenceScreen(requireContext())
        addPreferencesFromResource(R.xml.preference)

        updateSummaryText(KEY_PREF_GLOBAL_LANG_SRC)
        updateSummaryText(KEY_PREF_LANGUAGES_URL)
        updateSummaryText(KEY_PREF_UPLOAD_SERVER)

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
            openLanguagesFile.launch("*/*")
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

        val migratePref: Preference? = findPreference(KEY_PREF_MIGRATE_OLD_APP)
        migratePref?.setOnPreferenceClickListener {
            openDirectory.launch(null)
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

    private fun migrate(uri: Uri) {
        taskFragment.executeRunnable(
            MigrateAppFolderTask(MIGRATE_TASK_TAG, uri, migrateOldApp),
            getString(R.string.migrating_old_app),
            getString(R.string.please_wait),
            true
        )
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

    override fun onCreateBackup(zipFileUri: Uri) {
        taskFragment.executeRunnable(
            CreateBackupTask(BACKUP_TASK_TAG, zipFileUri, createBackup),
            getString(R.string.creating_backup),
            getString(R.string.please_wait),
            true
        )
    }

    override fun onRestoreBackup(zipFileUri: Uri) {
        taskFragment.executeRunnable(
            RestoreBackupTask(RESTORE_TASK_TAG, zipFileUri, restoreBackup),
            getString(R.string.restoring_backup),
            getString(R.string.please_wait),
            true
        )
    }

    private fun updateSummaryText(key: String) {
        try {
            var summary = prefs.getDefaultPref(key, "")
            val pref = findPreference(key) as? Preference

            when (key) {
                KEY_PREF_GLOBAL_SOURCE_LOC -> {
                    val file = File(summary)
                    if (file.exists()) {
                        summary = file.name
                    }
                }
                KEY_PREF_LANGUAGES_URL -> {
                    if (summary.isEmpty()) {
                        summary = getString(R.string.pref_languages_url)
                    }
                }
                KEY_PREF_UPLOAD_SERVER -> {
                    if (summary.isEmpty()) {
                        summary = getString(R.string.pref_upload_server)
                    }
                }
            }
            pref?.summary = summary
        } catch (e: ClassCastException) {
            println("IGNORING SUMMARY UPDATE FOR $key")
        }
    }

    fun resyncProjectList() {
        val task = ProjectListResyncTask(
            DATABASE_RESYNC_TASK,
            parentFragmentManager,
            db,
            directoryProvider,
            ChunkPluginLoader(directoryProvider, assetsProvider),
            true
        )
        taskFragment.executeRunnable(
            task,
            getString(R.string.resyncing_database),
            getString(R.string.please_wait),
            true
        )
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("application/zip") == true) {
                    val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    fileUri?.let { uri: Uri ->
                        onRestoreBackup(uri)
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG_TASK_FRAGMENT = "tag_task_fragment"
    }
}