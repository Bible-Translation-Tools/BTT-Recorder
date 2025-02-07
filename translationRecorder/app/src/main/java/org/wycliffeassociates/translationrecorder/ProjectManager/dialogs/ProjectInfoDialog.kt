package org.wycliffeassociates.translationrecorder.ProjectManager.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wycliffeassociates.translationrecorder.FilesPage.Export.AppExport
import org.wycliffeassociates.translationrecorder.FilesPage.Export.Export
import org.wycliffeassociates.translationrecorder.FilesPage.Export.FolderExport
import org.wycliffeassociates.translationrecorder.FilesPage.Export.TranslationExchangeExport
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Screen
import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ProjectLayoutDialogBinding
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.getDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.SourceAudioActivity
import org.wycliffeassociates.translationrecorder.project.components.Language
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Created by sarabiaj on 6/27/2016.
 */
@AndroidEntryPoint
class ProjectInfoDialog : DialogFragment() {

    @Inject lateinit var db: IProjectDatabaseHelper
    @Inject lateinit var directoryProvider: IDirectoryProvider
    @Inject lateinit var prefs: IPreferenceRepository
    @Inject lateinit var assetsProvider: AssetsProvider

    interface InfoDialogCallback {
        fun onDelete(project: Project)
    }

    interface ExportDelegator {
        fun delegateExport(exp: Export)
    }

    interface SourceAudioDelegator {
        fun delegateSourceAudio(project: Project)
    }

    private lateinit var project: Project
    var export: Export? = null
    private var exportDelegator: ExportDelegator? = null

    private var _binding: ProjectLayoutDialogBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        exportDelegator = context as ExportDelegator
    }

    override fun onDetach() {
        super.onDetach()
        exportDelegator = null
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = ProjectLayoutDialogBinding.inflate(layoutInflater)

        Screen.lockOrientation(requireActivity())

        val builder = AlertDialog.Builder(requireActivity())
        builder.setView(binding.root)

        project = arguments?.getParcelable(Project.PROJECT_EXTRA)!!

        val languageCode = project.targetLanguageSlug
        val language = db.getLanguageName(languageCode)
        val bookCode = project.bookSlug
        val book = project.bookName
        val translation = project.versionSlug

        binding.title.text = "$book - $language"
        binding.projectTitle.text = "$book ($bookCode)"
        binding.languageTitle.text = "$language ($languageCode)"

        when (translation) {
            "ulb" -> {
                binding.translationTypeTitle.text = "Unlocked Literal Bible ($translation)"
            }
            "udb" -> {
                binding.translationTypeTitle.text = "Unlocked Dynamic Bible ($translation)"
            }
            else -> {
                binding.translationTypeTitle.text =
                    "Regular (" + translation.uppercase(Locale.getDefault()) + ")"
            }
        }

        setSourceAudioTextInfo()

        binding.unitTitle.text = project.getLocalizedModeName(requireActivity())

        binding.deleteButton.setOnClickListener {
            dismiss()
            (activity as InfoDialogCallback).onDelete(project)
        }

        binding.folderButton.setOnClickListener {
            export = FolderExport(project, directoryProvider).apply {
                exportDelegator?.delegateExport(this)
            }
        }

        binding.publishButton.setOnClickListener {
            val server = prefs.getDefaultPref(
                SettingsActivity.KEY_PREF_UPLOAD_SERVER,
                getString(R.string.pref_upload_server)
            )
            export = TranslationExchangeExport(
                project,
                db,
                directoryProvider,
                prefs,
                assetsProvider,
                server
            ).apply {
                exportDelegator?.delegateExport(this)
            }
        }

        binding.otherButton.setOnClickListener {
            export = AppExport(project, directoryProvider).apply {
                exportDelegator?.delegateExport(this)
            }
        }

        binding.exportAsSourceBtn.setOnClickListener {
            (exportDelegator as SourceAudioDelegator).delegateSourceAudio(project)
        }

        binding.editSourceLanguage.setOnClickListener {
            val intent = SourceAudioActivity.getSourceAudioIntent(requireActivity())
            startActivityForResult(intent, SOURCE_AUDIO_REQUEST)
        }

        binding.editSourceLocation.setOnClickListener {
            val intent = SourceAudioActivity.getSourceAudioIntent(requireActivity())
            startActivityForResult(intent, SOURCE_AUDIO_REQUEST)
        }

        return builder.create()
    }

    private fun setSourceAudioTextInfo() {
        val sourceLanguageCode = project.sourceLanguageSlug
        val sourceLanguageName = if (db.languageExists(sourceLanguageCode)) {
            db.getLanguageName(sourceLanguageCode)
        } else ""
        binding.sourceAudioLanguage.text = String.format("%s - (%s)", sourceLanguageName, sourceLanguageCode)
        binding.sourceAudioLocation.text = project.sourceAudioPath?.let { File(it).name }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SOURCE_AUDIO_REQUEST) {
                val projectId = db.getProjectId(project)
                val sourceLanguage = data?.getParcelableExtra<Language>(Project.SOURCE_LANGUAGE_EXTRA)
                val sourceLocation = data?.getStringExtra(Project.SOURCE_LOCATION_EXTRA)

                if (sourceLanguage != null || sourceLocation != null) {
                    project.sourceLanguage = sourceLanguage
                    project.sourceAudioPath = sourceLocation
                    db.updateSourceAudio(projectId, project)
                    setSourceAudioTextInfo()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Screen.unlockOrientation(requireActivity())
    }

    companion object {
        private const val SOURCE_AUDIO_REQUEST = 223
    }
}
