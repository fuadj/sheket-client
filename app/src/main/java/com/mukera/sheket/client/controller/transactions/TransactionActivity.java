package com.mukera.sheket.client.controller.transactions;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.*;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.Date;
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
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mCurrentLaunch = getIntent().getIntExtra(LAUNCH_ACTION_KEY, LAUNCH_TYPE_NONE);

        String title = sTitles[mCurrentLaunch - 1];
        setTitle(title);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        mTransactionItemList = new ArrayList<>();

        mBranchId = getIntent().getLongExtra(LAUNCH_BRANCH_ID_KEY, BRANCH_ID_NONE);

        displayItemSearcher();
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


    void setActionbarVisibility(boolean visible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && (actionBar.getCustomView() != null))
            actionBar.getCustomView().setVisibility(visible ?
                    View.VISIBLE : View.GONE);
        invalidateOptionsMenu();
    }

    void displayItemSearcher() {
        setActionbarVisibility(true);
        ItemSearchFragment fragment = ItemSearchFragment.newInstance(mBranchId,
                mCurrentLaunch == LAUNCH_TYPE_BUY);
        final AppCompatActivity activity = this;
        fragment.setResultListener(new ItemSearchFragment.SearchResultListener() {

            @Override
            public int numItemsInTransaction() {
                return mTransactionItemList.size();
            }

            @Override
            public void transactionItemAdded(STransactionItem transactionItem) {
                mTransactionItemList.add(transactionItem);
            }

            @Override
            public void finishTransaction() {
                setActionbarVisibility(false);
                final SummaryFragment summaryFragment = new SummaryFragment();
                summaryFragment.setListener(new SummaryFragment.SummaryListener() {
                    @Override
                    public void cancelSelected() {
                        activity.finish();
                    }

                    @Override
                    public void backSelected() {
                        activity.getSupportFragmentManager().popBackStack();
                        setActionbarVisibility(true);
                    }

                    @Override
                    public void okSelected(final List<STransactionItem> itemList) {
                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                if (!itemList.isEmpty())
                                    TransactionUtil.createTransactionWithItems(activity, itemList, mBranchId);

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
                    public void editItemAtPosition(List<STransactionItem> itemList, final int position) {
                        STransactionItem tranItem = itemList.get(position);
                        SItem item = tranItem.item;

                        FragmentManager fm = getSupportFragmentManager();
                        final TransDialog.QtyDialog dialog = TransDialog.newInstance(mCurrentLaunch == LAUNCH_TYPE_BUY);
                        dialog.setItem(item);
                        dialog.setBranches(getBranches());

                        dialog.setListener(new TransDialog.TransQtyDialogListener() {
                            @Override
                            public void dialogOk(STransactionItem transactionItem) {
                                dialog.dismiss();

                                mTransactionItemList.set(position, transactionItem);
                                summaryFragment.refreshAdapter();
                            }

                            @Override
                            public void dialogCancel() {
                                dialog.dismiss();
                                summaryFragment.refreshAdapter();
                            }

                        });
                        dialog.show(fm, "Set Item Quantity");
                    }

                    @Override
                    public void deleteItemAtPosition(List<STransactionItem> itemList, int position) {
                        mTransactionItemList.remove(position);
                    }
                });

                summaryFragment.mItemList = mTransactionItemList;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.transaction_action_container, summaryFragment)
                        .addToBackStack(null)
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

}
