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
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.items.transactions.TransactionUtil;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.SheketTextUtils;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by gamma on 3/27/16.
 */
public class BranchItemFragment extends SearchableItemFragment {
    private static final String KEY_BRANCH = "key_branch";

    private long mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
    private SBranch mBranch;

    private FloatingActionButton mFinishTransactionBtn;

    private List<STransactionItem> mTransactionItemList;

    private static final String KEY_SAVE_VIEW_ALL_ITEMS = "key_save_all_items";
    private boolean mShowAllItems = false;

    public static BranchItemFragment newInstance(SBranch branch) {
        Bundle args = new Bundle();

        BranchItemFragment fragment = new BranchItemFragment();
        args.putParcelable(KEY_BRANCH, branch);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mShowAllItems = savedInstanceState.getBoolean(KEY_SAVE_VIEW_ALL_ITEMS, false);
        }
        Bundle args = getArguments();
        mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;

        mBranch = args.getParcelable(KEY_BRANCH);

        mTransactionItemList = new ArrayList<>();

        setCurrentCategory(mCategoryId);

        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SAVE_VIEW_ALL_ITEMS, mShowAllItems);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.branch_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_item_branch_item_list_all_items);
        if (SPermission.getSingletonPermission().getPermissionType() !=
                SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            menuItem.setVisible(false);
        } else {
            menuItem.setVisible(true);
            menuItem.setIcon(
                    getActivity().getResources().getDrawable(mShowAllItems ?
                            R.drawable.ic_action_eye_open :
                            R.drawable.ic_action_eye_closed));
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_branch_item_list_all_items:
                mShowAllItems = !mShowAllItems;
                // This will force the menu to be redrawn, updating the UI
                getActivity().invalidateOptionsMenu();

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
    void updateFloatingActionBtnStatus() {
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
        updateFloatingActionBtnStatus();

        getActivity().setTitle(Utils.toTitleCase(mBranch.branch_name));

        return rootView;
    }

    private static class BranchItemViewHolder {
        TextView item_name;
        TextView item_code;
        TextView qty_remain;
        TextView item_loc;
        ImageButton edit_loc;
        View layout_edit_loc;
        View layout_branch_quantity;
        View name_padding_if_not_exist_in_branch;

        ImageView item_not_exist;

        public BranchItemViewHolder(View view) {
            item_name = (TextView) view.findViewById(R.id.list_item_b_item_text_view_item_name);
            item_code = (TextView) view.findViewById(R.id.list_item_b_item_text_view_item_code);
            qty_remain = (TextView) view.findViewById(R.id.list_item_b_item_text_view_item_qty);
            item_loc = (TextView) view.findViewById(R.id.list_item_b_item_text_view_item_loc);
            edit_loc = (ImageButton) view.findViewById(R.id.list_item_b_item_img_btn_edit_location);
            layout_edit_loc = view.findViewById(R.id.list_item_b_item_layout_edit_location);
            layout_branch_quantity = view.findViewById(R.id.list_item_b_item_layout_quantity);
            name_padding_if_not_exist_in_branch = view.findViewById(R.id.list_item_b_item_padding_if_not_exist_in_branch);
            item_not_exist = (ImageView) view.findViewById(R.id.list_item_b_item_img_view_item_not_exist);
        }
    }


    @Override
    public View newItemView(Context context, ViewGroup parent, Cursor cursor, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_branch_item, parent, false);
        BranchItemViewHolder holder = new BranchItemViewHolder(view);

        view.setTag(holder);
        return view;
    }


    @Override
    public void bindItemView(Context context, Cursor cursor, View view, int position) {
        BranchItemViewHolder holder = (BranchItemViewHolder) view.getTag();
        final SBranchItem branchItem = new SBranchItem(cursor, true);
        final SItem item = branchItem.item;

        if (isSearching()) {
            SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_name, item.name, getSearchText());
            SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_code, item.item_code, getSearchText());
        } else {
            holder.item_name.setText(item.name);
            holder.item_code.setText(item.item_code);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTransactionQuantityDialog(item, false, -1,
                        false, null);
            }
        });
        if (branchItem.branch_id == SBranchItem.NO_BRANCH_ITEM_FOUND ||
                branchItem.item_location == null ||
                branchItem.item_location.isEmpty()) {
            holder.item_loc.setVisibility(View.GONE);
        } else {
            holder.item_loc.setVisibility(View.VISIBLE);
            holder.item_loc.setText(branchItem.item_location);
        }

        boolean item_exist = branchItem.branch_id != SBranchItem.NO_BRANCH_ITEM_FOUND;

        int show_if_exist = item_exist ? View.VISIBLE : View.GONE;
        int show_if_not_exist = item_exist ? View.GONE : View.VISIBLE;

        holder.layout_branch_quantity.setVisibility(show_if_exist);
        holder.edit_loc.setVisibility(show_if_exist);
        holder.layout_edit_loc.setVisibility(show_if_exist);

        holder.item_not_exist.setVisibility(show_if_not_exist);
        holder.name_padding_if_not_exist_in_branch.setVisibility(show_if_not_exist);

        if (item_exist) {
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayItemLocationDialog(branchItem);
                }
            };
            holder.edit_loc.setOnClickListener(listener);
            holder.layout_edit_loc.setOnClickListener(listener);

            holder.qty_remain.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
        }
    }

    @Override
    protected boolean onEntitySelected(Cursor cursor) {
        return true;
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
                                updateFloatingActionBtnStatus();
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
                updateFloatingActionBtnStatus();
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
                        updateFloatingActionBtnStatus();
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
                    TransactionUtil.createTransactionWithItems(getActivity(), itemList, mBranch.branch_id, transactionNote);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTransactionItemList.clear();
                        updateFloatingActionBtnStatus();
                        progress.dismiss();

                        // TODO: fix this bug
                        // Fixme: without this, the category list above the item list disappears,
                        restartLoader();
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
        QuantityDialog dialog = QuantityDialog.newInstance(
                item,
                mBranch.branch_id,
                new QuantityDialog.QuantityListener() {
                    @Override
                    public void dialogCancel(DialogFragment dialog) {
                        dialog.dismiss();
                    }

                    @Override
                    public void dialogOkContinue(DialogFragment dialog, STransactionItem transItem) {
                        dialog.dismiss();
                        if (!is_editing) {
                            mTransactionItemList.add(transItem);
                        } else {
                            mTransactionItemList.remove(edit_position);
                            mTransactionItemList.add(edit_position, transItem);
                        }

                        updateFloatingActionBtnStatus();
                    }

                    @Override
                    void dialogOkFinish(DialogFragment dialog, STransactionItem transItem) {
                        dialog.dismiss();
                        if (!is_editing) {
                            mTransactionItemList.add(transItem);
                        } else {
                            mTransactionItemList.remove(edit_position);
                            mTransactionItemList.add(edit_position, transItem);
                        }

                        /**
                         * If there is only 1 item, commit the transaction right away.
                         * There will not be any "transaction-note" since there is only 1 item
                         * in it. The transaction note is designed so multiple items can share
                         * some common "reminder" about their transaction. For only a single item,
                         * item note will suffice.
                         */
                        if (mTransactionItemList.size() == 1) {
                            commitTransaction(mTransactionItemList, "");
                        } else {
                            // don't forget to the floating btn with the added item count
                            updateFloatingActionBtnStatus();

                            displaySummaryDialog();
                        }
                    }
                }
        );
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
    protected boolean onSearchTextChanged(String newText) {
        restartLoader();
        return true;
    }

    @Override
    protected boolean onSearchTextViewClosed() {
        restartLoader();
        return true;
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
    public void onCategorySelected(long previous_category, long selected_category) {
        mCategoryId = selected_category;
    }

    /**
     * We've overloaded it because we should only show categories that have items inside the branch.
     * The "base-class's" implementation shows all categories.
     */
    @Override
    protected Loader<Cursor> getCategoryTreeLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._fullCurrent(CategoryEntry.COLUMN_NAME) + " COLLATE NOCASE ASC";

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
            Uri uri = BranchCategoryEntry.buildBranchCategoryUri(PrefUtil.getCurrentCompanyId(getContext()),
                    // The NO_ID_SET is so we fetch ALL branch categories, not just a single one
                    // with a particular id.
                    mBranch.branch_id, BranchCategoryEntry.NO_ID_SET);

            // also include children categories in the result
            uri = BranchCategoryEntry.buildFetchCategoryWithChildrenUri(uri);

            String selection = CategoryEntry._fullCurrent(CategoryEntry.COLUMN_PARENT_ID) + " = ? AND " +
                    // we don't want the deleted to appear(until they are totally removed when syncing)
                    CategoryEntry._fullCurrent(ChangeTraceable.COLUMN_CHANGE_INDICATOR) + " != ? AND " +
                    BranchCategoryEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR) + " != ?";

            String[] selectionArgs = new String[]{
                    String.valueOf(mCurrentCategoryId),
                    String.valueOf(ChangeTraceable.CHANGE_STATUS_DELETED),
                    String.valueOf(ChangeTraceable.CHANGE_STATUS_DELETED)
            };

            return new CursorLoader(getActivity(),
                    uri,
                    SCategory.CATEGORY_WITH_CHILDREN_COLUMNS,
                    selection,
                    selectionArgs,
                    sortOrder);
        }
    }

    @Override
    protected Loader<Cursor> onEntityCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getContext());

        String selection = null;
        String[] selectionArgs = null;

        if (super.isSearching()) {
            String search_text = super.getSearchText();
            selection = "(" + ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " LIKE '%" + search_text + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_NAME) + " LIKE '%" + search_text + "%' ) ";
        } else if (super.isShowingCategoryTree()) {
            selection = ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?";
            selectionArgs = new String[]{String.valueOf(mCategoryId)};
        }

        Uri uri = BranchItemEntry.buildAllItemsInBranchUri(company_id, mBranch.branch_id);
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
    protected boolean shouldShowCategoryNavigation() {
        return !super.isSearching();
    }

    @Override
    protected void onEntityLoaderFinished(Loader<Cursor> loader, Cursor data) {
        setEntityCursor(data);
    }

    @Override
    protected void onEntityLoaderReset(Loader<Cursor> loader) {
        setEntityCursor(null);
    }
}
