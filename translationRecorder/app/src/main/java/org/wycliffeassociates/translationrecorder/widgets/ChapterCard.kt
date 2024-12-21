package org.wycliffeassociates.translationrecorder.widgets

import android.app.AlertDialog
import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import org.wycliffeassociates.translationrecorder.ProjectManager.adapters.ChapterCardAdapter
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CheckingDialog
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.CompileDialog
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.chapterIntToString
import org.wycliffeassociates.translationrecorder.project.ProjectFileUtils.getProjectDirectory
import java.io.File
import java.lang.ref.SoftReference

/**
 * Created by leongv on 8/15/2016.
 */
class ChapterCard(
    val title: String,
    val chapterNumber: Int,
    private val mProject: Project,
    private val mUnitCount: Int,
    private val db: IProjectDatabaseHelper,
    private val directoryProvider: IDirectoryProvider
) {
    interface OnClickListener : View.OnClickListener {
        fun onClick(
            v: View,
            vh: ChapterCardAdapter.ViewHolder,
            expandedCards: List<Int>,
            position: Int
        )
    }

    interface ChapterDB {
        fun checkingLevel(project: Project, chapter: Int): Int
    }

    interface ChapterProgress {
        fun updateChapterProgress(chapter: Int)
        fun chapterProgress(chapter: Int): Int
    }

    lateinit var viewHolder: ChapterCardAdapter.ViewHolder
    private var mChapterWav: File? = null
    private var mUnitStarted = 0

    // State
    var isEmpty: Boolean = true
        private set
    private var mCanCompile = false
    var isCompiled: Boolean = false
        private set
    var isExpanded: Boolean = false
        private set
    private var mIconsClickable = true

    val playPauseOnClick: View.OnClickListener
        get() = View.OnClickListener {
            if (viewHolder.binding.playPauseChapterBtn.isActivated) {
                pauseAudio()
            } else {
                playAudio()
            }
        }

    private var _checkingLevel = 0
    var checkingLevel: Int
        get() = _checkingLevel
        set(level) {
            _checkingLevel = if (level < MIN_CHECKING_LEVEL) {
                MIN_CHECKING_LEVEL
            } else if (level > MAX_CHECKING_LEVEL) {
                MAX_CHECKING_LEVEL
            } else {
                level
            }
        }

    private var _progress = 0
    var progress: Int
        get() = _progress
        set(progress) {
            _progress = if (progress < MIN_PROGRESS) {
                MIN_PROGRESS
            } else if (progress > MAX_PROGRESS) {
                MAX_PROGRESS
            } else {
                progress
            }
        }

    private var _audioPlayer: SoftReference<AudioPlayer>? = null
    private val audioPlayer: AudioPlayer
        // Private Methods
        get() {
            var ap: AudioPlayer? = _audioPlayer?.get()
            if (ap == null) {
                ap = initializeAudioPlayer()
            }
            return ap
        }

    fun refreshIsEmpty() {
        isEmpty = _progress == 0
    }

    fun refreshChapterCompiled(chapter: Int) {
        if (!mCanCompile) {
            return
        }
        val dir = getProjectDirectory(mProject, directoryProvider)
        val chapterString = chapterIntToString(mProject, chapter)
        val chapterDir = File(dir, chapterString)
        if (chapterDir.exists()) {
            mChapterWav = File(chapterDir, mProject.getChapterFileName(chapter))
            if (mChapterWav!!.exists()) {
                isCompiled = true
                return
            }
        }
        isCompiled = false
    }

    fun refreshCheckingLevel(chapterDb: ChapterDB, project: Project, chapter: Int) {
        if (isCompiled) {
            _checkingLevel = chapterDb.checkingLevel(project, chapter)
        }
    }

    fun refreshProgress() {
        val progress = calculateProgress()
        if (progress != _progress) {
            this.progress = progress
            saveProgressToDB(progress)
        }
    }

    fun setIconsEnabled(enabled: Boolean) {
        viewHolder.binding.checkLevelBtn.isEnabled = enabled
        viewHolder.binding.compileBtn.isEnabled = enabled
        viewHolder.binding.recordBtn.isEnabled = enabled
        viewHolder.binding.expandBtn.isEnabled = enabled
    }

    fun setIconsClickable(clickable: Boolean) {
        mIconsClickable = clickable
    }

    fun setNumOfUnitStarted(count: Int) {
        mUnitStarted = count
    }

    fun canCompile(): Boolean {
        return mCanCompile
    }

    fun areIconsClickable(): Boolean {
        return mIconsClickable
    }

    private fun initializeAudioPlayer(): AudioPlayer {
        val ap = AudioPlayer()
        ap.refreshView(
            viewHolder.binding.timeElapsed,
            viewHolder.binding.timeDuration,
            viewHolder.binding.playPauseChapterBtn,
            viewHolder.binding.seekBar
        )
        _audioPlayer = SoftReference(ap)
        return ap
    }

    private fun refreshAudioPlayer() {
        val ap = audioPlayer
        if (!ap.isLoaded) {
            ap.reset()
            ap.loadFile(mChapterWav)
        }
        ap.refreshView(
            viewHolder.binding.timeElapsed,
            viewHolder.binding.timeDuration,
            viewHolder.binding.playPauseChapterBtn,
            viewHolder.binding.seekBar
        )
    }

    private fun calculateProgress(): Int {
        return Math.round((mUnitStarted.toFloat() / mUnitCount) * 100)
    }

    private fun saveProgressToDB(progress: Int) {
        if (db.chapterExists(mProject, chapterNumber)) {
            val chapterId = db.getChapterId(mProject, chapterNumber)
            db.setChapterProgress(chapterId, progress)
        }
    }

    // Public API
    fun expand() {
        refreshAudioPlayer()
        viewHolder.binding.cardBody.visibility = View.VISIBLE
        viewHolder.binding.expandBtn.isActivated = true
        isExpanded = true
    }

    fun collapse() {
        viewHolder.binding.cardBody.visibility = View.GONE
        viewHolder.binding.expandBtn.isActivated = false
        isExpanded = false
    }

    fun raise(backgroundColor: Int, textColor: Int) {
        viewHolder.binding.chapterCard.cardElevation = 8f
        viewHolder.binding.chapterCardContainer.setBackgroundColor( //mCtx.getResources().getColor(R.color.accent)
            backgroundColor
        )
        viewHolder.binding.title.setTextColor(textColor)
        // Compile button activated status gets reset by multiSelector.
        // This is a way to correct it.
        viewHolder.binding.compileBtn.isActivated = canCompile()
        setIconsEnabled(false)
    }

    fun drop(backgroundColor: Int, textColor: Int, emptyTextColor: Int) {
        viewHolder.binding.chapterCard.cardElevation = 2f
        viewHolder.binding.chapterCardContainer.setBackgroundColor(backgroundColor)
        viewHolder.binding.title.setTextColor(
            if ((isEmpty))
                emptyTextColor
            else
                textColor
        )
        // Compile button activated status gets reset by multiSelector.
        // This is a way to correct it.
        viewHolder.binding.compileBtn.isActivated = canCompile()
        setIconsEnabled(true)
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

    fun refreshCanCompile() {
        mCanCompile = _progress == 100
    }

    fun compile() {
        isCompiled = true
        checkingLevel = 0
    }

    fun getCheckLevelOnClick(fm: FragmentManager): View.OnClickListener {
        return View.OnClickListener {
            if (!areIconsClickable()) {
                return@OnClickListener
            }
            pauseAudio()
            val dialog = CheckingDialog.newInstance(
                mProject,
                viewHolder.adapterPosition,
                _checkingLevel
            )
            dialog.show(fm, "single_chapter_checking_level")
        }
    }

    fun getCompileOnClick(fm: FragmentManager): View.OnClickListener {
        return View.OnClickListener {
            if (canCompile()) {
                if (!areIconsClickable()) {
                    return@OnClickListener
                }
                pauseAudio()
                //pass in chapter index, not chapter number
                val dialog = CompileDialog.newInstance(
                    mProject,
                    viewHolder.adapterPosition,
                    isCompiled
                )
                dialog.show(fm, "single_compile_chapter")
            }
        }
    }

    fun getRecordOnClick(context: Context): View.OnClickListener {
        return View.OnClickListener {
            if (!areIconsClickable()) {
                return@OnClickListener
            }
            pauseAudio()
            destroyAudioPlayer()
            val chapter: Int = chapterNumber
            val intent = RecordingActivity.getNewRecordingIntent(
                context,
                mProject,
                chapter,
                ChunkPlugin.DEFAULT_UNIT
            )
            context.startActivity(intent)
        }
    }

    fun getExpandOnClick(listener: OnCardExpandedListener, position: Int): View.OnClickListener {
        return View.OnClickListener {
            if (!areIconsClickable()) {
                return@OnClickListener
            }
            if (this.isExpanded) {
                pauseAudio()
                collapse()
            } else {
                expand()
                listener.onCardExpanded(position)
            }
        }
    }

    fun getDeleteOnClick(adapter: ChapterCardAdapter, context: Context?): View.OnClickListener {
        return View.OnClickListener {
            pauseAudio()
            val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.delete_chapter_recording)
                .setIcon(R.drawable.ic_delete_black_36dp)
                .setPositiveButton(R.string.yes) { _, _ ->
                    destroyAudioPlayer()
                    mChapterWav!!.delete()
                    isCompiled = false
                    collapse()
                    db.setCheckingLevel(mProject, chapterNumber, 0)
                    adapter.notifyItemChanged(viewHolder.adapterPosition)
                }
                .setNegativeButton(
                    R.string.no
                ) { dialog, _ -> dialog.dismiss() }
                .create()
            dialog.show()
        }
    }

    companion object {
        const val MIN_CHECKING_LEVEL: Int = 0
        const val MAX_CHECKING_LEVEL: Int = 3
        const val MIN_PROGRESS: Int = 0
        const val MAX_PROGRESS: Int = 100
    }
}
