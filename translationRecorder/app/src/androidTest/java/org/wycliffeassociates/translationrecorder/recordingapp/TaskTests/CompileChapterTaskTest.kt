package org.wycliffeassociates.translationrecorder.recordingapp.TaskTests

import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.CompileChapterTask
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.ProjectMockingUtil.createBibleTestProject
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard
import java.util.Arrays
import javax.inject.Inject

/**
 * Created by sarabiaj on 9/28/2017.
 */
@HiltAndroidTest
@RunWith(Parameterized::class)
@SmallTest //This test is to ensure that the files are properly sorted when submitted to the wav compile method
//It assumes that the compile method is tested in a WavFile unit test.
class CompileChapterTaskTest(
    private val mUnsortedList: List<String>,
    private val mSortedList: List<String>,
    private val mProject: Project
) {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun sortFilesTest() {
        Assert.assertEquals(
            "Sorted and unsorted lists should be the same size",
            mUnsortedList.size.toLong(),
            mSortedList.size.toLong()
        )
        val chapterCard = ChapterCard("", 1, mProject, mSortedList.size, db, directoryProvider)
        val map: MutableMap<ChapterCard, List<String>> = HashMap()
        map[chapterCard] = mUnsortedList
        val cct = CompileChapterTask(0, map, mProject, directoryProvider)
        cct.sortFilesInChapter(mUnsortedList)
        for (i in mSortedList.indices) {
            Assert.assertEquals(
                "Unsorted list should be sorted and match the sorted list",
                mUnsortedList[i], mSortedList[i]
            )
        }
    }

    companion object {
        private var mainMenuActivityTestRule: ActivityTestRule<MainMenu> = ActivityTestRule(
            MainMenu::class.java
        )

        /*@Parameterized.Parameters
        fun data(): Iterable<Array<Any>> {
            //Project notesProject = ProjectMockingUtil.createNotesTestProject(mainMenuActivityTestRule);

            val bibleProject = createBibleTestProject(
                mainMenuActivityTestRule,
                directoryProvider
            )

            return Arrays.asList(
                *arrayOf(
                    //Bible Projects
                    arrayOf(
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v01_t01.wav",
                            "en_ulb_b01_gen_c01_v03_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav"
                        ),
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v01_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav",
                            "en_ulb_b01_gen_c01_v03_t01.wav"
                        ),
                        bibleProject
                    ),
                    arrayOf(
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v01_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav",
                            "en_ulb_b01_gen_c01_v03_t01.wav"
                        ),
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v01_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav",
                            "en_ulb_b01_gen_c01_v03_t01.wav"
                        ),
                        bibleProject
                    ),
                    arrayOf(
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v03_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav",
                            "en_ulb_b01_gen_c01_v01_t01.wav"
                        ),
                        mutableListOf(
                            "en_ulb_b01_gen_c01_v01_t01.wav",
                            "en_ulb_b01_gen_c01_v02_t01.wav",
                            "en_ulb_b01_gen_c01_v03_t01.wav"
                        ),
                        bibleProject
                    ),
                )
            )
        }*/
    }
}
