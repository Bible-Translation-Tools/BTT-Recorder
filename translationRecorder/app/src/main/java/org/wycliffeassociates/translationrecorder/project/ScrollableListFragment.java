package org.wycliffeassociates.translationrecorder.project;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import org.wycliffeassociates.translationrecorder.R;


public class ScrollableListFragment extends Fragment implements Searchable {
    private OnItemClickListener mListener;
    private ArrayAdapter mAdapter;
    private String mSearchHint = "";

    public interface OnItemClickListener {
        void onItemClick(Object result);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scroll_list, container, false);
        ImageButton searchBackButton = rootView.findViewById(R.id.search_back_button);
        searchBackButton.setVisibility(View.GONE);
        ImageView searchIcon = rootView.findViewById(R.id.search_mag_icon);
        searchIcon.setVisibility(View.GONE);

        ListView list = rootView.findViewById(R.id.list);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> mListener.onItemClick(mAdapter.getItem(position)));

        //if only one item is in the adapter, just choose it and continue on in the project wizard
        if(mAdapter != null && mAdapter.getCount() == 1) {
            mListener.onItemClick(mAdapter.getItem(0));
        }
        EditText searchView = rootView.findViewById(R.id.search_text);
        searchView.setHint(mSearchHint);
        searchView.setEnabled(false);

        return rootView;
    }

    public void setAdapter(ArrayAdapter adapter){
        mAdapter = adapter;
    }

    public void setSearchHint(String hint){
        mSearchHint = hint;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnItemClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnItemClickListener");
        }
    }

    @Override
    public void onSearchQuery(String query) {
        if(mAdapter != null) {
            mAdapter.getFilter().filter(query);
        }
    }

    public static class Builder{
        private ScrollableListFragment mFragment;

        public Builder(ArrayAdapter adapter){
            mFragment = new ScrollableListFragment();
            mFragment.setAdapter(adapter);
        }
        public Builder setSearchHint(String hint){
            mFragment.setSearchHint(hint);
            return this;
        }

        public ScrollableListFragment build(){
            return mFragment;
        }

    }
}