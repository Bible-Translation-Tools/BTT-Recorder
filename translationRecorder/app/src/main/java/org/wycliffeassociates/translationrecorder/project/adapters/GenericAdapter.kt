package org.wycliffeassociates.translationrecorder.project.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentScrollListItemBinding
import org.wycliffeassociates.translationrecorder.project.components.Mode
import org.wycliffeassociates.translationrecorder.project.components.ProjectComponent
import java.util.Locale


/**
 * Created by joel on 9/4/2015.
 */
class GenericAdapter(
    component: List<ProjectComponent>,
    ctx: Context
) : ArrayAdapter<Any?>(ctx, R.layout.fragment_scroll_list_item) {
    private val mProjectComponents: MutableList<ProjectComponent> = arrayListOf()
    private val mFilteredProjectComponents: MutableList<ProjectComponent> = arrayListOf()
    private var mProjectComponentFilter: ProjectComponentFilter? = null

    init {
        mProjectComponents.addAll(component.sorted())
        mFilteredProjectComponents.addAll(mProjectComponents)
    }


    override fun getCount(): Int {
        return mFilteredProjectComponents.size
    }

    override fun getItem(position: Int): ProjectComponent {
        return mFilteredProjectComponents[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val holder: ViewHolder
        val binding: FragmentScrollListItemBinding
        if (convertView == null) {
            binding = FragmentScrollListItemBinding.inflate(LayoutInflater.from(context))
            holder = ViewHolder(binding)
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.bind(position)

        return holder.binding.root
    }

    /**
     * Returns the target language filter
     * @return
     */
    override fun getFilter(): Filter {
        if (mProjectComponentFilter == null) {
            mProjectComponentFilter = ProjectComponentFilter()
        }
        return mProjectComponentFilter!!
    }

    private inner class ProjectComponentFilter : Filter() {
        override fun performFiltering(charSequence: CharSequence?): FilterResults {
            val results = FilterResults()
            if (charSequence.isNullOrEmpty()) {
                // no filter
                results.values = mProjectComponents
                results.count = mProjectComponents.size
            } else {
                // perform filter
                val filteredCategories: MutableList<ProjectComponent> = ArrayList()
                for (language in mProjectComponents) {
                    // match the target language id
                    var match = language.slug.lowercase(Locale.getDefault()).startsWith(
                        charSequence.toString().lowercase(
                            Locale.getDefault()
                        )
                    )
                    if (!match) {
                        if (language.name.lowercase(Locale.getDefault()).startsWith(
                                charSequence.toString().lowercase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            // match the target language name
                            match = true
                        }
                    }
                    if (match) {
                        filteredCategories.add(language)
                    }
                }
                results.values = filteredCategories
                results.count = filteredCategories.size
            }
            return results
        }

        override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
            var filteredProjectComponents = filterResults.values as List<ProjectComponent>
            if (!charSequence.isNullOrEmpty()) {
                filteredProjectComponents = sortProjectComponents(filteredProjectComponents, charSequence)
            }
            mFilteredProjectComponents.clear()
            mFilteredProjectComponents.addAll(filteredProjectComponents)
            notifyDataSetChanged()
        }
    }

    private fun getLocalizedModeLabel(ctx: Context, label: String): String {
        val chunk = ctx.getString(R.string.chunk_title)
        val verse = ctx.getString(R.string.title_verse)

        return when (label) {
            Mode.CHUNK -> chunk
            Mode.VERSE -> verse
            else -> label
        }
    }

    private inner class ViewHolder(val binding: FragmentScrollListItemBinding) {
        init {
            binding.root.tag = this
        }

        fun bind(position: Int) {
            // render view
            val label = getLocalizedModeLabel(context, getItem(position).label)
            binding.majorText.text = label
            binding.minorText.text = getItem(position).slug

            if (!mFilteredProjectComponents[position].displayItemIcon()) {
                binding.scrollListItemLayout.removeView(binding.itemIcon)
            }
            if (!mFilteredProjectComponents[position].displayMoreIcon()) {
                binding.rightmostScrollListItemLayout.removeView((binding.moreIcon))
            }
        }
    }

    companion object {
        /**
         * Sorts components by id
         * @param components
         * @param referenceId languages are sorted according to the reference id
         */
        private fun sortProjectComponents(
            components: List<ProjectComponent>,
            referenceId: CharSequence
        ): List<ProjectComponent> {
            return components.sortedWith { lhs, rhs ->
                var lhId = lhs.slug
                var rhId = rhs.slug
                // give priority to matches with the reference
                if (lhId.startsWith(referenceId.toString().lowercase(Locale.getDefault()))) {
                    lhId = "!$lhId"
                }
                if (rhId.startsWith(referenceId.toString().lowercase(Locale.getDefault()))) {
                    rhId = "!$rhId"
                }
                lhId.compareTo(rhId, ignoreCase = true)
            }
        }
    }
}