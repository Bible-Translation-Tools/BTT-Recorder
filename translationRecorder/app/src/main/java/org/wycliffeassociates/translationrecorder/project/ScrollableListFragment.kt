package org.wycliffeassociates.translationrecorder.project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import org.wycliffeassociates.translationrecorder.databinding.FragmentScrollListBinding

class ScrollableListFragment : Fragment(), Searchable {

    private var mSearchHint = ""

    private var mListener: OnItemClickListener? = null
    private var mAdapter: ArrayAdapter<*>? = null

    interface OnItemClickListener {
        fun onItemClick(result: Any?)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentScrollListBinding.inflate(inflater, container, false)

        binding.searchBar.searchBackButton.visibility = View.GONE
        binding.searchBar.searchMagIcon.visibility = View.GONE

        binding.list.adapter = mAdapter
        binding.list.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                mListener?.onItemClick(mAdapter?.getItem(position))
            }

        // if only one item is in the adapter, just choose it and continue on in the project wizard
        if (mAdapter?.count == 1) {
            mListener?.onItemClick(mAdapter?.getItem(0))
        }
        binding.searchBar.searchText.hint = mSearchHint
        binding.searchBar.searchText.isEnabled = false

        return binding.root
    }

    fun setAdapter(adapter: ArrayAdapter<*>?) {
        mAdapter = adapter
    }

    fun setSearchHint(hint: String) {
        mSearchHint = hint
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            this.mListener = context as OnItemClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnItemClickListener")
        }
    }

    override fun onSearchQuery(query: String) {
        mAdapter?.filter?.filter(query)
    }

    class Builder(adapter: ArrayAdapter<*>?) {
        private val mFragment = ScrollableListFragment()

        init {
            mFragment.setAdapter(adapter)
        }

        fun setSearchHint(hint: String): Builder {
            mFragment.setSearchHint(hint)
            return this
        }

        fun build(): ScrollableListFragment {
            return mFragment
        }
    }
}