package org.wycliffeassociates.translationrecorder.recordingapp.AdapterTests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityChapterList
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ChapterCardAdapter
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.recordingapp.ProjectMockingUtil
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 9/20/2017.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChapterCardAdapterTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var mActivityChapterListRule: ActivityTestRule<ActivityChapterList> = ActivityTestRule(
        ActivityChapterList::class.java,
        true,
        false
    )

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private var mBibleAdapter: ChapterCardAdapter? = null
    private var mNotesAdapter: ChapterCardAdapter? = null
    private var bibleProject: Project? = null
    private var notesProject: Project? = null

    @Before
    fun setUp() {
        hiltRule.inject()

        bibleProject = ProjectMockingUtil.createBibleTestProject(
            mActivityChapterListRule,
            directoryProvider
        )
        notesProject = ProjectMockingUtil.createNotesTestProject(
            mActivityChapterListRule,
            directoryProvider
        )
        try {
            mBibleAdapter = ChapterCardAdapter(
                mActivityChapterListRule.activity,
                bibleProject!!,
                createChapterCardList(bibleProject!!),
                db
            )
            mNotesAdapter = ChapterCardAdapter(
                mActivityChapterListRule.activity,
                notesProject!!,
                createChapterCardList(notesProject!!),
                db
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun createChapterCardList(project: Project): List<ChapterCard> {
        val pluginLoader = ChunkPluginLoader(directoryProvider, assetsProvider)
        val plugin = project.getChunkPlugin(pluginLoader)
        val chapters = plugin.chapters
        val cards: List<ChapterCard> = ArrayList()
        for (chapter in chapters) {
//            cards.add(new ChapterCard(
//                    mActivityChapterListRule.getActivity(),
//                    project,
//                    chapter.getNumber(),
//                    chapter.getChunks().size()
//            ));
        }
        return cards
    }
}
