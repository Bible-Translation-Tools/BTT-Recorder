package org.wycliffeassociates.translationrecorder.ProjectManager.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.UnitCardAdapter
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CheckingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.RatingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync.UnitResyncTask
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ActivityUnitListBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import org.wycliffeassociates.translationrecorder.project.ProjectProgress
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import org.wycliffeassociates.translationrecorder.utilities.Task
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment
import org.wycliffeassociates.translationrecorder.utilities.TaskFragment.OnTaskComplete
import org.wycliffeassociates.translationrecorder.widgets.UnitCard
import org.wycliffeassociates.translationrecorder.widgets.UnitCard.OnTakeActionListener
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sarabiaj on 6/30/2016.
 */
@AndroidEntryPoint
class ActivityUnitList : AppCompatActivity(), CheckingDialog.DialogListener,
    RatingDialog.DialogListener, OnTaskComplete, OnTakeActionListener {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetsProvider: AssetsProvider

    private lateinit var project: Project

    private var chapterNum = 0
    private var unitCardList: MutableList<UnitCard> = arrayListOf()
    private var adapter: UnitCardAdapter? = null
    private var dbResyncing = false
    private var taskFragment: TaskFragment? = null
    private var projectProgress: ProjectProgress? = null

    private lateinit var chunkPlugin: ChunkPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityUnitListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        project = intent.getParcelableExtra(PROJECT_KEY)!!

        try {
            chunkPlugin = project.getChunkPlugin(ChunkPluginLoader(
                directoryProvider,
                assetsProvider
            ))
            chapterNum = intent.getIntExtra(CHAPTER_KEY, 1)
            projectProgress = ProjectProgress(project, db, chunkPlugin.chapters)

            val fm = supportFragmentManager
            taskFragment = fm.findFragmentByTag(TAG_TASK_FRAGMENT) as? TaskFragment
            if (taskFragment == null) {
                taskFragment = TaskFragment()
                fm.beginTransaction().add(taskFragment!!, TAG_TASK_FRAGMENT).commit()
                fm.executePendingTransactions()
            }

            if (savedInstanceState != null) {
                dbResyncing = savedInstanceState.getBoolean(STATE_RESYNC)
            }

            // Setup toolbar
            val language = db.getLanguageName(project.targetLanguageSlug)
            val book = project.bookName
            setSupportActionBar(binding.unitListToolbar)

            val chapterLabel = if (chunkPlugin.chapterLabel == "chapter") getString(R.string.chapter_title) else ""
            val chapterName = chunkPlugin.getChapterName(chapterNum)
            supportActionBar?.title = "$language - $book - $chapterLabel $chapterName"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // Find the recycler view
            binding.unitList.setHasFixedSize(false)

            // Set its layout manager
            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            binding.unitList.layoutManager = layoutManager

            // Set its adapter
            unitCardList = ArrayList()
            adapter = UnitCardAdapter(project, chapterNum, unitCardList, db, prefs)
            binding.unitList.adapter = adapter

            // Set its animator
            binding.unitList.itemAnimator = DefaultItemAnimator()
            prepareUnitCardData()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!dbResyncing) {
            dbResyncing = true
            val task = UnitResyncTask(
                DATABASE_RESYNC_TASK,
                supportFragmentManager,
                project,
                chapterNum,
                db,
                directoryProvider
            )
            taskFragment?.executeRunnable(
                task,
                "Resyncing Database",
                "Please wait...",
                true
            )
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_RESYNC, dbResyncing)
        super.onSaveInstanceState(outState)
    }

    private fun refreshUnitCards() {
        for (i in unitCardList.indices) {
            unitCardList[i].refreshUnitStarted(
                project,
                chapterNum,
                unitCardList[i].startVerse
            )
        }
        adapter?.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        adapter?.exitCleanUp()
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

    override fun onPositiveClick(dialog: CheckingDialog) {
        // NOTE: Deprecated
    }

    override fun onPositiveClick(dialog: RatingDialog) {
        db.setTakeRating(dialog.takeInfo, dialog.rating)
        adapter?.notifyDataSetChanged()
    }

    override fun onNegativeClick(dialog: CheckingDialog) {
        dialog.dismiss()
    }

    override fun onNegativeClick(dialog: RatingDialog) {
        dialog.dismiss()
    }

    private fun prepareUnitCardData() {
        if (chunkPlugin != null) {
            val chunks = chunkPlugin.getChapter(chapterNum).chunks
            for (unit in chunks) {
                val modeLabel = project.getLocalizedModeName(this)
                val title = modeLabel + " " + unit.label
                unitCardList.add(
                    UnitCard(
                        adapter!!,
                        project,
                        title,
                        chapterNum,
                        unit.startVerse,
                        unit.endVerse,
                        this,
                        directoryProvider
                    )
                )
            }
        }
    }

    override fun onTakeDeleted() {
        if (projectProgress != null) {
            projectProgress!!.updateProjectProgress()
        }
    }

    override fun onRated(name: String, currentTakeRating: Int) {
        val ppm: ProjectPatternMatcher = project.patternMatcher
        ppm.match(name)
        val takeInfo: TakeInfo = ppm.takeInfo
        val dialog = RatingDialog.newInstance(takeInfo, currentTakeRating)
        dialog.show(supportFragmentManager, "single_take_rating")
    }

    override fun onTaskComplete(taskTag: Int, resultCode: Int) {
        if (resultCode == TaskFragment.STATUS_OK) {
            if (taskTag == DATABASE_RESYNC_TASK) {
                dbResyncing = false
                refreshUnitCards()
            }
        }
    }

    companion object {
        var PROJECT_KEY: String = "project_key"
        var CHAPTER_KEY: String = "chapter_key"
        private val DATABASE_RESYNC_TASK = Task.FIRST_TASK
        private const val TAG_TASK_FRAGMENT = "task_fragment"
        private const val STATE_RESYNC = "db_resync"

        @JvmStatic
        fun getActivityUnitListIntent(ctx: Context?, p: Project?, chapter: Int): Intent {
            val intent = Intent(ctx, ActivityUnitList::class.java)
            intent.putExtra(PROJECT_KEY, p)
            intent.putExtra(CHAPTER_KEY, chapter)
            return intent
        }
    }
}
