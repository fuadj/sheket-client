package com.mukera.sheket.client.input;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.b_actions.ItemSelectionListener;
import com.mukera.sheket.client.input.SearchCursorAdapter;
import com.mukera.sheket.client.contentprovider.SheketContract.ItemEntry;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public class ManualInputFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private ListView mSearchList;
    private SearchCursorAdapter mSearchAdapter;

    private EditText mSearchText;
    private TextView mResultLabel;

    private String mCurrSearch;

    private ItemSelectionListener mListener;

    public void setItemSelectedListener(ItemSelectionListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_input, container, false);

        mSearchList = (ListView) rootView.findViewById(R.id.list_view_manual_results);
        mSearchAdapter = new SearchCursorAdapter(getActivity());
        mSearchList.setAdapter(mSearchAdapter);
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mSearchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item = new SItem(cursor);

                    if (mListener != null)
                        mListener.itemSelected(item);
                }
            }
        });

        mResultLabel = (TextView) rootView.findViewById(R.id.text_view_manual_result_label);

        mSearchText = (EditText) rootView.findViewById(R.id.edit_text_manual_code);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mCurrSearch = s.toString().trim().toUpperCase();
                getLoaderManager().restartLoader(LoaderId.SEARCH_RESULT_LOADER,
                        null, ManualInputFragment.this);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        String sortOrder = ItemEntry._full(ItemEntry._ID) + " ASC";

        if (mCurrSearch != null) mCurrSearch = mCurrSearch.trim();

        String selection = null;

        if (mCurrSearch != null && !mCurrSearch.isEmpty()) {
            selection = ItemEntry.COLUMN_MANUAL_CODE + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry.COLUMN_BAR_CODE + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry.COLUMN_NAME + " LIKE '%" + mCurrSearch + "%'";
        }

        return new CursorLoader(getActivity(),
                ItemEntry.CONTENT_URI,
                SItem.ITEM_COLUMNS,
                selection, null,
                sortOrder
        );
    }

    void setSearchStatus(boolean found_items) {
        String str;
        if (found_items)
            str = "Search Results";
        else {
            str = "No result found";
            if (mCurrSearch != null)
                str = String.format("No result found for: %s", mCurrSearch);
        }
        mResultLabel.setText(str);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mSearchAdapter.swapCursor(data);
        setSearchStatus(!mSearchAdapter.isEmpty());
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mSearchAdapter.swapCursor(null);
        setSearchStatus(false);
    }
}
