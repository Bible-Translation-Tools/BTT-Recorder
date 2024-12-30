package org.wycliffeassociates.translationrecorder.recordingapp.UnitTests.project

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.createFile
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.Version
import javax.inject.Inject

/**
 * Created by sarabiaj on 4/27/2017.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProjectFileUtilsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var directoryProvider: IDirectoryProvider

    var regex: String =
        ("([a-zA-Z]{2,3}[-[\\d\\w]+]*)_([a-zA-Z]{3})_b([\\d]{2})_([1-3]*[a-zA-Z]+)_c([\\d]{2,3})"
                + "_v([\\d]{2,3})(-([\\d]{2,3}))?(_t([\\d]{2}))?(.wav)?")

    //lang, version booknum book chapter start end take
    private var groups: String = "1 2 3 4 5 6 8 10"
    private var language: Language = Language("en", "English")
    private var book: Book = Book(context, "gen", "Genesis", "ot", 1)
    private var version: Version = Version("ulb", "Unlocked Literal Bible")
    private var mask: String = "1111111111"
    private var sort: Int = 1

    private var jarName: String = "biblechunk.jar"
    private var className: String = "org.wycliffeassociates.translationrecorder.biblechunk.BibleChunkPlugin"
    private var anthology: Anthology = Anthology(
        "ot",
        "Old Testament",
        "bible",
        sort,
        regex,
        groups,
        mask,
        jarName,
        className
    )
    var mode: Mode = Mode("chunk", "chunk", "chunk")
    var project: Project = Project(language, anthology, book, version, mode)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testCreateFile() {
        val file = createFile(project, 1, 1, 2, directoryProvider)

        val ppm = project.patternMatcher
        ppm.match(file)
        Assert.assertEquals(true, ppm.matched())
        val slugs = ppm.projectSlugs
        Assert.assertEquals(language.slug, slugs!!.language)
        Assert.assertEquals(book.slug, slugs.book)
        Assert.assertEquals(version.slug, slugs.version)
        Assert.assertEquals(book.order.toLong(), slugs.bookNumber.toLong())

        val info = ppm.takeInfo
        Assert.assertEquals(1, info!!.chapter.toLong())
        Assert.assertEquals(1, info.startVerse.toLong())
        Assert.assertEquals(2, info.endVerse.toLong())
    }

    @Test
    fun testGetNameFromProject() {
        val name = project.getFileName(1, 1, 2)
        val expected = "en_ulb_b01_gen_c01_v01-02"
        Assert.assertEquals(expected, name)

        val name2 = project.getFileName(1, 1, -1)
        val expected2 = "en_ulb_b01_gen_c01_v01"
        Assert.assertEquals(expected, name)
    }

    @Test
    fun testGetParentDirectory() {
        val name = project.getFileName(1, 1, 2)
        val expected = "en_ulb_b01_gen_c01_v01-02_t01.wav"
        val ti = TakeInfo(project.projectSlugs, 1, 1, 2, 1)
        //        ProjectFileUtils.getParentDirectory();
        Assert.assertEquals(expected, name)

        val name2 = project.getFileName(1, 1, -1)
        val expected2 = "en_ulb_b01_gen_c01_v01"
        Assert.assertEquals(expected, name)
    }
}
