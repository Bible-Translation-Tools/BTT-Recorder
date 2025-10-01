package org.wycliffeassociates.translationrecorder.ProjectManager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityUnitList.Companion.getActivityUnitListIntent
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CheckingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CheckingDialog.Companion.newInstance
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CompileDialog
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ChapterCardBinding
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard
import org.wycliffeassociates.translationrecorder.widgets.ChapterCard.ChapterDB
import org.wycliffeassociates.translationrecorder.widgets.OnCardExpandedListener

/**
 * Created by leongv on 8/15/2016.
 */
class ChapterCardAdapter(
    private val mCtx: AppCompatActivity,
    private val mProject: Project,
    private val mChapterCardList: List<ChapterCard>,
    private val db: IProjectDatabaseHelper
) : RecyclerView.Adapter<ChapterCardAdapter.ViewHolder>(),
    ChapterDB, OnCardExpandedListener {
    private var recyclerView: RecyclerView? = null
    private val mExpandedCards: MutableList<Int> = ArrayList()
    private val mSelectedCards: MutableList<Int> = ArrayList()
    private val mMultiSelector = MultiSelector()
    var actionMode: ActionMode? = null
        private set

    private fun initializeColors(mCtx: Context) {
        RAISED_CARD_BACKGROUND_COLOR = mCtx.resources.getColor(R.color.accent)
        RAISED_CARD_TEXT_COLOR = mCtx.resources.getColor(R.color.text_light)

        DROPPED_CARD_BACKGROUND_COLOR = mCtx.resources.getColor(R.color.card_bg)
        DROPPED_CARD_EMPTY_TEXT_COLOR =
            mCtx.resources.getColor(R.color.primary_text_disabled_material_light)
        DROPPED_CARD_TEXT_COLOR =
            mCtx.resources.getColor(R.color.primary_text_default_material_light)
    }

    private val mMultiSelectMode: ActionMode.Callback = object : ModalMultiSelectorCallback(mMultiSelector) {
            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                mMultiSelector.clearSelections()
                mMultiSelector.isSelectable = true
                return false
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                actionMode = mode
                mCtx.menuInflater.inflate(R.menu.chapter_menu, menu)
                setIconsClickable(false)
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val chapters = IntArray(mSelectedCards.size)
                for (i in mSelectedCards.indices) {
                    chapters[i] = mSelectedCards[i]
                }
                when (item.itemId) {
                    R.id.chapters_checking_level -> {
                        val dialog = newInstance(
                            mProject, chapters,
                            commonCheckingLevel
                        )
                        dialog.show(mCtx.supportFragmentManager, "multi_chapter_checking_level")
                    }

                    R.id.chapters_compile -> {
                        val isCompiled = BooleanArray(mSelectedCards.size)
                        var i = 0
                        while (i < mSelectedCards.size) {
                            isCompiled[i] = mChapterCardList[mSelectedCards[i]].isCompiled
                            i++
                        }
                        val compileDialog =
                            CompileDialog.newInstance(mProject, chapters, isCompiled)
                        compileDialog.show(mCtx.supportFragmentManager, "multi_chapter_compile")
                    }

                    else -> println("Default action")
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                mMultiSelector.isSelectable = false
                mMultiSelector.clearSelections()
                for (i in mSelectedCards) {
                    notifyItemChanged(i)
                }
                mSelectedCards.clear()
                actionMode = null
                setIconsClickable(true)
            }
        }

    // Constructor
    init {
        initializeColors(mCtx)
    }

    override fun checkingLevel(project: Project, chapter: Int): Int {
        val checkingLevel = db.getChapterCheckingLevel(project, chapter)
        return checkingLevel
    }

    override fun onCardExpanded(position: Int) {
        recyclerView?.layoutManager?.scrollToPosition(position)
    }

    inner class ViewHolder(
        val binding: ChapterCardBinding
    ) : SwappingHolder(binding.root, mMultiSelector), View.OnClickListener, OnLongClickListener {
        var chapterCard: ChapterCard? = null

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
            binding.root.isLongClickable = true

            selectionModeStateListAnimator = null
            defaultModeStateListAnimator = null
            selectionModeBackgroundDrawable = null
            defaultModeBackgroundDrawable = null
        }

        fun bindViewHolder(holder: ViewHolder, pos: Int, cc: ChapterCard?) {
            chapterCard = cc
            chapterCard?.let { card ->
                card.viewHolder = holder

                // Title
                binding.title.text = card.title

                // Progress Pie
                binding.progressPie.progress = card.progress

                // Checking Level
                card.refreshCheckingLevel(
                    this@ChapterCardAdapter,
                    mProject,
                    card.chapterNumber
                )
                binding.checkLevelBtn.step = card.checkingLevel

                // Compile
                binding.compileBtn.isActivated = card.canCompile()

                // Checking Level and Expansion
                if (card.isCompiled) {
                    binding.checkLevelBtn.visibility = View.VISIBLE
                    binding.expandBtn.visibility = View.VISIBLE
                } else {
                    binding.checkLevelBtn.visibility = View.INVISIBLE
                    binding.expandBtn.visibility = View.INVISIBLE
                }

                // Expand card if it's already expanded before
                if (card.isExpanded) {
                    card.expand()
                } else {
                    card.collapse()
                }

                // Raise card, and show appropriate visual cue, if it's already selected
                if (mMultiSelector.isSelected(pos, 0)) {
                    card.raise(RAISED_CARD_BACKGROUND_COLOR, RAISED_CARD_TEXT_COLOR)
                    if (!mSelectedCards.contains(adapterPosition)) {
                        mSelectedCards.add(adapterPosition)
                    }
                } else {
                    mSelectedCards.remove(adapterPosition)
                    card.drop(
                        DROPPED_CARD_BACKGROUND_COLOR,
                        DROPPED_CARD_TEXT_COLOR,
                        DROPPED_CARD_EMPTY_TEXT_COLOR
                    )
                }

                // Clickable
                card.setIconsClickable(!isInActionMode)

                setListeners(this, card)
            }
        }

        override fun onClick(view: View) {
            // Completing a chapter (hence can be compiled) is the minimum requirements to
            //    include a chapter in multi-selection
            chapterCard?.let { card ->
                if (mMultiSelector.isSelectable) {
                    if (!card.canCompile()) {
                        return
                    }

                    // Close card if it is expanded in multi-select mode
                    if (card.isExpanded) {
                        toggleExpansion(this, mExpandedCards, this.adapterPosition)
                    }

                    mMultiSelector.tapSelection(this)

                    // Raise/drop card
                    if (mMultiSelector.isSelected(this.adapterPosition, 0)) {
                        mSelectedCards.add(adapterPosition)
                        card.raise(RAISED_CARD_BACKGROUND_COLOR, RAISED_CARD_TEXT_COLOR)
                    } else {
                        mSelectedCards.remove(adapterPosition)
                        card.drop(
                            DROPPED_CARD_BACKGROUND_COLOR,
                            DROPPED_CARD_TEXT_COLOR,
                            DROPPED_CARD_EMPTY_TEXT_COLOR
                        )
                    }

                    setAvailableActions()

                    // Finish action mode if all cards are de-selected
                    if (actionMode != null && mSelectedCards.size <= 0) {
                        actionMode!!.finish()
                    }
                } else {
                    card.pauseAudio()
                    card.destroyAudioPlayer()
                    val intent = getActivityUnitListIntent(mCtx, mProject, card.chapterNumber)
                    mCtx.startActivity(intent)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            // Completing a chapter (hence can be compiled) is the minimum requirements to
            //    include a chapter in multi-selection

            chapterCard?.let { card ->
                mCtx.startSupportActionMode(mMultiSelectMode)
                mMultiSelector.setSelected(this, true)

                // Close card if it is expanded on entering multi-select mode
                if (card.isExpanded) {
                    toggleExpansion(this, mExpandedCards, this.adapterPosition)
                }

                card.raise(RAISED_CARD_BACKGROUND_COLOR, RAISED_CARD_TEXT_COLOR)

                setAvailableActions()
            }

            return true
        }
    }


    // Overrides
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ChapterCardBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapterCard = mChapterCardList[position]
        holder.bindViewHolder(holder, position, chapterCard)
    }

    override fun getItemCount(): Int {
        return mChapterCardList.size
    }

    val selectedCards: List<ChapterCard>
        // Getters
        get() {
            val cards: MutableList<ChapterCard> = arrayListOf()
            for (i in itemCount downTo 0) {
                if (mMultiSelector.isSelected(i, 0)) {
                    cards.add(mChapterCardList[i])
                }
            }
            return cards
        }

    val isInActionMode: Boolean
        get() = actionMode != null

    // Private Methods
    private fun setListeners(holder: ViewHolder, chapterCard: ChapterCard) {
        holder.binding.checkLevelBtn.setOnClickListener(chapterCard.getCheckLevelOnClick(mCtx.supportFragmentManager))
        holder.binding.compileBtn.setOnClickListener(chapterCard.getCompileOnClick(mCtx.supportFragmentManager))
        holder.binding.recordBtn.setOnClickListener(chapterCard.getRecordOnClick(mCtx))
        holder.binding.expandBtn.setOnClickListener(
            chapterCard.getExpandOnClick(
                this,
                holder.adapterPosition
            )
        )
        holder.binding.deleteChapterAudioBtn.setOnClickListener(chapterCard.getDeleteOnClick(this, mCtx))
        holder.binding.playPauseChapterBtn.setOnClickListener(chapterCard.playPauseOnClick)
    }

    private fun setAvailableActions() {
        actionMode?.let { mode ->
            var checkEnabled = true

            for (chapterCard in selectedCards) {
                if (!chapterCard.isCompiled) {
                    checkEnabled = false
                    break
                }
            }

            mode.menu.findItem(R.id.chapters_checking_level).setEnabled(checkEnabled)
        }
    }

    private val commonCheckingLevel: Int
        get() {
            val selectedCards = selectedCards
            val length = selectedCards.size
            var checkingLevel = if (length >= 1) {
                selectedCards[0].checkingLevel
            } else CheckingDialog.NO_LEVEL_SELECTED

            // If there are more items, check if their checking level is similar. If not, set the
            // checking level to an empty value
            for (i in 1 until length) {
                if (selectedCards[i].checkingLevel != checkingLevel) {
                    checkingLevel = CheckingDialog.NO_LEVEL_SELECTED
                    break
                }
            }

            return checkingLevel
        }


    // Public API
    fun toggleExpansion(vh: ViewHolder, expandedCards: MutableList<Int>, position: Int) {
        vh.chapterCard?.let { card ->
            if (!card.isExpanded) {
                card.expand()
                if (!expandedCards.contains(position)) {
                    expandedCards.add(position)
                }
            } else {
                card.collapse()
                if (expandedCards.contains(position)) {
                    expandedCards.removeAt(expandedCards.indexOf(position))
                }
            }
        }
    }

    fun setIconsClickable(clickable: Boolean) {
        for (i in mChapterCardList.indices) {
            mChapterCardList[i].setIconsClickable(clickable)
            notifyItemChanged(i)
        }
    }

    fun exitCleanUp() {
        for (cc in mChapterCardList) {
            if (cc.isExpanded) {
                cc.destroyAudioPlayer()
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun getItem(index: Int): ChapterCard {
        return mChapterCardList[index]
    }

    companion object {
        private var RAISED_CARD_BACKGROUND_COLOR = 0
        private var DROPPED_CARD_BACKGROUND_COLOR = 0
        private var RAISED_CARD_TEXT_COLOR = 0
        private var DROPPED_CARD_TEXT_COLOR = 0
        private var DROPPED_CARD_EMPTY_TEXT_COLOR = 0
    }
}
