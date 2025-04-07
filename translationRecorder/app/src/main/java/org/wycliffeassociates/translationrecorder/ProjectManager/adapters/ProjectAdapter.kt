package org.wycliffeassociates.translationrecorder.ProjectManager.adapters

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import org.wycliffeassociates.translationrecorder.ProjectManager.activities.ActivityChapterList
import org.wycliffeassociates.translationrecorder.ProjectManager.dialogs.ProjectInfoDialog
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.Recording.RecordingActivity
import org.wycliffeassociates.translationrecorder.chunkplugin.ChunkPlugin
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.databinding.ProjectListItemBinding
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.project.Project

/**
 * Creates a custom view for the audio entries in the file screen.
 */
class ProjectAdapter(
    var mCtx: Activity,
    private val mProjectList: List<Project>,
    private val db: IProjectDatabaseHelper,
    private val prefs: IPreferenceRepository
) : ArrayAdapter<Any?>(mCtx, R.layout.project_list_item, mProjectList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val holder: ViewHolder
        val binding: ProjectListItemBinding
        if (convertView == null) {
            binding = ProjectListItemBinding.inflate(LayoutInflater.from(context))
            holder = ViewHolder(binding)
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.bind(position)

        return holder.binding.root
    }

    companion object {
        fun initializeProjectCard(
            ctx: Activity,
            project: Project,
            db: IProjectDatabaseHelper,
            prefs: IPreferenceRepository,
            projectCard: ProjectListItemBinding
        ) {
            val book = project.bookName
            projectCard.bookTextView.text = book

            val language = db.getLanguageName(project.targetLanguageSlug)
            projectCard.languageTextView.text = language

            // Get project's progress
            if (db.projectExists(project)) {
                val projectID = db.getProjectId(project)
                projectCard.progressPie.progress = db.getProjectProgress(projectID)
            }

            projectCard.recordButton.setOnClickListener { v ->
                project.loadProjectIntoPreferences(db, prefs)
                //TODO: should find place left off at?
                v.context.startActivity(
                    RecordingActivity.getNewRecordingIntent(
                        v.context,
                        project,
                        ChunkPlugin.DEFAULT_CHAPTER,
                        ChunkPlugin.DEFAULT_UNIT
                    )
                )
            }

            projectCard.infoButton.setOnClickListener {
                val info: DialogFragment = ProjectInfoDialog()
                val args = Bundle()
                args.putParcelable(Project.PROJECT_EXTRA, project)
                info.arguments = args
                info.show((ctx as AppCompatActivity).supportFragmentManager, "title")
            }

            projectCard.textLayout.setOnClickListener { v ->
                val intent = Intent(v.context, ActivityChapterList::class.java)
                intent.putExtra(Project.PROJECT_EXTRA, project)
                v.context.startActivity(intent)
            }
        }
    }

    private inner class ViewHolder(val binding: ProjectListItemBinding) {
        init {
            binding.root.tag = this
        }

        fun bind(position: Int) {
            initializeProjectCard(
                mCtx,
                mProjectList[position],
                db,
                prefs,
                binding
            )
        }
    }
}
