package org.wycliffeassociates.translationrecorder.Recording.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.door43.tools.reporting.Logger
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.UnitPicker.DIRECTION
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.FragmentRecordingFileBarBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.ChunkPluginLoader
import org.wycliffeassociates.translationrecorder.project.Project
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * Created by sarabiaj on 2/20/2017.
 */
@AndroidEntryPoint
class FragmentRecordingFileBar : Fragment() {
    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var assetProvider: AssetsProvider

    private lateinit var mProject: Project
    private var mChapter = ChunkPlugin.DEFAULT_CHAPTER
    private var mUnit = ChunkPlugin.DEFAULT_UNIT
    private lateinit var mHandler: Handler
    private lateinit var mChunks: ChunkPlugin

    private var mOnUnitChangedListener: OnUnitChangedListener? = null
    private var mMode: FragmentRecordingControls.Mode? = null

    private var _binding: FragmentRecordingFileBarBinding? = null
    private val binding get() = _binding!!

    interface OnUnitChangedListener {
        fun onUnitChanged(project: Project, fileName: String, chapter: Int)
    }

    private fun setMode(mode: FragmentRecordingControls.Mode) {
        mMode = mode
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentRecordingFileBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mHandler = Handler(Looper.getMainLooper())
        loadArgs(arguments)
        initializeViews()
        try {
            initializePickers()
            if (mMode == FragmentRecordingControls.Mode.INSERT_MODE) {
                disablePickers()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Logger.e(this.toString(), "onViewCreate", e)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnUnitChangedListener) {
            mOnUnitChangedListener = context
        } else {
            throw RuntimeException(
                "Attempted to attach activity which does not implement OnUnitChangedListener"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mOnUnitChangedListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadArgs(args: Bundle?) {
        mProject = args?.getParcelable(KEY_PROJECT)!!
        mChapter = args.getInt(KEY_CHAPTER, ChunkPlugin.DEFAULT_CHAPTER)
        mUnit = args.getInt(KEY_UNIT, ChunkPlugin.DEFAULT_UNIT)
    }

    private fun initializeViews() {
        //Logging to help track issue #669
        if (mProject.bookSlug == "") {
            Logger.e(
                this.toString(),
                "Project book is empty string $mProject"
            )
        }

        val languageCode = mProject.targetLanguageSlug
        binding.fileLanguage.text = languageCode.uppercase(Locale.getDefault())
        binding.fileLanguage.postInvalidate()

        val bookName = mProject.bookName
        binding.fileBook.text = bookName
        binding.fileBook.postInvalidate()

        binding.fileUnitLabel.text = mProject.getLocalizedModeName(requireActivity())
        binding.fileProject.text = mProject.versionSlug.uppercase(Locale.getDefault())
    }

    @Throws(IOException::class)
    private fun initializePickers() {
        mChunks = mProject.getChunkPlugin(ChunkPluginLoader(directoryProvider, assetProvider)).apply {
            initialize(mChapter, mUnit)
            val chapterLabel = if (chapterLabel == "chapter") getString(R.string.chapter_title) else ""
            binding.fileChapterLabel.text = chapterLabel
        }
        initializeUnitPicker()
        initializeChapterPicker()
    }

    private fun initializeChapterPicker() {
        val values = mChunks.chapterDisplayLabels
        if (!values.isNullOrEmpty()) {
            binding.chapterPicker.displayedValues = values
            binding.chapterPicker.setCurrent(mChunks.chapterLabelIndex)
            binding.chapterPicker.setOnValueChangedListener { _, _, _, direction ->
                if (direction == DIRECTION.INCREMENT) {
                    mChunks.nextChapter()
                } else {
                    mChunks.previousChapter()
                }
                binding.unitPicker.setCurrent(0)
                initializeUnitPicker()
                mOnUnitChangedListener?.onUnitChanged(
                    mProject,
                    mProject.getFileName(
                        mChunks.chapter,
                        mChunks.startVerse,
                        mChunks.endVerse
                    ),
                    mChunks.chapter
                )
            }
        } else {
            Logger.e(this.toString(), "values was null or of zero length")
        }
    }

    private fun initializeUnitPicker() {
        val values = mChunks.chunkDisplayLabels
        binding.unitPicker.displayedValues = values
        if (!values.isNullOrEmpty()) {
            binding.unitPicker.setCurrent(mChunks.startVerseLabelIndex)
            mOnUnitChangedListener?.onUnitChanged(
                mProject,
                mProject.getFileName(
                    mChunks.chapter,
                    mChunks.startVerse,
                    mChunks.endVerse
                ),
                mChunks.chapter
            )
            //reinitialize all of the filenames
            binding.unitPicker.setOnValueChangedListener { _, _, _, direction ->
                if (direction == DIRECTION.INCREMENT) {
                    mChunks.nextChunk()
                } else {
                    mChunks.previousChunk()
                }
                mOnUnitChangedListener?.onUnitChanged(
                    mProject,
                    mProject.getFileName(
                        mChunks.chapter,
                        mChunks.startVerse,
                        mChunks.endVerse
                    ),
                    mChunks.chapter
                )
            }
        } else {
            Logger.e(this.toString(), "values was null or of zero length")
        }
    }

    val startVerse: String
        get() = mChunks.startVerse.toString()

    val endVerse: String
        get() = mChunks.endVerse.toString()

    val unit: Int
        get() = mChunks.startVerse

    val chapter: Int
        get() = mChunks.chapter

    fun disablePickers() {
        binding.unitPicker.displayIncrementDecrement(false)
        binding.chapterPicker.displayIncrementDecrement(false)
    }

    companion object {
        private const val KEY_PROJECT = "key_project"
        const val KEY_CHAPTER: String = "key_chapter"
        const val KEY_UNIT: String = "key_unit"

        @JvmStatic
        fun newInstance(
            project: Project,
            chapter: Int,
            unit: Int,
            mode: FragmentRecordingControls.Mode
        ): FragmentRecordingFileBar {
            val f = FragmentRecordingFileBar()
            val args = Bundle()
            args.putParcelable(KEY_PROJECT, project)
            args.putInt(KEY_CHAPTER, chapter)
            args.putInt(KEY_UNIT, unit)
            f.arguments = args
            f.setMode(mode)
            return f
        }
    }
}
