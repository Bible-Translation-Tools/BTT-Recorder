package org.wycliffeassociates.translationrecorder.Reporting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.MainMenu
import org.wycliffeassociates.translationrecorder.databinding.FragmentBugReportBinding

/**
 * Created by leongv on 12/8/2015.
 */
class BugReportDialog : DialogFragment() {
    lateinit var mm: MainMenu

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)

        mm = activity as MainMenu

        binding.send.setOnClickListener {
            sendReport()
        }
        binding.cancel.setOnClickListener {
            cancelReport()
        }

        return binding.root
    }

    private fun sendReport() {
        mm.report(binding.crashReport.text.toString())
        this.dismiss()
    }

    private fun cancelReport() {
        mm.archiveStackTraces()
        this.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}