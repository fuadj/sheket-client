package com.mukera.sheket.client.controller.transactions;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.items.EmbeddedCategoryFragment;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/5/16.
 */
public class ItemSearchFragment extends EmbeddedCategoryFragment {
    public static final String SEARCH_BRANCH_ID_KEY = "search_branch_id_key";
    public static final String SEARCH_TRANS_TYPE = "trans_type";

    private long mBranchId;
    private boolean mIsBuyingTransaction;

    private ListView mSearchList;
    private CursorAdapter mSearchAdapter;

    private EditText mSearchText;
    private TextView mResultLabel, mNumItems;

    private String mCurrSearch;
    private Button mCancel, mFinish, mBtnCategory;

    private SearchResultListener mListener;

    private List<SBranch> mBranches = null;     // this is lazily instantiated

    private List<CategoryTreeGenerator.CategoryNode> mCategoryNodes = null;
    private String[] mCategoryNames = null;
    private int mSavedSelectedCategoryIndex = 0;

    // setting ROOT category means you don't want to filter by category!!!
    private long mSelectedCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
    // whether the category came from the category selection dialog?
    private boolean mIsCategoryDialogSelected = false;

    public void setResultListener(SearchResultListener listener) {
        mListener = listener;
    }

    public static ItemSearchFragment newInstance(long branch_id, boolean is_buying_trans) {
        ItemSearchFragment fragment = new ItemSearchFragment();
        Bundle args = new Bundle();
        args.putLong(SEARCH_BRANCH_ID_KEY, branch_id);
        args.putBoolean(SEARCH_TRANS_TYPE, is_buying_trans);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mBranchId = args.getLong(SEARCH_BRANCH_ID_KEY, com.mukera.sheket.client.controller.transactions.TransactionActivity.BRANCH_ID_NONE);
        mIsBuyingTransaction = args.getBoolean(SEARCH_TRANS_TYPE, true);
    }

    @Override
    protected int getCategoryLoaderId() {
        return LoaderId.TransactionActivity.ITEM_SEARCH_CATEGORY_LOADER;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_item_search;
    }

    @Override
    public void onInitLoader() {
        getLoaderManager().initLoader(LoaderId.TransactionActivity.ITEM_SEARCH_LOADER, null, this);
    }

    @Override
    public void onRestartLoader() {
        getLoaderManager().restartLoader(LoaderId.TransactionActivity.ITEM_SEARCH_LOADER, null, this);
    }

    @Override
    public void onCategorySelected(long category_id) {
        mSelectedCategoryId = category_id;
        mIsCategoryDialogSelected = false;
        //setParentCategoryId(mSelectedCategoryId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mSearchList = (ListView) rootView.findViewById(R.id.item_search_list_view_items);
        mSearchAdapter = new ItemSearchCursorAdapter(getContext());
        mSearchList.setAdapter(mSearchAdapter);
        mSearchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mSearchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item;
                    item = new SItem(cursor);

                    displayQuantityDialog(item);
                }
            }
        });

        mBtnCategory = (Button) rootView.findViewById(R.id.item_search_btn_set_category);
        mBtnCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                getCategories();
                builder.setTitle("Select Category").setSingleChoiceItems(mCategoryNames,
                        mSavedSelectedCategoryIndex,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSavedSelectedCategoryIndex = which;
                                mSelectedCategoryId = mCategoryNodes.get(which).category.category_id;
                                mIsCategoryDialogSelected = true;
                                mBtnCategory.setText(mCategoryNames[which]);
                                dialog.dismiss();

                                ItemSearchFragment.this.addCategoryToStack(mSelectedCategoryId);

                                restartLoader();
                            }
                        }).create().show();
            }
        });

        mCancel = (Button) rootView.findViewById(R.id.item_search_btn_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.cancelTransaction();
            }
        });
        mFinish = (Button) rootView.findViewById(R.id.item_search_btn_finish);
        mFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.finishTransaction();
            }
        });
        mFinish.setEnabled(mListener.numItemsInTransaction() > 0);

        mResultLabel = (TextView) rootView.findViewById(R.id.item_search_text_view_search_result);
        mNumItems = (TextView) rootView.findViewById(R.id.item_search_text_view_num_items);
        adjustNumItemsCounterVisibility();

        mSearchText = (EditText) rootView.findViewById(R.id.item_search_edit_text_keyword);
        mSearchText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                mCurrSearch = s.toString().trim().toUpperCase();
                restartLoader();
            }
        });

        final Button searchBtn = (Button) rootView.findViewById(R.id.item_search_btn_search);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();
            }
        });

        return rootView;
    }

    void adjustNumItemsCounterVisibility() {
        if (mListener.numItemsInTransaction() > 0) {
            mNumItems.setVisibility(View.VISIBLE);
            mNumItems.setText(String.format("%d items", mListener.numItemsInTransaction()));
        } else
            mNumItems.setVisibility(View.GONE);
    }

    void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getApplicationWindowToken(), 0);
    }

    List<CategoryTreeGenerator.CategoryNode> getCategories() {
        if (mCategoryNodes == null) {
            mCategoryNodes = CategoryTreeGenerator.createFlatCategoryTree(getContext());

            mCategoryNames = new String[mCategoryNodes.size()];
            int i = 0;

            for (CategoryTreeGenerator.CategoryNode elem : mCategoryNodes) {
                if (elem.category.category_id == CategoryEntry.ROOT_CATEGORY_ID) {
                    mCategoryNames[i++] = "--No Category--";
                } else {
                    String indent = "";
                    for (int j = 1; j < elem.node_depth; j++) {
                        indent += "\t\t\t";
                    }
                    mCategoryNames[i++] = indent + elem.category.name;
                }
            }
        }
        return mCategoryNodes;
    }

    List<SBranch> getBranches() {
        if (mBranches == null) {
            long company_id = PrefUtil.getCurrentCompanyId(getContext());

            String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";
            Cursor cursor = getActivity().getContentResolver().query(BranchEntry.buildBaseUri(company_id),
                    SBranch.BRANCH_COLUMNS, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                mBranches = new ArrayList<>();
                do {
                    SBranch branch = new SBranch(cursor);

                    // we don't want the current branch to be in the list of "transfer branches"
                    if (branch.branch_id != mBranchId) {
                        mBranches.add(branch);
                    }
                } while (cursor.moveToNext());
            }
        }
        return mBranches;
    }

    void displayQuantityDialog(SItem item) {
        FragmentManager fm = getActivity().getSupportFragmentManager();

        final TransDialog.QtyDialog dialog = TransDialog.newInstance(
                mIsBuyingTransaction);
        dialog.setItem(item);
        dialog.setBranches(getBranches());
        dialog.setListener(new TransDialog.TransQtyDialogListener() {
            @Override
            public void dialogCancel() {
                dialog.dismiss();
            }

            @Override
            public void dialogOk(STransaction.STransactionItem transactionItem) {
                dialog.dismiss();
                if (mListener != null)
                    mListener.transactionItemAdded(transactionItem);
                mFinish.setEnabled(true);
                adjustNumItemsCounterVisibility();
            }
        });
        dialog.show(fm, "Set Item Quantity");
    }

    @Override
    protected Loader<Cursor> onEmbeddedCreateLoader(int id, Bundle args) {
        String sortOrder = ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " ASC";

        if (mCurrSearch != null) mCurrSearch = mCurrSearch.trim();

        String selection = null;

        if (mCurrSearch != null && !mCurrSearch.isEmpty()) {
            selection = "(" + ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_BAR_CODE) + " LIKE '%" + mCurrSearch + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_NAME) + " LIKE '%" + mCurrSearch + "%' ) ";
        }

        /**
         * We either need to have selected a category from the category dialog
         * OR from the tree and it better not be the root category!!!
         */
        if ((mIsCategoryDialogSelected || super.isShowingCategoryTree())
                && (mSelectedCategoryId != CategoryEntry.ROOT_CATEGORY_ID)) {
            String and_clause = (selection == null) ? " " : " AND ";
            if (selection == null)
                selection = "";
            selection += and_clause + ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = " + mSelectedCategoryId;
        }

        return new CursorLoader(getActivity(),
                ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SItem.ITEM_COLUMNS,
                selection, null,
                sortOrder);
    }

    @Override
    protected void onEmbeddedLoadFinished(Loader<Cursor> loader, Cursor data) {
        mSearchAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mSearchList);
        setSearchStatus(!mSearchAdapter.isEmpty() || !getCategoryAdapter().isEmpty());
    }

    @Override
    protected void onEmbeddedLoadReset(Loader<Cursor> loader) {
        mSearchAdapter.swapCursor(null);
        ListUtils.setDynamicHeight(mSearchList);
        setSearchStatus(false);
    }

    void setSearchStatus(boolean found_items) {
        String str;
        if (found_items)
            str = "Search Results";
        else {
            str = "No Items Found";
            if (!TextUtils.isEmpty(mCurrSearch))
                str = String.format("No result found for: %s", mCurrSearch);
        }
        mResultLabel.setText(str);
    }

    public interface SearchResultListener {
        void transactionItemAdded(STransaction.STransactionItem transactionItem);

        void finishTransaction();

        void cancelTransaction();

        int numItemsInTransaction();
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
                code = item.item_code;
            }
            holder.item_code.setText(code);
        }
    }
}
