package org.wycliffeassociates.translationrecorder.Playback.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.Playback.interfaces.VerseMarkerModeToggler
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin.TYPE
import org.wycliffeassociates.translationrecorder.databinding.FragmentFileViewBinding
import org.wycliffeassociates.translationrecorder.widgets.FourStepImageView
import java.util.Locale

/**
 * Created by sarabiaj on 11/4/2016.
 */
class FragmentFileBar : Fragment() {
    private var mInsertCallback: InsertCallback? = null
    private var mUnitType: TYPE? = null

    fun onRatingChanged(mRating: Int) {
        binding.btnRate.step = mRating
    }

    interface RerecordCallback {
        fun onRerecord()
    }

    interface RatingCallback {
        fun onOpenRating(view: FourStepImageView?)
    }

    interface InsertCallback {
        fun onInsert()
    }

    private var mModeToggleCallback: VerseMarkerModeToggler? = null
    private var mRatingCallback: RatingCallback? = null
    private var mRerecordCallback: RerecordCallback? = null

    private var _binding: FragmentFileViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentFileViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setText()
        setClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setText() {
        val args = arguments
        binding.fileLanguage.text = args?.getString(KEY_LANGUAGE)
        binding.fileProject.text = args?.getString(KEY_VERSION)
        binding.fileBook.text = args?.getString(KEY_BOOK)
        binding.fileChapterLabel.text = args?.getString(KEY_CHAPTER_LABEL)
        binding.fileChapter.text = args?.getString(KEY_CHAPTER_NUMBER)
        binding.fileUnitLabel.text = args?.getString(KEY_UNIT_LABEL)
        binding.fileUnit.text = args?.getString(KEY_UNIT_NUMBER)
        mUnitType = args?.getSerializable(KEY_UNIT_TYPE) as TYPE?
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mModeToggleCallback = context as VerseMarkerModeToggler
        mRerecordCallback = context as RerecordCallback
        mRatingCallback = context as RatingCallback
        mInsertCallback = context as InsertCallback
    }

    private fun setClickListeners() {
        binding.btnEnterVerseMarkerMode.setOnClickListener {
            mModeToggleCallback?.onEnableVerseMarkerMode()
        }

        binding.btnRate.setOnClickListener {
            mRatingCallback?.onOpenRating(binding.btnRate)
        }

        binding.btnRerecord.setOnClickListener {
            mRerecordCallback?.onRerecord()
        }
        binding.btnInsertRecord.setOnClickListener {
            mInsertCallback?.onInsert()
        }
    }

    companion object {
        var KEY_LANGUAGE: String = "language"
        var KEY_VERSION: String = "version"
        var KEY_BOOK: String = "book"
        var KEY_CHAPTER_LABEL: String = "chapter_label"
        var KEY_CHAPTER_NUMBER: String = "chapter_number"
        var KEY_UNIT_LABEL: String = "unit"
        var KEY_UNIT_NUMBER: String = "unit_number"
        var KEY_UNIT_TYPE: String = "unit_type"

        fun newInstance(
            language: String,
            version: String,
            book: String,
            chapterLabel: String?,
            chapterNumber: String?,
            unitLabel: String?,
            unitNumber: String?,
            unitType: TYPE?
        ): FragmentFileBar {
            val f = FragmentFileBar()
            val args = Bundle()
            args.putString(KEY_LANGUAGE, language.uppercase(Locale.getDefault()))
            args.putString(KEY_VERSION, version.uppercase(Locale.getDefault()))
            args.putString(KEY_BOOK, book.uppercase(Locale.getDefault()))
            args.putString(KEY_CHAPTER_LABEL, chapterLabel)
            args.putString(KEY_CHAPTER_NUMBER, chapterNumber)
            args.putString(KEY_UNIT_LABEL, unitLabel)
            args.putString(KEY_UNIT_NUMBER, unitNumber)
            args.putSerializable(KEY_UNIT_TYPE, unitType)
            f.arguments = args
            return f
        }
    }
}
