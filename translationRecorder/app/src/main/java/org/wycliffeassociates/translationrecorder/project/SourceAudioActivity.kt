package org.wycliffeassociates.translationrecorder.project

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Utils
import org.wycliffeassociates.translationrecorder.databinding.ActivitySourceAudioBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.project.adapters.TargetLanguageAdapter
import org.wycliffeassociates.translationrecorder.project.components.Language
import javax.inject.Inject

/**
 * Created by sarabiaj on 5/25/2016.
 */
@AndroidEntryPoint
class SourceAudioActivity : AppCompatActivity(), ScrollableListFragment.OnItemClickListener {

    @Inject lateinit var assetsProvider: AssetsProvider

    private var sourceLanguage: Language? = null
    private var sourceLocation: String? = null

    private var mSetLocation = false
    private var mSetLanguage = false
    private var mFragment: Searchable? = null
    private var mSearchText: String? = null
    private var mShowSearch = false

    private lateinit var binding: ActivitySourceAudioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySourceAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languageBtn.setOnClickListener(btnClick)
        binding.locationBtn.setOnClickListener(btnClick)
        binding.continueBtn.setOnClickListener(btnClick)

        val decorView = window.decorView
        // Hide the status bar.
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions

//        // Remember that you should never show the action bar if the
//        // status bar is hidden, so hide that too if necessary.
//        ActionBar actionBar = getActionBar();
//        actionBar.hide();
        supportActionBar?.title = getString(R.string.source_audio)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SOURCE_LANGUAGE_KEY, sourceLanguage)
        outState.putString(SOURCE_LOCATION_KEY, sourceLocation)
        outState.putBoolean(mSetLanguageKey, mSetLanguage)
        outState.putBoolean(mSetLocationKey, mSetLocation)
        outState.putString(mSearchTextKey, mSearchText)
        outState.putBoolean(mUserSearchingLanguageKey, mFragment != null)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        sourceLanguage = savedInstanceState.getParcelable(SOURCE_LANGUAGE_KEY)
        sourceLocation = savedInstanceState.getString(SOURCE_LOCATION_KEY)
        mSetLanguage = savedInstanceState.getBoolean(mSetLanguageKey)
        mSetLocation = savedInstanceState.getBoolean(mSetLocationKey)
        if (mSetLocation) {
            binding.locationBtn.text = resources.getString(
                R.string.source_location_selected,
                sourceLocation
            )
        }
        if (mSetLanguage) {
            binding.languageBtn.text = resources.getString(
                R.string.source_language_selected,
                sourceLanguage?.slug
            )
        }
        if (savedInstanceState.getBoolean(mUserSearchingLanguageKey)) {
            mSearchText = savedInstanceState.getString(mSearchTextKey)
            setSourceLanguage()
        } else {
            continueIfBothSet()
        }
    }

    override fun onBackPressed() {
        //if the source language fragment is showing, then close that, otherwise proceed with back press
        if (findViewById<View>(R.id.fragment_container).visibility == View.VISIBLE) {
            findViewById<View>(R.id.fragment_container).visibility =
                View.INVISIBLE
            hideSearchMenu()
        } else {
            super.onBackPressed()
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.language_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_update).setVisible(false)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchMenuItem.setVisible(mShowSearch)
        val searchViewAction = searchMenuItem.actionView as SearchView
        searchViewAction.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return true
            }
            override fun onQueryTextChange(s: String): Boolean {
                mSearchText = s
                if (mFragment != null) {
                    mFragment!!.onSearchQuery(s)
                }
                return true
            }
        })
        searchViewAction.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (mSearchText != null) {
            searchViewAction.setQuery(mSearchText, true)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (binding.fragmentContainer.visibility == View.VISIBLE) {
                    binding.fragmentContainer.visibility = View.INVISIBLE
                } else {
                    finish()
                    return true
                }
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setSourceLanguage() {
        if (mFragment != null) {
            supportFragmentManager
                .beginTransaction()
                .remove(mFragment as Fragment)
                .commit()
        }

        mFragment = ScrollableListFragment.Builder(
            TargetLanguageAdapter(ParseJSON.getLanguages(assetsProvider), this)
        ).setSearchHint(getString(R.string.choose_source_language) + ":").build()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, mFragment as Fragment)
            .commit()
        binding.fragmentContainer.visibility = View.VISIBLE
    }

    private fun setSourceLocation() {
        startActivityForResult(
            Intent(this, SelectSourceDirectory::class.java),
            REQUEST_SOURCE_LOCATION
        )
    }

    private fun proceed() {
        val intent = Intent()
        intent.putExtra(Project.SOURCE_LANGUAGE_EXTRA, sourceLanguage)
        intent.putExtra(Project.SOURCE_LOCATION_EXTRA, sourceLocation)
        setResult(RESULT_OK, intent)
        finish()
    }

    private val btnClick = View.OnClickListener { v ->
        when (v.id) {
            R.id.location_btn -> {
                setSourceLocation()
            }
            R.id.language_btn -> {
                displaySearchMenu()
                setSourceLanguage()
            }
            R.id.continue_btn -> {
                proceed()
            }
        }
    }

    private fun displaySearchMenu() {
        mShowSearch = true
        invalidateOptionsMenu()
        mSearchText = ""
    }

    private fun hideSearchMenu() {
        mShowSearch = false
        invalidateOptionsMenu()
        mSearchText = ""
    }

    private fun continueIfBothSet() {
        if (mSetLocation && mSetLanguage) {
            binding.continueBtn.text = getString(R.string.label_continue)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SOURCE_LOCATION) {
            if (data?.hasExtra(SelectSourceDirectory.SOURCE_LOCATION) == true) {
                sourceLocation = data.getStringExtra(SelectSourceDirectory.SOURCE_LOCATION)
                binding.locationBtn.text = resources.getString(
                    R.string.source_location_selected,
                    sourceLocation
                )
                mSetLocation = true
                continueIfBothSet()
            }
        }
    }

    override fun onItemClick(result: Any) {
        Utils.closeKeyboard(this)
        hideSearchMenu()
        sourceLanguage = result as Language
        binding.languageBtn.text = resources.getString(
            R.string.source_language_selected,
            sourceLanguage?.slug
        )
        mSetLanguage = true
        supportFragmentManager.beginTransaction().remove(mFragment as Fragment).commit()
        binding.fragmentContainer.visibility = View.INVISIBLE
        continueIfBothSet()
    }

    companion object {
        private const val mSetLanguageKey = "set_language_key"
        private const val mSetLocationKey = "set_location_key"
        private const val SOURCE_LANGUAGE_KEY = "source_language_key"
        private const val SOURCE_LOCATION_KEY = "source_location_key"
        private const val mUserSearchingLanguageKey = "searching_language_key"
        private const val mSearchTextKey = "search_text_key"
        private const val REQUEST_SOURCE_LOCATION = 42
        private const val REQUEST_SOURCE_LANGUAGE = 43

        fun getSourceAudioIntent(ctx: Context): Intent {
            val intent = Intent(ctx, SourceAudioActivity::class.java)
            return intent
        }
    }
}
