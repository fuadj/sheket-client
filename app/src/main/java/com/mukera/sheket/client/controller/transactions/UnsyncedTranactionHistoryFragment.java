package com.mukera.sheket.client.controller.transactions;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.mukera.sheket.client.controller.admin.TransactionHistoryFragment;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 6/12/16.
 */
public class UnsyncedTranactionHistoryFragment extends TransactionHistoryFragment {
    /**
     * Since we are displaying un-synced transactions, they are
     * transaction that were created by us, no need to display username
     * @return
     */
    @Override
    protected boolean displayUserName() {
        return false;
    }

    @Override
    protected boolean displayDeleteButton() {
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // we are sorting by descending order because these are un-synced transactions
        // that are negative and go down.
        String sortOrder = TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID) + " DESC";
        return new CursorLoader(getActivity(),
                TransItemEntry.buildTransactionItemsUri(
                        PrefUtil.getCurrentCompanyId(getContext()),
                        TransItemEntry.NO_TRANS_ID_SET),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS,
                // only display the negative(the un-synced) transactions
                TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID) + " < 0",
                null,
                sortOrder);
    }
}
