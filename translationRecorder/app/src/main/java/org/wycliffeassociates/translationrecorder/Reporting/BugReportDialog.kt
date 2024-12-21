package org.wycliffeassociates.translationrecorder.Reporting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentBugReportBinding

/**
 * Created by leongv on 12/8/2015.
 */
class BugReportDialog : DialogFragment(), View.OnClickListener {
    var mm: MainMenu? = null
    var message: EditText? = null

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)

        mm = activity as MainMenu

        binding.deleteConfirm.setOnClickListener(this)
        binding.deleteCancel.setOnClickListener(this)

        return binding.root
    }

    override fun onClick(view: View) {
        if (view.id == R.id.delete_confirm) {
            mm?.report(message?.text.toString())
            this.dismiss()
        } else {
            mm?.archiveStackTraces()
            this.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}