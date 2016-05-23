package com.mukera.sheket.client.controller.transactions;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.util.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by gamma on 3/5/16.
 */
public class ItemSearchFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final String SEARCH_BRANCH_ID_KEY = "search_branch_id_key";

    private long mBranchId;

    private ListView mSearchList;
    private CursorAdapter mSearchAdapter;

    private EditText mSearchText;
    private TextView mResultLabel;

    private String mCurrSearch;
    private Button mCancel, mFinish;

    private SearchResultListener mListener;

    public void setResultListener(SearchResultListener listener) {
        mListener = listener;
    }

    public static ItemSearchFragment newInstance(long branch_id) {
        ItemSearchFragment fragment = new ItemSearchFragment();
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

            getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_input, container, false);

        mSearchList = (ListView) rootView.findViewById(R.id.list_view_manual_results);
        mSearchAdapter = new ItemSearchCursorAdapter(getContext());
        mSearchList.setAdapter(mSearchAdapter);
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mSearchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item;
                    item = new SItem(cursor);

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
        mSearchText.addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                mCurrSearch = s.toString().trim().toUpperCase();
                getLoaderManager().restartLoader(LoaderId.SEARCH_RESULT_LOADER,
                        null, ItemSearchFragment.this);
            }
        });

        final Button searchBtn = (Button) rootView.findViewById(R.id.btn_manual_search);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
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

        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        CursorLoader loader;
        loader = new CursorLoader(getActivity(),
            ItemEntry.buildBaseUri(company_id),
            SItem.ITEM_COLUMNS,
            selection, null,
            sortOrder);

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

    public interface SearchResultListener {
        void itemSelected(SItem item);

        void finishTransaction();
        void cancelTransaction();
    }

    public static class ItemSearchCursorAdapter extends CursorAdapter {
        private static class ViewHolder {
            TextView item_name;
            TextView item_code;

            public ViewHolder(View view) {
                item_name = (TextView) view.findViewById(R.id.list_item_text_view_item_name);
                item_code = (TextView) view.findViewById(R.id.list_item_text_view_item_code);
            }
        }

        public ItemSearchCursorAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_search_item, parent, false);
            ViewHolder holder = new ViewHolder(view);

            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            SItem item = new SItem(cursor);

            holder.item_name.setText(item.name);
            String code;
            if (item.has_bar_code) {
                code = item.bar_code;
            } else {
                code = item.manual_code;
            }
            holder.item_code.setText(code);
        }
    }
}
