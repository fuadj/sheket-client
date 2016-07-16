package com.mukera.sheket.client.controller.items;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.items.transactions.TransactionUtil;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by gamma on 3/27/16.
 */
public class BranchItemFragment extends CategoryTreeNavigationFragment {
    private static final String KEY_BRANCH_ID = "key_branch_id";

    private long mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
    private long mBranchId;

    private ListView mBranchItemList;
    private BranchItemCursorAdapter mBranchItemAdapter;

    private FloatingActionButton mFinishTransactionBtn;

    private List<STransactionItem> mTransactionItemList;
    private List<SBranch> mBranches = null;

    private static final String KEY_SAVE_ALL_ITEMS = "key_save_all_items";
    private boolean mShowAllItems = false;

    public static BranchItemFragment newInstance(long branch_id) {
        Bundle args = new Bundle();

        BranchItemFragment fragment = new BranchItemFragment();
        args.putLong(KEY_BRANCH_ID, branch_id);
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * Lazily fetch the branches
     *
     * @return
     */
    List<SBranch> getBranches() {
        if (mBranches == null) {
            long company_id = PrefUtil.getCurrentCompanyId(getActivity());

            String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";
            Cursor cursor = getActivity().getContentResolver().
                    query(BranchEntry.buildBaseUri(company_id),
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mShowAllItems = savedInstanceState.getBoolean(KEY_SAVE_ALL_ITEMS, false);
        }
        Bundle args = getArguments();
        mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
        mBranchId = args.getLong(KEY_BRANCH_ID);

        mTransactionItemList = new ArrayList<>();

        setCurrentCategory(mCategoryId);

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SAVE_ALL_ITEMS, mShowAllItems);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.branch_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_item_branch_item_list_all_items).
                setIcon(
                        getActivity().getResources().getDrawable(mShowAllItems ?
                                R.drawable.ic_action_eye_open :
                                R.drawable.ic_action_eye_closed));
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_branch_item_list_all_items:
                mShowAllItems = !mShowAllItems;
                // request the loader to be restarted
                restartLoader();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets visible the finish transaction floating button if
     * there is at least 1 item in the transaction.
     */
    void updateFinishBtnVisibility() {
        mFinishTransactionBtn.setVisibility(
                mTransactionItemList.isEmpty() ? View.GONE : View.VISIBLE
        );
        if (!mTransactionItemList.isEmpty()) {
            final float TEXT_SIZE = 48.f;
            mFinishTransactionBtn.setImageDrawable(
                    new TextDrawable(getContext(),
                            String.format(Locale.US, "%d", mTransactionItemList.size()),
                            ColorStateList.valueOf(Color.WHITE), TEXT_SIZE, TextDrawable.VerticalAlignment.BASELINE)
            );
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_branch_item;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // the base class uses {@code getLayoutResId} to inflate the resource
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mFinishTransactionBtn = (FloatingActionButton) rootView.findViewById(R.id.float_btn_branch_item_transaction);
        mFinishTransactionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySummaryDialog();
            }
        });
        updateFinishBtnVisibility();

        mBranchItemList = (ListView) rootView.findViewById(R.id.branch_item_list_view_items);
        mBranchItemAdapter = new BranchItemCursorAdapter(getActivity());
        mBranchItemAdapter.setListener(new BranchItemCursorAdapter.ItemSelectionListener() {
            @Override
            public void editItemLocationSelected(final SBranchItem branchItem) {
                displayItemLocationDialog(branchItem);
            }

            @Override
            public void itemSelected(SItem item) {
                displayTransactionQuantityDialog(item, false, -1,
                        false, null);
            }
        });
        mBranchItemList.setAdapter(mBranchItemAdapter);

        return rootView;
    }

    void displaySummaryDialog() {
        TransactionSummaryDialog dialog = new TransactionSummaryDialog();
        dialog.setTransactionItems(mTransactionItemList);
        dialog.setListener(new TransactionSummaryDialog.SummaryListener() {
            @Override
            public void cancelSelected(final DialogFragment dialog) {
                new AlertDialog.Builder(getActivity()).
                        setTitle("Quit Transaction?").
                        setMessage("Are You Sure?").
                        setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface quitDialog, int which) {
                                quitDialog.dismiss();
                                dialog.dismiss();
                                mTransactionItemList.clear();
                                updateFinishBtnVisibility();
                            }
                        }).setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface quitDialog, int which) {
                                quitDialog.dismiss();
                            }
                        }).show();
            }

            @Override
            public void backSelected(DialogFragment dialog) {
                dialog.dismiss();
            }

            @Override
            public void editItemAtPosition(DialogFragment trans_dialog,
                                           List<STransactionItem> itemList, int position) {
                SItem item = itemList.get(position).item;

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().
                        beginTransaction();

                // we are removing the dialog and adding the transaction to
                // the back-stack so we can get rollback the transaction when the quantity
                // dialog returns
                transaction.remove(trans_dialog);
                transaction.addToBackStack(null);

                displayTransactionQuantityDialog(item, true, position,
                        true, transaction);
            }

            @Override
            public void deleteItemAtPosition(DialogFragment dialog, List<STransactionItem> itemList, int position) {
                itemList.remove(position);
                if (itemList.isEmpty()) {
                    dialog.dismiss();
                } else {
                    ((TransactionSummaryDialog) dialog).refreshSummaryDialog();
                }
                updateFinishBtnVisibility();
            }

            @Override
            public void okSelected(DialogFragment dialog, List<STransactionItem> list) {
                displayTransactionNoteDialog((TransactionSummaryDialog) dialog, list);
            }
        });
        dialog.show(getActivity().getSupportFragmentManager(), null);
    }

    void displayTransactionNoteDialog(final TransactionSummaryDialog summaryDialog,
                                      final List<STransactionItem> itemList) {
        final EditText editText = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add a Reminder?")
                .setMessage("(Optional) Write a reminder to remember the transaction").
                setView(editText).
                setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        summaryDialog.dismiss();
                        commitTransaction(itemList, editText.getText().toString().trim());
                    }
                }).setNegativeButton("Back",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateFinishBtnVisibility();
                        dialog.dismiss();
                    }
                }).setNeutralButton("No Reminder",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        summaryDialog.dismiss();
                        commitTransaction(itemList, "");
                    }
                }).setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        editText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
                        !TextUtils.isEmpty(s.toString().trim()));
            }
        });
        dialog.show();
    }

    void commitTransaction(final List<STransactionItem> itemList, final String transactionNote) {
        final ProgressDialog progress = ProgressDialog.show(
                getActivity(), "Saving", "Please Wait...", true);
        Thread t = new Thread() {
            @Override
            public void run() {
                if (!itemList.isEmpty())
                    TransactionUtil.createTransactionWithItems(getActivity(), itemList, mBranchId, transactionNote);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTransactionItemList.clear();
                        updateFinishBtnVisibility();
                        progress.dismiss();
                    }
                });
            }
        };
        t.start();
    }


    /**
     * Displays a quantity selection dialog for the item. It supports
     * this operation for both new items and editing items already in the transaction.
     * <p/>
     * This dialog can be run in a transaction if specified.
     *
     * @param item
     * @param is_editing
     * @param edit_position
     * @param run_in_transaction
     * @param transaction
     */
    void displayTransactionQuantityDialog(SItem item,
                                          final boolean is_editing,
                                          final int edit_position,
                                          boolean run_in_transaction,
                                          FragmentTransaction transaction) {
        QuantityDialog dialog = new QuantityDialog();

        dialog.setBranches(getBranches());
        dialog.setItem(item);
        dialog.setListener(new QuantityDialog.DialogListener() {
            @Override
            public void dialogCancel(DialogFragment dialog) {
                dialog.dismiss();
            }

            @Override
            public void dialogOk(DialogFragment dialog, STransactionItem transItem) {
                dialog.dismiss();
                if (!is_editing) {
                    mTransactionItemList.add(transItem);
                } else {
                    mTransactionItemList.remove(edit_position);
                    mTransactionItemList.add(edit_position, transItem);
                }
                updateFinishBtnVisibility();
            }
        });
        if (run_in_transaction) {
            dialog.show(transaction, null);
        } else {
            dialog.show(getActivity().getSupportFragmentManager(), null);
        }
    }

    void displayItemLocationDialog(final SBranchItem branchItem) {
        final EditText editText = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setTitle("Set Item Location").
                setMessage(branchItem.item.name).
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String location = editText.getText().toString().trim();

                        // the text didn't change, ignore it
                        if (TextUtils.equals(branchItem.item_location, location)) {
                            dialog.dismiss();
                            return;
                        }

                        setItemLocation(dialog, branchItem, location);
                    }
                }).setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false);

        final AlertDialog dialog = builder.create();

        if (!TextUtils.isEmpty(branchItem.item_location))
            editText.setText(branchItem.item_location);

        dialog.show();
    }

    void setItemLocation(final DialogInterface dialog,
                         final SBranchItem branchItem,
                         final String location) {
        Thread t = new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(BranchItemEntry.COLUMN_ITEM_LOCATION, location);

                /**
                 * If the branch item was in created state, we don't want to change it until
                 * we sync with server. If we change it to updated state, it will create problems
                 * as the server still doesn't know about it and the update will fail.
                 */
                if (branchItem.change_status != ChangeTraceable.CHANGE_STATUS_CREATED) {
                    values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_UPDATED);
                }
                getContext().getContentResolver().update(
                        BranchItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                        values,
                        String.format("%s = ? AND %s = ?",
                                BranchItemEntry.COLUMN_BRANCH_ID, BranchItemEntry.COLUMN_ITEM_ID),
                        new String[]{
                                String.valueOf(branchItem.branch_id),
                                String.valueOf(branchItem.item_id)
                        }
                );
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        };
        t.start();
    }

    @Override
    protected void onCategoryTreeViewToggled(boolean show_tree_view) {
    }

    @Override
    protected int getCategoryLoaderId() {
        return LoaderId.MainActivity.BRANCH_ITEM_CATEGORY_LOADER;
    }

    @Override
    public void onInitLoader() {
        getLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_ITEM_LOADER, null, this);
    }

    @Override
    public void onRestartLoader() {
        getLoaderManager().restartLoader(LoaderId.MainActivity.BRANCH_ITEM_LOADER, null, this);
    }

    @Override
    public void onCategorySelected(long category_id) {
        mCategoryId = category_id;
    }

    @Override
    protected Loader<Cursor> getCategoryTreeLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._fullCurrent(CategoryEntry.COLUMN_NAME) + " ASC";

        if (mShowAllItems) {
            // the super class's implementation loads ALL categories, not just the ones
            // that exist inside the branch
            return super.getCategoryTreeLoader(id, args);
        } else {
            /**
             * NOTE: we are able to use the projection of SCategory because the
             * the result is a join between Branch and Category with each column
             * being fully qualified(i.e: saying from which table it came from)
             * so it doesn't create any problems.
             *
             * We are fetching the children so we want to use the CategoryTreeNavigation fragment's
             * UI to display the number of children sub-categories.
             */
            return new CursorLoader(getActivity(),
                    BranchCategoryEntry.buildBranchCategoryUri(PrefUtil.getCurrentCompanyId(getContext()),
                            // The NO_ID_SET is so we fetch ALL branch categories, not just a single one
                            // with a particular id.
                            mBranchId, BranchCategoryEntry.NO_ID_SET),
                    SCategory.CATEGORY_WITH_CHILDREN_COLUMNS,
                    CategoryEntry._fullCurrent(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                    new String[]{String.valueOf(mCurrentCategoryId)},
                    sortOrder);
        }
    }

    @Override
    protected Loader<Cursor> onCategoryTreeCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getContext());

        String selection = null;
        String[] selectionArgs = null;

        if (super.isShowingCategoryTree()) {
            selection = ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?";
            selectionArgs = new String[]{String.valueOf(mCategoryId)};
        }

        Uri uri = BranchItemEntry.buildAllItemsInBranchUri(company_id, mBranchId);
        if (mShowAllItems)
            uri = BranchItemEntry.buildFetchNoneExistingItemsUri(uri);
        return new CursorLoader(getActivity(),
                uri,
                SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                selection,
                selectionArgs,
                ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " ASC"
        );
    }

    @Override
    protected void onCategoryTreeLoaderFinished(Loader<Cursor> loader, Cursor data) {
        mBranchItemAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mBranchItemList);
    }

    @Override
    protected void onCategoryTreeLoaderReset(Loader<Cursor> loader) {
        mBranchItemAdapter.swapCursor(null);
        ListUtils.setDynamicHeight(mBranchItemList);
    }

    public static class BranchItemCursorAdapter extends android.support.v4.widget.CursorAdapter {
        public interface ItemSelectionListener {
            void itemSelected(SItem item);

            void editItemLocationSelected(SBranchItem branchItem);
        }

        private static class ViewHolder {
            TextView item_name;
            TextView item_code;
            TextView qty_remain;
            TextView item_loc;
            ImageButton edit_loc;
            LinearLayout layout_branch_item;
            ImageView item_not_exist;

            public ViewHolder(View view) {
                item_name = (TextView) view.findViewById(R.id.list_item_text_view_b_item_name);
                item_code = (TextView) view.findViewById(R.id.list_item_text_view_b_item_code);
                qty_remain = (TextView) view.findViewById(R.id.list_item_text_view_b_item_qty);
                item_loc = (TextView) view.findViewById(R.id.list_item_text_view_b_item_loc);
                edit_loc = (ImageButton) view.findViewById(R.id.list_item_img_btn_b_edit_location);
                layout_branch_item = (LinearLayout) view.findViewById(R.id.layout_branch_item_section);
                item_not_exist = (ImageView) view.findViewById(R.id.list_item_img_view_b_item_not_exist);
            }
        }

        public ItemSelectionListener mListener;

        public void setListener(ItemSelectionListener listener) {
            mListener = listener;
        }

        public BranchItemCursorAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_branch_item, parent, false);
            ViewHolder holder = new ViewHolder(view);

            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            final SBranchItem branchItem = new SBranchItem(cursor, true);
            SItem item = branchItem.item;

            holder.item_name.setText(item.name);
            String code;
            if (item.has_bar_code) {
                code = item.bar_code;
            } else {
                code = item.item_code;
            }
            holder.item_code.setText(code);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.itemSelected(branchItem.item);
                }
            });
            if (branchItem.branch_id == SBranchItem.ITEM_NOT_FOUND_IN_BRANCH ||
                    branchItem.item_location == null ||
                    branchItem.item_location.isEmpty()) {
                holder.item_loc.setVisibility(View.GONE);
            } else {
                holder.item_loc.setVisibility(View.VISIBLE);
                holder.item_loc.setText(branchItem.item_location);
            }

            boolean item_exist = branchItem.branch_id != SBranchItem.ITEM_NOT_FOUND_IN_BRANCH;

            int show_if_exist = item_exist ? View.VISIBLE : View.GONE;
            int show_if_not_exist = item_exist ? View.GONE : View.VISIBLE;

            holder.layout_branch_item.setVisibility(show_if_exist);
            holder.edit_loc.setVisibility(show_if_exist);

            holder.item_not_exist.setVisibility(show_if_not_exist);

            if (item_exist) {
                holder.edit_loc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.editItemLocationSelected(branchItem);
                    }
                });
                holder.qty_remain.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
            }
        }
    }
}
