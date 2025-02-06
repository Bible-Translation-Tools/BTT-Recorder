package org.wycliffeassociates.translationrecorder.recordingapp.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkDialogText
import org.wycliffeassociates.translationrecorder.recordingapp.UiTestUtils.checkText
import org.wycliffeassociates.translationrecorder.recordingapp.tryCheck
import org.wycliffeassociates.translationrecorder.recordingapp.tryPerform
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun showSettings() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            checkText(R.string.app_settings, true)
            checkText(R.string.app_version, true)
            checkText(R.string.source_audio_location, true)
            checkText(R.string.source_audio_language, true)
            checkText(R.string.add_temp_language, true)
            checkText(R.string.update_language_from_td, true)
            checkText(R.string.update_language_from_file, true)
            checkText(R.string.change_languages_url, true)
            checkText(R.string.change_upload_server, true)
            checkText(R.string.migrate_old_app, true)
            checkText(R.string.backup_restore, true)
        }
    }

    @Test
    fun sourceAudioLocation() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.source_audio_location)).tryPerform(click())

            Intents.intended(hasAction("org.wycliffeassociates.translationrecorder.SettingsPage.SELECT_SRC_INTENT"))
        }
    }

    @Test
    fun sourceAudioLanguage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.source_audio_language)).tryPerform(click())

            val hint = context.getString(R.string.choose_source_language)
            onView(withHint(containsString(hint))).tryCheck(matches(isDisplayed()))
        }
    }

    @Test
    fun addTemporaryLanguage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.add_temp_language)).tryPerform(click())

            checkDialogText(R.string.add_temp_language, true)
            checkDialogText(R.string.language_name, true)
            checkDialogText(R.string.language_code, true)
        }
    }

    @Test
    fun updateLanguagesFromUrl() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.update_language_from_td)).tryPerform(click())
        }
    }

    @Test
    fun updateLanguagesFromFile() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.update_language_from_file)).tryPerform(click())

            Intents.intended(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("*/*")
            ))
        }
    }

    @Test
    fun changeLanguagesUrl() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.change_languages_url)).tryPerform(click())

            checkDialogText(R.string.change_languages_url, true)
            checkDialogText(R.string.lang_url, true)
            checkDialogText(R.string.label_close, true)
            checkDialogText(R.string.restore_defaults, true)
            checkDialogText(R.string.save, true)
        }
    }

    @Test
    fun changeUploadServer() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.change_upload_server)).tryPerform(click())

            checkDialogText(R.string.change_upload_server, true)
            checkDialogText(R.string.server_name, true)
            checkDialogText(R.string.label_close, true)
            checkDialogText(R.string.restore_defaults, true)
            checkDialogText(R.string.save, true)
        }
    }

    @Test
    fun migrateOldApp() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.migrate_old_app)).tryPerform(click())

            Intents.intended(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
            ))
        }
    }

    @Test
    fun backupRestore() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withText(R.string.backup_restore)).tryPerform(click())

            checkDialogText(R.string.backup_restore, true)
            checkDialogText(R.string.label_close, true)
            checkDialogText(R.string.backup, true)
            checkDialogText(R.string.restore, true)
        }
    }
}