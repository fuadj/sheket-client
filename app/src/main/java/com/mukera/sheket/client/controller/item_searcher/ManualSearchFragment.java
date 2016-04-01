package com.mukera.sheket.client.controller.item_searcher;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.TransactionActivity;
import com.mukera.sheket.client.controller.item_searcher.adapters.BranchItemSearchCursorAdapter;
import com.mukera.sheket.client.controller.item_searcher.adapters.ItemSearchCursorAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public class ManualSearchFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final String SEARCH_BRANCH_ID_KEY = "search_branch_id_key";

    private boolean mIsBranchSpecified;
    private long mBranchId;

    private ListView mSearchList;
    //private ItemSearchCursorAdapter mSearchAdapter;
    private CursorAdapter mSearchAdapter;

    private EditText mSearchText;
    private TextView mResultLabel;

    private String mCurrSearch;
    private Button mCancel, mFinish;

    private ItemSearchResultListener mListener;

    public void setInputFragmentListener(ItemSearchResultListener listener) {
        mListener = listener;
    }

    public static ManualSearchFragment newInstance(long branch_id) {
        ManualSearchFragment fragment = new ManualSearchFragment();
        Bundle args = new Bundle();
        args.putLong(SEARCH_BRANCH_ID_KEY, branch_id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mBranchId = args.getLong(SEARCH_BRANCH_ID_KEY, TransactionActivity.BRANCH_ID_NONE);
            mIsBranchSpecified = TransactionActivity.isBranchSpecified(mBranchId);

            getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_input, container, false);

        mSearchList = (ListView) rootView.findViewById(R.id.list_view_manual_results);
        //mSearchAdapter = new ItemSearchCursorAdapter(getActivity());
        if (mIsBranchSpecified) {
            mSearchAdapter = new BranchItemSearchCursorAdapter(getContext());
        } else {
            mSearchAdapter = new ItemSearchCursorAdapter(getContext());
        }
        mSearchList.setAdapter(mSearchAdapter);
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mSearchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item;
                    if (mIsBranchSpecified) {
                        SBranchItem branchItem = new SBranchItem(cursor);
                        item = branchItem.item;
                    } else {
                        item = new SItem(cursor);
                    }

                    if (mListener != null)
                        mListener.itemSelected(item);
                }
            }
        });

        mCancel = (Button) rootView.findViewById(R.id.btn_manual_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.cancelTransaction();
            }
        });
        mFinish = (Button) rootView.findViewById(R.id.btn_manual_finish);
        mFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.finishTransaction();
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
                        null, ManualSearchFragment.this);
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
        String sortOrder = ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " ASC";

        if (mCurrSearch != null) mCurrSearch = mCurrSearch.trim();

        String selection = null;

        if (mCurrSearch != null && !mCurrSearch.isEmpty()) {
            selection = ItemEntry._full(ItemEntry.COLUMN_MANUAL_CODE) + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_BAR_CODE) + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_NAME) + " LIKE '%" + mCurrSearch + "%'";
        }

        CursorLoader loader;
        if (mIsBranchSpecified) {
            loader = new CursorLoader(getActivity(),
                    BranchItemEntry.buildItemsInBranchUri(mBranchId),
                    SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMN,
                    selection, null,
                    sortOrder);
        } else {
            loader = new CursorLoader(getActivity(),
                ItemEntry.CONTENT_URI,
                SItem.ITEM_COLUMNS,
                selection, null,
                sortOrder);
        }

        return loader;
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
