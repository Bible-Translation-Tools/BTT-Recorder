package org.wycliffeassociates.translationrecorder.ProjectManager.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.DialogRatingBinding
import org.wycliffeassociates.translationrecorder.project.TakeInfo

/**
 * Created by leongv on 8/9/2016.
 */
class RatingDialog : DialogFragment() {
    interface DialogListener {
        fun onPositiveClick(dialog: RatingDialog)
        fun onNegativeClick(dialog: RatingDialog)
    }

    private var listener: DialogListener? = null
    lateinit var takeInfo: TakeInfo
        private set

    private var _binding: DialogRatingBinding? = null
    private val binding get() = _binding!!

    private var _rating = 0
    var rating: Int
        get() = _rating
        private set(rating) {
            _rating = rating
            when (_rating) {
                1 -> {
                    binding.oneStarRating.step = 1
                    binding.twoStarRating.step = 0
                    binding.threeStarRating.step = 0
                }
                2 -> {
                    binding.oneStarRating.step = 2
                    binding.twoStarRating.step = 2
                    binding.threeStarRating.step = 0
                }

                3 -> {
                    binding.oneStarRating.step = 3
                    binding.twoStarRating.step = 3
                    binding.threeStarRating.step = 3
                }

                else -> {
                    binding.oneStarRating.step = 0
                    binding.twoStarRating.step = 0
                    binding.threeStarRating.step = 0
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        takeInfo = args?.getParcelable(TAKE_INFO)!!
        _rating = args.getInt(CURRENT_RATING_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogRatingBinding.inflate(layoutInflater)

        val alertDialog = AlertDialog.Builder(activity)
            .setTitle(getString(R.string.rate_take))
            .setView(binding.root)
            .setPositiveButton(
                getString(R.string.label_ok)
            ) { _, _ -> listener?.onPositiveClick(this@RatingDialog) }
            .setNegativeButton(
                getString(R.string.title_cancel)
            ) { _, _ -> listener?.onNegativeClick(this@RatingDialog) }
            .create()

        alertDialog.setOnShowListener {
            binding.oneStarRating.setOnClickListener { rating = 1 }
            binding.twoStarRating.setOnClickListener { rating = 2 }
            binding.threeStarRating.setOnClickListener { rating = 3 }

            rating = _rating
        }

        return alertDialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement RatingDialogListener")
        }
    }

    companion object {
        var TAKE_INFO: String = "key_take_info"
        var CURRENT_RATING_KEY: String = "key_current_rating"

        @JvmStatic
        fun newInstance(takeInfo: TakeInfo?, currentRating: Int): RatingDialog {
            val rate = RatingDialog()
            val args = Bundle()
            args.putParcelable(TAKE_INFO, takeInfo)
            args.putInt(CURRENT_RATING_KEY, currentRating)
            rate.arguments = args
            return rate
        }
    }
}