package org.wycliffeassociates.translationrecorder.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.Playback.PlaybackActivity
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.UnitCardAdapter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.TranslationRecorderApp
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import org.wycliffeassociates.translationrecorder.project.TakeInfo
import org.wycliffeassociates.translationrecorder.wav.WavFile
import java.io.File
import java.lang.ref.SoftReference
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.max

/**
 * Created by leongv on 7/28/2016.
 */
class UnitCard(
    private val databaseAccessor: DatabaseAccessor,
    private val project: Project,
    val title: String,
    private val chapter: Int,
    val startVerse: Int,
    private val endVerse: Int,
    private val onTakeActionListener: OnTakeActionListener,
    private val directoryProvider: IDirectoryProvider,
    private val context: Context
) {
    interface DatabaseAccessor {
        fun updateSelectedTake(takeInfo: TakeInfo)
        fun selectedTakeNumber(takeInfo: TakeInfo): Int
        fun takeCount(project: Project, chapter: Int, firstVerse: Int): Int
        fun deleteTake(takeInfo: TakeInfo)
        fun removeSelectedTake(takeInfo: TakeInfo)
        fun selectTake(takeInfo: TakeInfo)
        fun takeRating(takeInfo: TakeInfo): Int
    }

    interface OnTakeActionListener {
        fun onTakeDeleted()
        fun onRated(name: String, currentTakeRating: Int)
    }

    interface OnClickListener : View.OnClickListener {
        fun onClick(
            v: View,
            vh: UnitCardAdapter.ViewHolder,
            expandedCards: List<Int>,
            position: Int
        )
    }

    // Constants
    //public static boolean RATING_MODE = true;
    //public static boolean CHECKING_MODE = false;
    // State
    var isExpanded = false
        private set
    private var takeIndex = 0
    var isEmpty = true
        private set
    private var currentTakeRating = 0

    // Attributes
    private lateinit var viewHolder: UnitCardAdapter.ViewHolder
    var takeCount = 0
        private set

    // Constructors
    init {
        refreshTakeCount()
    }

    fun setViewHolder(vh: UnitCardAdapter.ViewHolder) {
        viewHolder = vh
    }

    // Private Methods
    private fun initializeAudioPlayer(): AudioPlayer {
        val ap = AudioPlayer(
            viewHolder.binding.timeElapsed,
            viewHolder.binding.timeDuration,
            viewHolder.binding.playTakeBtn,
            viewHolder.binding.seekBar
        )
        _audioPlayer = SoftReference(ap)
        return ap
    }

    private var _audioPlayer: SoftReference<AudioPlayer>? = null
    private val audioPlayer: AudioPlayer
        get() {
            var ap: AudioPlayer? = _audioPlayer?.get()
            if (ap == null) {
                ap = initializeAudioPlayer()
            }
            return ap
        }

    private fun refreshAudioPlayer() {
        val ap: AudioPlayer = audioPlayer
        if (!ap.isLoaded) {
            ap.reset()
            val takes: List<File> = takeList
            if (takeIndex < takes.size) {
                ap.loadFile(takeList[takeIndex])
            }
        }
        ap.refreshView(
            viewHolder.binding.timeElapsed,
            viewHolder.binding.timeDuration,
            viewHolder.binding.playTakeBtn,
            viewHolder.binding.seekBar
        )
    }

    private var _takeList: SoftReference<MutableList<File>>? = null
    private val takeList: MutableList<File>
        get() {
            var takes: MutableList<File>? = _takeList?.get()
            if (takes == null) {
                takes = populateTakeList()
            }
            return takes
        }

    private fun populateTakeList(): MutableList<File> {
        val root: File = ProjectFileUtils.getProjectDirectory(project, directoryProvider)
        val chap: String = ProjectFileUtils.chapterIntToString(project, chapter)
        val folder = File(root, chap)
        val files: Array<File>? = folder.listFiles()
        var ppm: ProjectPatternMatcher
        val first: Int = startVerse
        val end: Int = endVerse
        //Get only the files of the appropriate unit
        val resultFiles: MutableList<File> = ArrayList()
        if (files != null) {
            for (file: File in files) {
                ppm = project.patternMatcher
                ppm.match(file)
                if (ppm.matched()) {
                    val ti: TakeInfo? = ppm.takeInfo
                    if (ti != null && ti.startVerse == first && ti.endVerse == end) {
                        resultFiles.add(file)
                    }
                }
            }
        }
        resultFiles.sortWith { f, s ->
            val ppm: ProjectPatternMatcher = project.patternMatcher
            val ppm2: ProjectPatternMatcher = project.patternMatcher
            ppm.match(f)
            val takeInfo: TakeInfo = ppm.takeInfo!!
            ppm2.match(s)
            val takeInfo2: TakeInfo = ppm2.takeInfo!!

            //                Long first = f.lastModified();
            //                Long second = s.lastModified();
            //Change to take name rather than last modified because editing verse markers modifies the file
            //this means that adding verse markers would change the position in the list when returning to the card
            val first: Int = takeInfo.take
            val second: Int = takeInfo2.take
            first.compareTo(second)
        }
        _takeList = SoftReference(resultFiles)
        return resultFiles
    }

    private fun refreshTakes() {
        //if the soft reference still has the takes, cool, if not, repopulate them
        val takes: List<File> = takeList
        refreshTakeCountText(takes)
        refreshTakeText(takes)
        if (takes.isNotEmpty()) {
            val take: File = takes[takeIndex]
            refreshTakeRating(take)
            refreshSelectedTake(take)
        }
    }

    private fun refreshSelectedTake(take: File) {
        val ppm: ProjectPatternMatcher = project.patternMatcher
        ppm.match(take)
        val takeInfo: TakeInfo = ppm.takeInfo!!
        val chosen = databaseAccessor.selectedTakeNumber(takeInfo)
        viewHolder.binding.selectTakeBtn.isActivated = chosen == takeInfo.take
    }

    private fun refreshTakeRating(take: File) {
        val ppm: ProjectPatternMatcher = project.patternMatcher
        ppm.match(take)
        val takeInfo: TakeInfo = ppm.takeInfo!!
        Logger.w(this.toString(), "Refreshing take rating for " + take.name)
        currentTakeRating = databaseAccessor.takeRating(takeInfo)
        viewHolder.binding.rateTakeBtn.setStep(currentTakeRating)
        viewHolder.binding.rateTakeBtn.invalidate()
    }

    private fun refreshTakeText(takes: List<File>) {
        if (!this::viewHolder.isInitialized) {
            return
        }

        val text: String

        if (takes.isNotEmpty()) {
            text = context.resources.getString(
                R.string.label_take_detailed,
                (takeIndex + 1).toString(),
                takes.size.toString()
            )
            val created: Long = takes[takeIndex].lastModified()
            viewHolder.binding.currentTakeTimeStamp.text = convertTime(created)
        } else {
            text = context.resources.getString(
                R.string.label_take_detailed,
                "0",
                takes.size.toString()
            )
            viewHolder.binding.currentTakeTimeStamp.text = ""
        }
        viewHolder.binding.currentTakeView.text = text
        viewHolder.binding.currentTakeView.invalidate()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshTakeCountText(takes: List<File>) {
        if (!this::viewHolder.isInitialized) {
            return
        }

        viewHolder.unitCard?.refreshTakeCount()
        viewHolder.binding.takeCount.text = takes.size.toString()
    }

    @SuppressLint("SimpleDateFormat")
    private fun convertTime(time: Long): String {
        val date = Date(time)
        val format: Format = SimpleDateFormat("MMMM d, yyyy  HH:mm ")
        return format.format(date)
    }


    // Public API
    fun refreshUnitStarted(project: Project, chapter: Int, startVerse: Int) {
        val dir: File = ProjectFileUtils.getProjectDirectory(project, directoryProvider)
        val chapterString: String = ProjectFileUtils.chapterIntToString(project, chapter)
        val chapterDir = File(dir, chapterString)
        if (chapterDir.exists()) {
            val files: Array<File>? = chapterDir.listFiles()
            if (files != null) {
                for (f: File in files) {
                    val ppm: ProjectPatternMatcher = this.project.patternMatcher
                    ppm.match(f)
                    if (ppm.matched()) {
                        val takeInfo = ppm.takeInfo!!
                        if (takeInfo.startVerse == startVerse) {
                            isEmpty = false
                            return
                        }
                    }
                }
            }
        }
        isEmpty = true
    }

    fun refreshTakeCount() {
        takeCount = databaseAccessor.takeCount(project, chapter, startVerse)
    }

    fun expand() {
        refreshTakes()
        refreshAudioPlayer()
        isExpanded = true
        viewHolder.binding.takeCountContainer.visibility = View.GONE
        viewHolder.binding.cardBody.visibility = View.VISIBLE
        viewHolder.binding.cardFooter.visibility = View.VISIBLE
        viewHolder.binding.unitActions.isActivated = true
    }

    fun collapse() {
        isExpanded = false
        val visible = if (takeCount >= MIN_TAKE_THRESHOLD) View.VISIBLE else View.GONE
        viewHolder.binding.takeCountContainer.visibility = visible
        viewHolder.binding.cardBody.visibility = View.GONE
        viewHolder.binding.cardFooter.visibility = View.GONE
        viewHolder.binding.unitActions.isActivated = false
    }

    fun raise(context: Context) {
        if (!this::viewHolder.isInitialized) {
            return
        }
        viewHolder.binding.unitCard.cardElevation = 8f
        viewHolder.binding.unitCardContainer.setBackgroundColor(context.resources.getColor(R.color.accent))
        viewHolder.binding.unitTitle.setTextColor(context.resources.getColor(R.color.text_light))
        viewHolder.binding.unitActions.setEnabled(false)
    }

    fun drop(context: Context) {
        if (!this::viewHolder.isInitialized) {
            return
        }
        viewHolder.binding.unitCard.cardElevation = 2f
        val color = context.resources.getColor(R.color.card_bg)
        viewHolder.binding.unitCardContainer.setBackgroundColor(color)
        val resource = if (isEmpty) {
            R.color.primary_text_disabled_material_light
        } else R.color.primary_text_default_material_light
        viewHolder.binding.unitTitle.setTextColor(
            context.resources.getColor(resource)
        )
        viewHolder.binding.unitActions.setEnabled(true)
    }

    fun playAudio() {
        audioPlayer.play()
    }

    fun pauseAudio() {
        audioPlayer.pause()
    }

    fun destroyAudioPlayer() {
        _audioPlayer?.get()?.cleanup()
        _audioPlayer = null
    }

    fun getUnitRecordOnClick(
        context: Context,
        db: IProjectDatabaseHelper,
        prefs: IPreferenceRepository
    ): View.OnClickListener {
        return View.OnClickListener { view: View ->
            pauseAudio()
            project.loadProjectIntoPreferences(db, prefs)
            view.context.startActivity(
                RecordingActivity.getNewRecordingIntent(context, project, chapter, startVerse)
            )
        }
    }

    fun getUnitExpandOnClick(
        position: Int,
        expandedCards: MutableList<Int>,
        listener: OnCardExpandedListener
    ): View.OnClickListener {
        return View.OnClickListener {
            if (!isExpanded) {
                expand()
                if (!expandedCards.contains(position)) {
                    expandedCards.add(position)
                }
                listener.onCardExpanded(position)
            } else {
                pauseAudio()
                collapse()
                if (expandedCards.contains(position)) {
                    expandedCards.removeAt(expandedCards.indexOf(position))
                }
            }
        }
    }

    val takeIncrementOnClick: View.OnClickListener
        get() = View.OnClickListener {
            val takes: List<File> = this.takeList
            if (takes.isNotEmpty()) {
                takeIndex++
                if (takeIndex >= takes.size) {
                    takeIndex = 0
                }
                destroyAudioPlayer()
                refreshTakes()
                refreshAudioPlayer()
            }
        }

    val takeDecrementOnClick: View.OnClickListener
        get() {
            return View.OnClickListener {
                val takes: List<File> = this.takeList
                if (takes.isNotEmpty()) {
                    takeIndex--
                    if (takeIndex < 0) {
                        takeIndex = takes.size - 1
                    }
                    destroyAudioPlayer()
                    refreshTakes()
                    refreshAudioPlayer()
                }
            }
        }

    fun getTakeDeleteOnClick(
        ctx: Context,
        db: DatabaseAccessor,
        position: Int,
        adapter: UnitCardAdapter
    ): View.OnClickListener {
        return View.OnClickListener {
            pauseAudio()
            val takes: MutableList<File> = this.takeList
            if (takes.size > 0) {
                val dialog: AlertDialog = AlertDialog.Builder(ctx)
                    .setTitle(ctx.getString(R.string.delete_take))
                    .setIcon(R.drawable.ic_delete_black_36dp)
                    .setPositiveButton(ctx.getString(R.string.yes)) { _, _ ->
                        val selectedFile: File = takes[takeIndex]
                        val ppm: ProjectPatternMatcher = project.patternMatcher
                        ppm.match(selectedFile)
                        val takeInfo = ppm.takeInfo!!
                        db.deleteTake(takeInfo)
                        takes[takeIndex].delete()
                        takes.removeAt(takeIndex)
                        //keep the same index in the list, unless the one removed was the last take.
                        if (takeIndex > takes.size - 1) {
                            takeIndex--
                            //make sure the index is not negative
                            takeIndex = max(takeIndex.toDouble(), 0.0).toInt()
                        }
                        refreshTakes()
                        if (takes.size > 0) {
                            val ap: AudioPlayer = audioPlayer
                            ap.reset()
                            ap.loadFile(takes[takeIndex])
                        } else {
                            isEmpty = true
                            collapse()
                            destroyAudioPlayer()
                            adapter.notifyItemChanged(position)
                        }
                        onTakeActionListener.onTakeDeleted()
                    }
                    .setNegativeButton(ctx.getString(R.string.no)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
            }
        }
    }

    val takeEditOnClickListener: View.OnClickListener
        get() {
            return View.OnClickListener { v ->
                val takes: List<File> = this.takeList
                if (takes.isNotEmpty()) {
                    pauseAudio()
                    val wavFile = WavFile(takes[takeIndex])
                    val intent: Intent = PlaybackActivity.getPlaybackIntent(
                        v.context,
                        wavFile,
                        project,
                        chapter,
                        startVerse
                    )
                    v.context.startActivity(intent)
                }
            }
        }

    val takePlayPauseOnClick: View.OnClickListener
        get() {
            return View.OnClickListener {
                if (viewHolder.binding.playTakeBtn.isActivated) {
                    pauseAudio()
                } else {
                    viewHolder.pausePlayers()
                    playAudio()
                }
            }
        }

    fun getTakeRatingOnClick(): View.OnClickListener {
        return View.OnClickListener {
            val takes: List<File> = takeList
            if (takes.isNotEmpty()) {
                pauseAudio()
                val name: String = takes[takeIndex].name
                onTakeActionListener.onRated(name, currentTakeRating)
            }
        }
    }

    fun getTakeSelectOnClick(db: DatabaseAccessor): View.OnClickListener {
        return View.OnClickListener { view ->
            val takes: List<File> = takeList
            if (takes.isNotEmpty()) {
                val ppm: ProjectPatternMatcher = project.patternMatcher
                ppm.match(takes[takeIndex])
                val takeInfo = ppm.takeInfo!!
                if (view.isActivated) {
                    view.isActivated = false
                    db.removeSelectedTake(takeInfo)
                } else {
                    view.isActivated = true
                    db.selectTake(takeInfo)
                }
            }
        }
    }

    companion object {
        var NO_TAKES: Int = -1
        var MIN_TAKE_THRESHOLD: Int = 2
    }
}
