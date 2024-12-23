package org.wycliffeassociates.translationrecorder.project

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityScrollableListBinding
import org.wycliffeassociates.translationrecorder.project.adapters.GenericAdapter
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.Version
import javax.inject.Inject

/**
 * Created by sarabiaj on 5/27/2016.
 */
@AndroidEntryPoint
class ProjectWizardActivity : AppCompatActivity(), ScrollableListFragment.OnItemClickListener {
    @Inject lateinit var db: IProjectDatabaseHelper

    var mFragment: ScrollableListFragment? = null
    var mSearchText: String? = null

    private lateinit var mSearchViewAction: SearchView

    private var targetLanguage: Language? = null
    private var anthology: Anthology? = null
    private var book: Book? = null
    private var version: Version? = null
    private var mode: Mode? = null
    private var sourceLanguage: Language? = null
    private var sourceLocation: String? = null

    private var mCurrentFragment = BASE_PROJECT
    private var mLastFragment = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayFragment()

        val binding = ActivityScrollableListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setTitle(R.string.new_project)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(TARGET_LANGUAGE_KEY, targetLanguage)
        outState.putParcelable(ANTHOLOGY_KEY, anthology)
        outState.putParcelable(BOOK_KEY, book)
        outState.putParcelable(VERSION_KEY, version)
        outState.putParcelable(MODE_KEY, mode)
        outState.putInt(CURRENT_FRAGMENT_KEY, mCurrentFragment)
        outState.putInt(LAST_FRAGMENT_KEY, mLastFragment)
        outState.putString(SEARCH_TEXT_KEY, mSearchText)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        targetLanguage = savedInstanceState.getParcelable(TARGET_LANGUAGE_KEY)
        anthology = savedInstanceState.getParcelable(ANTHOLOGY_KEY)
        book = savedInstanceState.getParcelable(BOOK_KEY)
        version = savedInstanceState.getParcelable(VERSION_KEY)
        mode = savedInstanceState.getParcelable(MODE_KEY)
        mCurrentFragment = savedInstanceState.getInt(CURRENT_FRAGMENT_KEY)
        mLastFragment = savedInstanceState.getInt(LAST_FRAGMENT_KEY)
        mSearchText = savedInstanceState.getString(SEARCH_TEXT_KEY)
        displayFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.language_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_update).setVisible(false)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.action_search)
        mSearchViewAction = searchMenuItem.actionView as SearchView
        mSearchViewAction.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return true
            }
            override fun onQueryTextChange(s: String): Boolean {
                mSearchText = s
                mFragment!!.onSearchQuery(s)
                return true
            }
        })
        mSearchViewAction.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (mSearchText != null) {
            mSearchViewAction.setQuery(mSearchText, true)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SOURCE_AUDIO_REQUEST && resultCode == RESULT_OK) {
            sourceLanguage = data?.getParcelableExtra(Project.SOURCE_LANGUAGE_EXTRA)
            sourceLocation = data?.getStringExtra(Project.SOURCE_LOCATION_EXTRA)

            val project = Project(
                targetLanguage!!,
                anthology!!,
                book!!,
                version!!,
                mode!!,
                sourceLanguage = sourceLanguage,
                sourceAudioPath = sourceLocation
            )

            val intent = Intent()
            intent.putExtra(Project.PROJECT_EXTRA, project)
            setResult(RESULT_OK, intent)
            finish()
        } else if (resultCode == RESULT_CANCELED) {
            mCurrentFragment = mLastFragment
            this.displayFragment()
        }
    }

    private fun clearSearchState() {
        mSearchText = ""
        mSearchViewAction.onActionViewCollapsed()
    }

    override fun onItemClick(result: Any?) {
        clearSearchState()
        Utils.closeKeyboard(this)
        if (mCurrentFragment == TARGET_LANGUAGE && result is Language) {
            targetLanguage = result
            mCurrentFragment++
            displayFragment()
        } else if (mCurrentFragment == PROJECT && result is Anthology) {
            anthology = result
            mLastFragment = mCurrentFragment
            mCurrentFragment =
                if (anthology?.slug?.compareTo("hjklhjkhkl") == 0) SOURCE_LANGUAGE else BOOK
            displayFragment()
        } else if (mCurrentFragment == BOOK && result is Book) {
            book = result
            mCurrentFragment++
            displayFragment()
        } else if (mCurrentFragment == SOURCE_TEXT && result is Version) {
            version = result
            mCurrentFragment++
            displayFragment()
        } else if (mCurrentFragment == MODE && result is Mode) {
            mode = result
            mLastFragment = mCurrentFragment
            mCurrentFragment++
            displayFragment()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        clearSearchState()
        Utils.closeKeyboard(this)
        when (item.itemId) {
            android.R.id.home -> {
                if (mCurrentFragment > TARGET_LANGUAGE) {
                    mCurrentFragment--
                    this.displayFragment()
                } else {
                    this.finish()
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun displayFragment() {
        // Remove old fragment, if there's any
        if (mFragment != null) {
            supportFragmentManager.beginTransaction().remove(mFragment!!).commit()
        }
        // Build a new fragment based on the current step
        mFragment =
            when (mCurrentFragment) {
                TARGET_LANGUAGE -> ScrollableListFragment.Builder(
                    GenericAdapter(db.languages, this)
                )
                    .setSearchHint(getString(R.string.choose_target_language))
                    .build()

                PROJECT -> ScrollableListFragment.Builder(
                    GenericAdapter(db.anthologies, this)
                )
                    .setSearchHint(getString(R.string.choose_project))
                    .build()

                BOOK -> ScrollableListFragment.Builder(
                    GenericAdapter(getBooksList(anthology!!.slug), this)
                )
                    .setSearchHint(getString(R.string.choose_book))
                    .build()

                SOURCE_TEXT -> ScrollableListFragment.Builder(
                    GenericAdapter(getVersionsList(anthology!!.slug), this)
                )
                    .setSearchHint(getString(R.string.choose_translation_type))
                    .build()

                MODE -> ScrollableListFragment.Builder(
                    GenericAdapter(getModeList(anthology!!.slug), this)
                )
                    .setSearchHint(getString(R.string.choose_mode))
                    .build()

                else -> null
            }
        if (mFragment != null) {
            // Display fragment if a new one is built
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, mFragment!!).commit()
        } else {
            // Route to SourceAudioActivity if there's no new fragment
            val intent = Intent(this, SourceAudioActivity::class.java)
            startActivityForResult(intent, SOURCE_AUDIO_REQUEST)
        }
    }

    private fun getBooksList(anthologySlug: String): List<Book> {
        val books = db.getBooks(anthologySlug)
        return books
    }

    private fun getVersionsList(anthologySlug: String): List<Version> {
        val versions = db.getVersions(anthologySlug)
        return versions
    }

    private fun getModeList(anthologySlug: String): List<Mode> {
        val modes = db.getModes(anthologySlug)
        return modes
    }

    companion object {
        private const val TARGET_LANGUAGE_KEY: String = "target_language_key"
        private const val ANTHOLOGY_KEY: String = "anthology_key"
        private const val BOOK_KEY: String = "book_key"
        private const val VERSION_KEY: String = "version_key"
        private const val MODE_KEY: String = "mode_key"
        private const val CURRENT_FRAGMENT_KEY: String = "current_fragment_key"
        private const val LAST_FRAGMENT_KEY: String = "last_fragment_key"
        private const val SEARCH_TEXT_KEY: String = "search_text_key"

        const val BASE_PROJECT: Int = 1
        const val TARGET_LANGUAGE: Int = BASE_PROJECT
        const val PROJECT: Int = BASE_PROJECT + 1
        const val BOOK: Int = BASE_PROJECT + 2
        const val SOURCE_TEXT: Int = BASE_PROJECT + 3
        const val MODE: Int = BASE_PROJECT + 4
        const val SOURCE_LANGUAGE: Int = BASE_PROJECT + 5

        private const val SOURCE_AUDIO_REQUEST = 42

        fun displayProjectExists(context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.project_exists)
            builder.setMessage(
                TranslationRecorderApp.getContext().resources.getString(R.string.project_exists_message)
            )
            builder.setPositiveButton(
                context.getString(R.string.label_ok)
            ) { dialogInterface, i -> dialogInterface.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }
    }
}
