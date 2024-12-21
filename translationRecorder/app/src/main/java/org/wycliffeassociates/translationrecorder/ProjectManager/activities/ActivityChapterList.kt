package org.wycliffeassociates.translationrecorder.ProjectManager.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ChapterCardAdapter
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CheckingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CompileDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.CompileChapterTask
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.ChapterResyncTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityChapterListBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.utilities.Task
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment.OnTaskComplete
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard
import javax.inject.Inject

/**
 * Created by sarabiaj on 6/28/2016.
 */
@AndroidEntryPoint
class ActivityChapterList : AppCompatActivity(), CheckingDialog.DialogListener,
    CompileDialog.DialogListener,
    OnTaskComplete {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private var mChunks: ChunkPlugin? = null
    private var mPd: ProgressDialog? = null

    private val mIsCompiling = false
    private lateinit var mProject: Project
    private var mChapterCardList: MutableList<ChapterCard> = arrayListOf()
    private var mAdapter: ChapterCardAdapter? = null

    private val mProgress = 0
    private var mDbResyncing = false
    private var mTaskFragment: TaskFragment? = null
    private var mChaptersCompiled: IntArray? = null

    private lateinit var binding: ActivityChapterListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fm: FragmentManager = supportFragmentManager
        mTaskFragment = fm.findFragmentByTag(TAG_TASK_FRAGMENT) as TaskFragment?
        if (mTaskFragment == null) {
            mTaskFragment = TaskFragment()
            fm.beginTransaction().add(mTaskFragment!!, TAG_TASK_FRAGMENT).commit()
            fm.executePendingTransactions()
        }

        if (savedInstanceState != null) {
            mDbResyncing = savedInstanceState.getBoolean(STATE_RESYNC)
        }

        // Setup toolbar
        mProject = intent.getParcelableExtra(Project.PROJECT_EXTRA)!!
        val language = db.getLanguageName(mProject.targetLanguageSlug)
        val book = mProject.bookName

        val mToolbar = findViewById<View>(R.id.chapter_list_toolbar) as Toolbar
        setSupportActionBar(mToolbar)
        supportActionBar?.title = "$language - $book"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        try {
            mChunks = mProject.getChunkPlugin(ChunkPluginLoader(
                directoryProvider,
                assetsProvider
            ))
        } catch (e: Exception) {
            Logger.e(this.toString(), "Error parsing chunks", e)
        }

        // Find the recycler view
        binding.chapterList.setHasFixedSize(false)

        // Set its layout manager
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.chapterList.layoutManager = layoutManager

        // Set its adapter
        mChapterCardList = ArrayList()
        mAdapter = ChapterCardAdapter(this, mProject, mChapterCardList, db)
        binding.chapterList.adapter = mAdapter

        // Set its animator
        binding.chapterList.itemAnimator = DefaultItemAnimator()

        prepareChapterCardData()
    }

    override fun onResume() {
        super.onResume()

        //if the database is still resyncing from a previous orientation change, don't start a new one
        if (!mDbResyncing) {
            mDbResyncing = true
            val task = ChapterResyncTask(
                DATABASE_RESYNC_TASK,
                supportFragmentManager,
                mProject,
                db,
                directoryProvider
            )
            mTaskFragment!!.executeRunnable(task, "Resyncing Database", "Please wait...", true)
        }
    }

    fun refreshChapterCards() {
        val unitsStarted = db.getNumStartedUnitsInProject(
            mProject
        )
        for (i in mChapterCardList.indices) {
            val cc = mChapterCardList[i]
            val numUnits = if ((unitsStarted.containsKey(cc.chapterNumber))) {
                unitsStarted[cc.chapterNumber]!!
            } else 0
            cc.setNumOfUnitStarted(numUnits)
            cc.refreshProgress()
            cc.refreshIsEmpty()
            cc.refreshCanCompile()
            cc.refreshChapterCompiled(cc.chapterNumber)
            if (cc.isCompiled) {
                cc.checkingLevel = db.getChapterCheckingLevel(mProject, cc.chapterNumber)
            }
        }
        mAdapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        mAdapter!!.exitCleanUp()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mPd != null && mPd!!.isShowing) {
            mPd!!.dismiss()
            mPd = null
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_COMPILING, mIsCompiling)
        outState.putInt(STATE_PROGRESS, mProgress)
        outState.putBoolean(STATE_RESYNC, mDbResyncing)
        super.onSaveInstanceState(outState)
    }

    override fun onPositiveClick(dialog: CheckingDialog) {
        val level = dialog.checkingLevel
        val chapterIndices = dialog.chapterIndicies
        for (i in chapterIndices!!.indices) {
            val position = chapterIndices[i]
            val cc = mChapterCardList[position]
            cc.checkingLevel = level
            db.setCheckingLevel(dialog.project!!, cc.chapterNumber, level)
            mAdapter!!.notifyItemChanged(position)
        }
    }

    override fun onPositiveClick(dialog: CompileDialog) {
        val toCompile: MutableList<ChapterCard> = ArrayList()
        for (i in dialog.chapterIndicies!!) {
            toCompile.add(mChapterCardList[i])
            mChapterCardList[i].destroyAudioPlayer()
        }
        mChaptersCompiled = dialog.chapterIndicies

        val chaptersToCompile: MutableMap<ChapterCard, List<String>> = HashMap()
        for (cc in toCompile) {
            chaptersToCompile[cc] = db.getTakesForChapterCompilation(mProject, cc.chapterNumber)
        }

        val task = CompileChapterTask(
            COMPILE_CHAPTER_TASK,
            chaptersToCompile,
            mProject,
            directoryProvider
        )
        mTaskFragment!!.executeRunnable(
            task,
            getString(R.string.compiling_chapter),
            getString(R.string.please_wait),
            false
        )
    }

    override fun onNegativeClick(dialog: CheckingDialog) {
        dialog.dismiss()
    }

    override fun onNegativeClick(dialog: CompileDialog) {
        dialog.dismiss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun prepareChapterCardData() {
        val chapters = mChunks!!.chapters
        val chapterLabel = if (mChunks!!.chapterLabel == "chapter") {
            getString(R.string.chapter_title)
        } else ""
        for (chapter in chapters) {
            val unitCount = chapter.chunks.size
            val chapterNumber = chapter.number
            mChapterCardList.add(
                ChapterCard(
                    chapterLabel + " " + mChunks!!.getChapterName(chapterNumber),
                    chapterNumber,
                    mProject,
                    unitCount,
                    db,
                    directoryProvider
                )
            )
        }
    }

    override fun onTaskComplete(taskTag: Int, resultCode: Int) {
        if (resultCode == TaskFragment.STATUS_OK) {
            if (taskTag == DATABASE_RESYNC_TASK) {
                mDbResyncing = false
                refreshChapterCards()
            } else if (taskTag == COMPILE_CHAPTER_TASK) {
                for (i in mChaptersCompiled!!) {
                    val chapter = mChapterCardList[i].chapterNumber
                    db.setCheckingLevel(mProject, chapter, 0)
                    mChapterCardList[i].compile()
                    mAdapter!!.notifyItemChanged(i)
                }
            }
        }
    }

    companion object {
        const val STATE_COMPILING: String = "compiling"
        private const val STATE_PROGRESS = "progress"
        var PROJECT_KEY: String = "project_key"
        private val DATABASE_RESYNC_TASK = Task.FIRST_TASK
        private val COMPILE_CHAPTER_TASK = Task.FIRST_TASK + 1
        private val STATE_RESYNC = "db_resync"
        private val TAG_TASK_FRAGMENT = "task_fragment"
        fun getActivityUnitListIntent(ctx: Context?, p: Project?): Intent {
            val intent = Intent(ctx, ActivityUnitList::class.java)
            intent.putExtra(Project.PROJECT_EXTRA, p)
            return intent
        }
    }
}
