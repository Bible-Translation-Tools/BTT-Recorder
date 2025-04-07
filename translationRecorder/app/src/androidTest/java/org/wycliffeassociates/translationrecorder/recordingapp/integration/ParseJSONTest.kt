package org.wycliffeassociates.translationrecorder.recordingapp.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.wycliffeassociates.translationrecorder.recordingapp.IntegrationTest
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.project.ParseJSON
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@IntegrationTest
class ParseJSONTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var assetsProvider: AssetsProvider

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun parseJson() {
        val parse = ParseJSON(assetsProvider, context)

        val languages = parse.pullLangNames()
        val books = parse.pullBooks()

        assertTrue(languages.isNotEmpty())
        assertTrue(books.isNotEmpty())

        assertEquals("English", languages.find { it.slug == "en" }?.name)
        assertEquals("gen-ch-33", books.find { it.slug == "gen-ch-33" }?.name)
    }
}