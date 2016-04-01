package com.mukera.sheket.client.controller;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.item_searcher.ItemSearchResultListener;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.controller.item_searcher.ManualSearchFragment;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.*;

import java.util.ArrayList;
import java.util.List;

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

    public SItem mPassedInItem;
    private List<STransactionItem> mTransactionItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        mCurrentLaunch = getIntent().getIntExtra(LAUNCH_ACTION_KEY, LAUNCH_TYPE_NONE);

        String title = sTitles[mCurrentLaunch - 1];
        setTitle(title);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        if (savedInstanceState == null) {
            mTransactionItemList = new ArrayList<>();

            mBranchId = getIntent().getLongExtra(LAUNCH_BRANCH_ID_KEY, BRANCH_ID_NONE);

            displayItemSearcher();
        }
    }

    public static boolean isBranchSpecified(long branch_id) {
        return branch_id != BRANCH_ID_NONE;
    }

    private void startFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_action_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    void displayQuantityDialog(SItem item) {
        FragmentManager fm = getSupportFragmentManager();
        final QtyDialog dialog = new QtyDialog();
        dialog.setListener(new QtyDialog.ItemQtyListener() {
            @Override
            public void dialogCancel() {
                dialog.dismiss();
            }

            @Override
            public void dialogOk(SItem item, double qty) {
                dialog.dismiss();
                addItemToTransactionList(item, qty);
            }
        });
        dialog.item = item;
        dialog.show(fm, "Quantity");
    }

    void addItemToTransactionList(SItem item, double qty) {
        STransactionItem sitem = new STransactionItem();
        sitem.item = item;
        sitem.quantity = qty;
        mTransactionItemList.add(sitem);
    }

    void displayItemSearcher() {
        ManualSearchFragment manual = ManualSearchFragment.newInstance(mBranchId);
        final Activity activity = this;
        manual.setInputFragmentListener(new ItemSearchResultListener() {
            @Override
            public void itemSelected(SItem item) {
                displayQuantityDialog(item);
            }

            @Override
            public void finishTransaction() {
                SummaryFragment fragment = new SummaryFragment();
                fragment.setListener(new SummaryFragment.SummaryListener() {
                    @Override
                    public void cancelSelected() {
                        activity.finish();
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

                });
                fragment.mItems = mTransactionItemList;
                startFragment(fragment);
            }

            @Override
            public void cancelTransaction() {
                TransactionActivity.this.finish();
            }
        });
        startFragment(manual);
    }

    void createTransactionWithItems(Activity context, List<STransactionItem> itemList) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_BRANCH_ID, mBranchId);
        values.put(TransactionEntry.COLUMN_USER_ID, 1);

        operations.add(
                ContentProviderOperation.newInsert(TransactionEntry.CONTENT_URI).
                withValues(values).build());
        for (STransactionItem item : itemList) {
            operations.add(
                    ContentProviderOperation.newInsert(TransItemEntry.CONTENT_URI).
                            withValues(item.toContentValues()).
                            withValueBackReference(TransItemEntry.COLUMN_TRANSACTION_ID, 0).
                            build());
        }

        try {
            context.getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            // some error handling
        } catch (OperationApplicationException e) {
            // some error handling
        }
    }
}
