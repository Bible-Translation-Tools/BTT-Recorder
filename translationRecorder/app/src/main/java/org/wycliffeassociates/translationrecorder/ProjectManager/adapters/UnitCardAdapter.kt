package org.wycliffeassociates.translationrecorder.ProjectManager.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.UnitCardBinding
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import org.wycliffeassociates.translationrecorder.widgets.OnCardExpandedListener
import org.wycliffeassociates.translationrecorder.widgets.UnitCard
import org.wycliffeassociates.translationrecorder.widgets.UnitCard.DatabaseAccessor

/**
 * Created by leongv on 7/28/2016.
 */
class UnitCardAdapter (
    private val project: Project,
    private val chapterNum: Int,
    private val unitCardList: List<UnitCard>,
    private val db: IProjectDatabaseHelper,
    private val prefs: IPreferenceRepository
) : RecyclerView.Adapter<UnitCardAdapter.ViewHolder>(),
    DatabaseAccessor, OnCardExpandedListener {

    private var recyclerView: RecyclerView? = null
    private val expandedCards: MutableList<Int> = ArrayList()
    private val mSelectedCards: MutableList<ViewHolder> = ArrayList()
    private val multiSelector = MultiSelector()

    private lateinit var context: Context

    override fun updateSelectedTake(takeInfo: TakeInfo) {
        db.setSelectedTake(takeInfo)
    }

    override fun selectedTakeNumber(takeInfo: TakeInfo): Int {
        val selectedTakeNumber = db.getSelectedTakeNumber(takeInfo)
        return selectedTakeNumber
    }

    override fun takeRating(takeInfo: TakeInfo): Int {
        val takeRating = db.getTakeRating(takeInfo)
        return takeRating
    }

    override fun takeCount(project: Project, chapter: Int, firstVerse: Int): Int {
        var takeCount = 0
        if (db.chapterExists(this.project, chapter) && db.unitExists(this.project, chapter, firstVerse)) {
            val unitId = db.getUnitId(project, chapter, firstVerse)
            takeCount = db.getTakeCount(unitId)
        }
        return takeCount
    }

    override fun deleteTake(takeInfo: TakeInfo) {
        db.deleteTake(takeInfo)
    }

    override fun removeSelectedTake(takeInfo: TakeInfo) {
        db.removeSelectedTake(takeInfo)
    }

    override fun selectTake(takeInfo: TakeInfo) {
        db.setSelectedTake(takeInfo)
    }

    override fun onCardExpanded(position: Int) {
        recyclerView?.layoutManager?.scrollToPosition(position)
    }

    inner class ViewHolder(
        val binding: UnitCardBinding
    ) : SwappingHolder(
        binding.root,
        multiSelector
    ), View.OnClickListener, OnLongClickListener {
        var unitCard: UnitCard? = null

        init {
            binding.root.setOnClickListener(this)
            binding.root.setOnLongClickListener(this)
            binding.root.isLongClickable = true

            selectionModeStateListAnimator = null
            defaultModeStateListAnimator = null
            selectionModeBackgroundDrawable = null
            defaultModeBackgroundDrawable = null
        }

        // Called on onBindViewHolder, when the view is visible on the screen
        @SuppressLint("SetTextI18n")
        fun bindViewHolder(holder: ViewHolder, position: Int, uc: UnitCard) {
            // Capture the UnitCard object
            unitCard = uc
            uc.setViewHolder(holder)
            // Expand card if it's already expanded before
            if (uc.isExpanded) {
                uc.expand()
            } else {
                uc.collapse()
            }
            // Set card views based on the UnitCard object
            binding.unitTitle.text = uc.title
            binding.takeCount.text = uc.takeCount.toString()
            // Raise card, and show appropriate visual cue, if it's already selected
            if (multiSelector.isSelected(position, 0)) {
                mSelectedCards.add(this)
                uc.raise(context)
            } else {
                mSelectedCards.remove(this)
                uc.drop(context)
            }
            // Hide expand icon if it's empty
            if (uc.isEmpty) {
                binding.unitExpandBtn.visibility = View.INVISIBLE
            } else {
                binding.unitExpandBtn.visibility = View.VISIBLE
            }
            setListeners(uc, this)
        }

        override fun onClick(view: View) {
        }

        override fun onLongClick(view: View): Boolean {
            return true
        }

        fun pausePlayers() {
            for (uc in unitCardList) {
                if (uc.isExpanded) {
                    uc.pauseAudio()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val binding = UnitCardBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unitCard = unitCardList[position]
        holder.bindViewHolder(holder, position, unitCard)
    }

    override fun getItemCount(): Int {
        return unitCardList.size
    }

    val selectedCards: List<UnitCard>
        get() {
            val cards: MutableList<UnitCard> = ArrayList()
            for (i in itemCount downTo 0) {
                if (multiSelector.isSelected(i, 0)) {
                    cards.add(unitCardList[i])
                }
            }
            return cards
        }

    private fun setListeners(unitCard: UnitCard, holder: ViewHolder) {
        val position = holder.bindingAdapterPosition
        holder.binding.unitRecordBtn.setOnClickListener(
            unitCard.getUnitRecordOnClick(context, db, prefs)
        )
        holder.binding.unitExpandBtn.setOnClickListener(
            unitCard.getUnitExpandOnClick(
                position,
                expandedCards,
                this
            )
        )
        holder.binding.deleteTakeBtn.setOnClickListener(
            unitCard.getTakeDeleteOnClick(
                context,
                this,
                position,
                this
            )
        )
        holder.binding.playTakeBtn.setOnClickListener(unitCard.takePlayPauseOnClick)
        holder.binding.editTakeBtn.setOnClickListener(unitCard.takeEditOnClickListener)
        holder.binding.rateTakeBtn.setOnClickListener(unitCard.getTakeRatingOnClick())
        holder.binding.selectTakeBtn.setOnClickListener(unitCard.getTakeSelectOnClick(this))
        holder.binding.nextTakeBtn.setOnClickListener(unitCard.takeIncrementOnClick)
        holder.binding.prevTakeBtn.setOnClickListener(unitCard.takeDecrementOnClick)
    }

    fun exitCleanUp() {
        for (uc in unitCardList) {
            if (uc.isExpanded) {
                uc.destroyAudioPlayer()
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    fun getItem(id: Int): UnitCard {
        return unitCardList[id]
    }
}
