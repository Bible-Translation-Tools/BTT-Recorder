package org.wycliffeassociates.translationrecorder.project.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import org.wycliffeassociates.translationrecorder.R
import org.wycliffeassociates.translationrecorder.databinding.FragmentScrollListItemBinding
import org.wycliffeassociates.translationrecorder.project.components.Language
import java.util.Locale


class TargetLanguageAdapter(
    targetLanguages: List<Language>,
    ctx: Context
) : ArrayAdapter<Any?>(ctx, R.layout.fragment_scroll_list_item) {
    private val languages: ArrayList<Language> = arrayListOf()
    private val filteredLanguages: ArrayList<Language> = arrayListOf()
    private var languageFilter: LanguageFilter? = null

    init {
        languages.clear()
        languages.addAll(targetLanguages.sorted())
        filteredLanguages.clear()
        filteredLanguages.addAll(languages)
    }

    override fun getCount(): Int {
        return filteredLanguages.size
    }

    override fun getItem(position: Int): Language? {
        return filteredLanguages.getOrNull(position)
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
        if (languageFilter == null) {
            languageFilter = LanguageFilter()
        }
        return languageFilter!!
    }

    private inner class LanguageFilter : Filter() {
        override fun performFiltering(charSequence: CharSequence?): FilterResults {
            val results = FilterResults()
            if (charSequence.isNullOrEmpty()) {
                // no filter
                results.values = languages
                results.count = languages.size
            } else {
                // perform filter
                val filteredCategories: MutableList<Language> = ArrayList()
                for (language in languages) {
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
            var filteredLanguages = filterResults.values as List<Language>
            if (!charSequence.isNullOrEmpty()) {
                filteredLanguages = sortedLanguages(filteredLanguages, charSequence)
            }
            this@TargetLanguageAdapter.filteredLanguages.clear()
            this@TargetLanguageAdapter.filteredLanguages.addAll(filteredLanguages)
            notifyDataSetChanged()
        }
    }

    private inner class ViewHolder(val binding: FragmentScrollListItemBinding) {
        init {
            binding.root.tag = this
        }

        fun bind(position: Int) {
            binding.majorText.text = getItem(position)?.name
            binding.minorText.text = getItem(position)?.slug

            binding.scrollListItemLayout.removeView(binding.itemIcon)
            binding.rightmostScrollListItemLayout.removeView(binding.moreIcon)
        }
    }

    /**
     * Sorts target languages by id
     * @param languages
     * @param referenceId languages are sorted according to the reference id
     * @return sorted languages
     */
    private fun sortedLanguages(
        languages: List<Language>,
        referenceId: CharSequence
    ): List<Language> {
        return languages.sortedWith { lhs, rhs ->
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