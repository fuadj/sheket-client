package com.mukera.sheket.client.controller.transactions;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.*;
import com.mukera.sheket.client.utility.DbUtil;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TransactionActivity extends AppCompatActivity {
    public static final String LAUNCH_ACTION_KEY = "launch_action_key";
    public static final String LAUNCH_ITEM_ID_KEY = "launch_item_id_key";
    public static final String LAUNCH_BRANCH_ID_KEY = "launch_branch_id_key";

    public static final long ITEM_ID_NONE = -1;
    public static final long BRANCH_ID_NONE = -1;

    private static final int LAUNCH_TYPE_NONE = 0;
    public static final int LAUNCH_TYPE_BUY = 1;
    public static final int LAUNCH_TYPE_SELL = 2;

    private static final String[] sTitles = {"Buy", "Sell"};

    private int mCurrentLaunch = LAUNCH_TYPE_NONE;
    private long mBranchId = BRANCH_ID_NONE;

    private List<STransactionItem> mTransactionItemList;
    private List<SBranch> mBranches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        mCurrentLaunch = getIntent().getIntExtra(LAUNCH_ACTION_KEY, LAUNCH_TYPE_NONE);

        String title = sTitles[mCurrentLaunch - 1];
        setTitle(title);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        if (savedInstanceState == null) {
            mTransactionItemList = new ArrayList<>();

            mBranchId = getIntent().getLongExtra(LAUNCH_BRANCH_ID_KEY, BRANCH_ID_NONE);

            displayItemSearcher();
        }
    }

    interface DialogDismissListener {
        void dialogDismissed();
    }

    List<SBranch> getBranches() {
        if (mBranches == null) {
            long company_id = PrefUtil.getCurrentCompanyId(this);

            String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";
            Cursor cursor = getContentResolver().query(BranchEntry.buildBaseUri(company_id),
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

    void displayQuantityDialog(SItem item, final boolean is_editing, final int edit_position,
                               final DialogDismissListener listener) {
        FragmentManager fm = getSupportFragmentManager();

        final TransDialog.QtyDialog dialog = TransDialog.newInstance(mCurrentLaunch == LAUNCH_TYPE_BUY);

        dialog.setItem(item);
        dialog.setBranches(getBranches());

        dialog.setListener(new TransDialog.TransQtyDialogListener() {
            @Override
            public void dialogOk(STransactionItem transactionItem) {
                dialog.dismiss();

                if (!is_editing) {
                    mTransactionItemList.add(transactionItem);
                } else {
                    mTransactionItemList.set(edit_position, transactionItem);
                }
                if (listener != null)
                    listener.dialogDismissed();
            }

            @Override
            public void dialogCancel() {
                dialog.dismiss();
                if (listener != null)
                    listener.dialogDismissed();
            }

        });
        dialog.show(fm, "Quantity");
    }

    void displayItemSearcher() {
        ItemSearchFragment fragment = ItemSearchFragment.newInstance(mBranchId);
        final AppCompatActivity activity = this;
        final String SEARCH_FRAGMENT_TAG = "search_fragment_tag";
        fragment.setResultListener(new ItemSearchFragment.ItemSearchResultListener() {
            @Override
            public void itemSelected(SItem item) {
                displayQuantityDialog(item, false, 0, null);
            }

            @Override
            public void finishTransaction() {
                final SummaryFragment summaryFragment = new SummaryFragment();
                summaryFragment.setListener(new SummaryFragment.SummaryListener() {
                    @Override
                    public void cancelSelected() {
                        activity.finish();
                    }

                    @Override
                    public void backSelected() {
                        activity.getSupportFragmentManager().popBackStack(SEARCH_FRAGMENT_TAG, 0);
                    }

                    @Override
                    public void okSelected(final List<STransactionItem> itemList) {
                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                createTransactionWithItems(activity, itemList);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.finish();
                                    }
                                });
                            }
                        };
                        t.start();
                    }

                    @Override
                    public void editItemAtPosition(List<STransactionItem> itemList, int position) {
                        STransactionItem tranItem = itemList.get(position);
                        SItem item = tranItem.item;
                        DialogDismissListener listener = new DialogDismissListener() {
                            @Override
                            public void dialogDismissed() {
                                summaryFragment.refreshAdapter();
                            }
                        };
                        displayQuantityDialog(item, true, position, listener);
                    }

                    @Override
                    public void deleteItemAtPosition(List<STransactionItem> itemList, int position) {
                        mTransactionItemList.remove(position);
                    }
                });

                summaryFragment.mItemList = mTransactionItemList;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.transaction_action_container, summaryFragment)
                        .addToBackStack(SEARCH_FRAGMENT_TAG)
                        .commit();
            }

            @Override
            public void cancelTransaction() {
                TransactionActivity.this.finish();
            }
        });
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.transaction_action_container, fragment)
                .commit();
    }

    void createTransactionWithItems(Activity context, List<STransactionItem> itemList) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        long new_trans_id = PrefUtil.getNewTransId(this);

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_TRANS_ID, new_trans_id);
        values.put(TransactionEntry.COLUMN_BRANCH_ID, mBranchId);
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_CREATED);
        values.put(TransactionEntry.COLUMN_COMPANY_ID, PrefUtil.getCurrentCompanyId(this));
        values.put(TransactionEntry.COLUMN_USER_ID, PrefUtil.getUserId(this));

        values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());

        // TODO: figure out a better way to store time
        values.put(TransactionEntry.COLUMN_DATE, System.currentTimeMillis());

        long company_id = PrefUtil.getCurrentCompanyId(this);
        operations.add(
                ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(company_id)).
                        withValues(values).build());
        for (STransactionItem item : itemList) {
            operations.add(
                    ContentProviderOperation.newInsert(TransItemEntry.buildBaseUri(company_id)).
                            withValues(item.toContentValues()).
                            withValueBackReference(TransItemEntry.COLUMN_TRANSACTION_ID, 0).
                            build());
        }

        try {
            context.getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operations);

            // if all goes well, update the local transaction counter
            PrefUtil.setNewTransId(this, new_trans_id);

            updateBranchItemsInTransactions(itemList);

        } catch (RemoteException e) {
            // some error handling
        } catch (OperationApplicationException e) {
            // some error handling
        }
    }

    class KeyBranchItem {
        long branch_id, item_id;

        public KeyBranchItem(long b_id, long i_id) {
            branch_id = b_id;
            item_id = i_id;
        }
    }

    SBranchItem getBranchItem(HashMap<KeyBranchItem, SBranchItem> seenBranchItems,
                              long branch_id, long item_id) {
        KeyBranchItem key = new KeyBranchItem(branch_id, item_id);

        if (seenBranchItems.containsKey(key)) {
            return seenBranchItems.get(key);
        }

        long company_id = PrefUtil.getCurrentCompanyId(this);

        Cursor cursor = getContentResolver().
                query(BranchItemEntry.buildBranchItemUri(company_id, branch_id, item_id),
                        SBranchItem.BRANCH_ITEM_COLUMNS,
                        null, null, null);
        SBranchItem item;
        if (cursor != null && cursor.moveToFirst()) {
            item = new SBranchItem(cursor);

            // we haven't still synced the 'created' item, so don't change flag to update
            if (item.change_status != ChangeTraceable.CHANGE_STATUS_CREATED)
                item.change_status = ChangeTraceable.CHANGE_STATUS_UPDATED;

            cursor.close();
        } else {
            // the item doesn't exist in the branch, create a new item
            item = new SBranchItem();
            item.company_id = company_id;
            item.branch_id = branch_id;
            item.item_id = item_id;
            item.quantity = 0;
            item.change_status = ChangeTraceable.CHANGE_STATUS_CREATED;
        }

        seenBranchItems.put(key, item);
        return item;
    }

    void setBranchItem(HashMap<KeyBranchItem, SBranchItem> seenBranchItems,
                       SBranchItem branchItem) {
        long company_id = PrefUtil.getCurrentCompanyId(this);

        getContentResolver().insert(BranchItemEntry.buildBaseUri(company_id),
                DbUtil.setUpdateOnConflict(branchItem.toContentValues()));

        // update the cache
        seenBranchItems.put(new KeyBranchItem(branchItem.branch_id, branchItem.item_id),
                branchItem);
    }

    void updateBranchItemsInTransactions(List<STransactionItem> transItemList) {
        HashMap<KeyBranchItem, SBranchItem> seenBranchItems = new HashMap<>();

        for (STransactionItem transItem : transItemList) {
            switch (transItem.trans_type) {
                case TransItemEntry.TYPE_INCREASE_PURCHASE:
                case TransItemEntry.TYPE_INCREASE_RETURN_ITEM: {
                    SBranchItem branchItem = getBranchItem(seenBranchItems, mBranchId, transItem.item_id);
                    branchItem.quantity = branchItem.quantity + transItem.quantity;
                    setBranchItem(seenBranchItems, branchItem);
                    // increase branch item
                    break;
                }

                case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER: {
                    long source_branch, dest_branch;
                    if (transItem.trans_type == TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH) {
                        source_branch = transItem.other_branch_id;
                        dest_branch = mBranchId;
                    } else {
                        source_branch = mBranchId;
                        dest_branch = transItem.other_branch_id;
                    }
                    SBranchItem sourceItem = getBranchItem(seenBranchItems, source_branch, transItem.item_id);
                    SBranchItem destItem = getBranchItem(seenBranchItems, dest_branch, transItem.item_id);

                    sourceItem.quantity = sourceItem.quantity - transItem.quantity;
                    destItem.quantity = destItem.quantity + transItem.quantity;

                    setBranchItem(seenBranchItems, sourceItem);
                    setBranchItem(seenBranchItems, destItem);

                    break;
                }

                case TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH: {
                    SBranchItem branchItem = getBranchItem(seenBranchItems, mBranchId, transItem.item_id);
                    branchItem.quantity = branchItem.quantity - transItem.quantity;
                    setBranchItem(seenBranchItems, branchItem);
                }
            }
        }
    }
}
