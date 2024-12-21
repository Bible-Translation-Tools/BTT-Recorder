package org.wycliffeassociates.translationrecorder.SettingsPage

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsFragment.LanguageSelector
import org.wycliffeassociates.translationrecorder.SplashScreen
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.ScrollableListFragment
import org.wycliffeassociates.translationrecorder.project.adapters.TargetLanguageAdapter
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment.OnTaskComplete
import javax.inject.Inject
import kotlin.system.exitProcess

/**
 *
 * The settings page -- for all persistent options/information.
 *
 */
@AndroidEntryPoint
class Settings : AppCompatActivity(), OnTaskComplete, ScrollableListFragment.OnItemClickListener,
    LanguageSelector {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository

    private var searchText: String? = null
    private var fragment: Fragment? = null
    private var showSearch = false
    private var displayingList: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayingList = false

        supportActionBar?.setTitle(R.string.settings_menu)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setContentView(R.layout.settings)
        PreferenceManager.setDefaultValues(this, R.xml.preference, false)
    }

    override fun onBackPressed() {
        if (displayingList) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_scroll_list)
            if (fragment != null) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
            displayingList = false
            hideSearchMenu()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                Utils.closeKeyboard(this)
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.language_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (showSearch) {
            //if(mFragment instanceof LanguageListFragment) {
            menu.findItem(R.id.action_update).setVisible(false)
            menu.findItem(R.id.action_search).setVisible(true)

            val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
            val searchMenuItem = menu.findItem(R.id.action_search)
            val searchViewAction = searchMenuItem.actionView as SearchView
            searchViewAction.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(s: String): Boolean {
                    return true
                }
                override fun onQueryTextChange(s: String): Boolean {
                    searchText = s
                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_scroll_list)
                    //Seems to sometimes pull SettingsFragment instead and thus cannot cast?
                    if (fragment is ScrollableListFragment) {
                        fragment.onSearchQuery(s)
                    }
                    return true
                }
            })
            searchViewAction.setSearchableInfo(searchManager.getSearchableInfo(componentName))

            if (searchText != null) {
                searchViewAction.setQuery(searchText, true)
            }
        } else {
            menu.findItem(R.id.action_update).setVisible(false)
            menu.findItem(R.id.action_search).setVisible(false)
        }
        return true
    }

    override fun onItemClick(result: Any) {
        Utils.closeKeyboard(this)
        prefs.setDefaultPref(KEY_PREF_GLOBAL_LANG_SRC, (result as Language).slug)
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_scroll_list)
        if (fragment != null) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
        displayingList = false
        hideSearchMenu()
    }

    private fun displaySearchMenu() {
        showSearch = true
        invalidateOptionsMenu()
        searchText = ""
    }

    private fun hideSearchMenu() {
        showSearch = false
        invalidateOptionsMenu()
        searchText = ""
    }

    override fun sourceLanguageSelected() {
        displaySearchMenu()
        displayingList = true
        fragment = ScrollableListFragment.Builder(TargetLanguageAdapter(db.languages, this))
            .setSearchHint(getString(R.string.choose_source_language) + ":")
            .build()
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_scroll_list, fragment!!).commit()
    }

    override fun onTaskComplete(taskTag: Int, resultCode: Int) {
        if (resultCode == TaskFragment.STATUS_OK) {
            when (taskTag) {
                RESTORE_TASK_TAG -> {
                    val intent = Intent(this, SplashScreen::class.java)
                    startActivity(intent)
                    this.finishAffinity()
                    exitProcess(0)
                }
            }
        }
    }

    companion object {
        const val KEY_RECENT_PROJECT_ID: String = "pref_recent_project_id"

        const val KEY_PREF_CHAPTER: String = "pref_chapter"
        const val KEY_PREF_CHUNK: String = "pref_chunk"
        const val KEY_PREF_TAKE: String = "pref_take"
        const val KEY_PREF_CHUNK_VERSE: String = "pref_chunk_verse"
        const val KEY_PREF_START_VERSE: String = "pref_start_verse"
        const val KEY_PREF_END_VERSE: String = "pref_end_verse"
        const val KEY_PREF_SRC_LOC: String = "pref_src_loc"
        const val KEY_SDK_LEVEL: String = "pref_sdk_level"
        const val KEY_PROFILE: String = "pref_profile"
        const val KEY_USER: String = "pref_profile"

        const val KEY_PREF_GLOBAL_SOURCE_LOC: String = "pref_global_src_loc"
        const val KEY_PREF_GLOBAL_LANG_SRC: String = "pref_global_lang_src"
        const val KEY_PREF_ADD_LANGUAGE: String = "pref_add_temp_language"
        const val KEY_PREF_UPDATE_LANGUAGES: String = "pref_update_languages"
        const val KEY_PREF_UPDATE_LANGUAGES_URL: String = "pref_languages_url"
        const val KEY_PREF_UPDATE_LANGUAGES_FROM_FILE: String = "pref_update_languages_from_file"

        const val KEY_PREF_UPLOAD_SERVER: String = "pref_upload_server"
        const val KEY_PREF_LANGUAGES_URL: String = "pref_languages_url"

        const val KEY_PREF_BACKUP_RESTORE: String = "pref_backup_restore"

        const val RESYNC_LANGUAGE_NAMES_TASK_TAG: Int = 1
        const val BACKUP_TASK_TAG: Int = 2
        const val RESTORE_TASK_TAG: Int = 3
    }
}