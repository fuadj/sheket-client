package com.mukera.sheket.client.controller.item_searcher;

import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.TextWatcherAdapter;
import com.mukera.sheket.client.controller.transactions.TransactionActivity;
import com.mukera.sheket.client.controller.item_searcher.adapters.BranchItemSearchCursorAdapter;
import com.mukera.sheket.client.controller.item_searcher.adapters.ItemSearchCursorAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by gamma on 3/5/16.
 */
public class ManualSearchFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final String SEARCH_BRANCH_ID_KEY = "search_branch_id_key";

    //private boolean mIsBranchSpecified;
    private long mBranchId;

    private ListView mSearchList;
    //private ItemSearchCursorAdapter mSearchAdapter;
    private CursorAdapter mSearchAdapter;

    private EditText mSearchText;
    private TextView mResultLabel;

    private String mCurrSearch;
    private Button mCancel, mFinish;

    private ItemSearchResultListener mListener;

    //private CheckBox mCheckBoxSearchOnlyBranchItems;
    //private boolean mSearchOnlyBranchItems = true;

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
            //mIsBranchSpecified = TransactionActivity.isBranchSpecified(mBranchId);

            // the default is to only search the current branch
            // only admin users can the entire item list
            //mSearchOnlyBranchItems = true;

            getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        }
    }

    boolean isSearchingBranchItems() {
        return false;
        //return mIsBranchSpecified && mSearchOnlyBranchItems;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manual_input, container, false);

        //int user_permission = PrefUtil.getUserPermission(getActivity());
        int user_permission = SPermission.getSingletonPermission().getPermissionType();

        /*
        mCheckBoxSearchOnlyBranchItems = (CheckBox) rootView.findViewById(R.id.check_box_manual_search_only_branch_items);
        if (user_permission == SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            mCheckBoxSearchOnlyBranchItems.setVisibility(View.VISIBLE);
        } else {
            mCheckBoxSearchOnlyBranchItems.setVisibility(View.GONE);
        }

        mCheckBoxSearchOnlyBranchItems.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSearchOnlyBranchItems = isChecked;
                mSearchAdapter = null;
                if (isSearchingBranchItems()) {
                    mSearchAdapter = new BranchItemSearchCursorAdapter(getContext());
                } else {
                    mSearchAdapter = new ItemSearchCursorAdapter(getContext());
                }
                mSearchList.setAdapter(mSearchAdapter);
                getLoaderManager().restartLoader(LoaderId.SEARCH_RESULT_LOADER,
                        null, ManualSearchFragment.this);
            }
        });
        */

        mSearchList = (ListView) rootView.findViewById(R.id.list_view_manual_results);
        //mSearchAdapter = new ItemSearchCursorAdapter(getActivity());
        mSearchAdapter = new ItemSearchCursorAdapter(getContext());
        /*
        if (mIsBranchSpecified) {
            mSearchAdapter = new BranchItemSearchCursorAdapter(getContext());
        } else {
            mSearchAdapter = new ItemSearchCursorAdapter(getContext());
        }
        */
        mSearchList.setAdapter(mSearchAdapter);
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mSearchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item;
                    if (isSearchingBranchItems()) {
                        SBranchItem branchItem = new SBranchItem(cursor, true);
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
        mSearchText.addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                mCurrSearch = s.toString().trim().toUpperCase();
                getLoaderManager().restartLoader(LoaderId.SEARCH_RESULT_LOADER,
                        null, ManualSearchFragment.this);
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
        if (isSearchingBranchItems()) {
            loader = new CursorLoader(getActivity(),
                    BranchItemEntry.buildAllItemsInBranchUri(company_id, mBranchId),
                    SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                    selection, null,
                    sortOrder);
        } else {
            loader = new CursorLoader(getActivity(),
                ItemEntry.buildBaseUri(company_id),
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
