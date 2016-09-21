package com.mukera.sheket.client.controller.items.transactions;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by fuad on 6/22/16.
 */
public class TransactionUtil {
    public static void reverseTransactionWithItems(Context context, STransaction transaction,
                                                   List<STransaction.STransactionItem> itemList) {
        long company_id = PrefUtil.getCurrentCompanyId(context);

        // because there is a foreign dependency on the transaction items, deleting the
        // transaction also deletes the items involved in it.
        context.getContentResolver().delete(
                TransactionEntry.buildBaseUri(company_id),
                TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID) + " = ?",
                new String[]{Long.toString(transaction.transaction_id)});

        updateBranchItemQuantities(context, itemList, transaction.branch_id, true);
    }

    public static boolean commitTransactionWithItems(Context context,
                                                     List<STransaction.STransactionItem> itemList,
                                                     long branch_id,
                                                     String transactionNote) {
        if (itemList.isEmpty())
            return false;

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        long new_trans_id = PrefUtil.getNewTransId(context);
        PrefUtil.setNewTransId(context, new_trans_id);

        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_TRANS_ID, new_trans_id);
        values.put(TransactionEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(TransactionEntry.COLUMN_TRANS_NOTE, transactionNote);
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_CREATED);
        values.put(TransactionEntry.COLUMN_COMPANY_ID, PrefUtil.getCurrentCompanyId(context));
        values.put(TransactionEntry.COLUMN_USER_ID, PrefUtil.getUserId(context));

        values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());

        values.put(TransactionEntry.COLUMN_DATE, SheketContract.getDbDateInteger(new Date()));

        long company_id = PrefUtil.getCurrentCompanyId(context);
        operations.add(
                ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(company_id)).
                        withValues(values).build());
        for (STransaction.STransactionItem item : itemList) {
            operations.add(
                    ContentProviderOperation.newInsert(TransItemEntry.buildBaseUri(company_id)).
                            withValues(item.toContentValues()).
                            withValueBackReference(TransItemEntry.COLUMN_TRANSACTION_ID, 0).
                            build());
        }

        boolean success = true;
        try {
            context.getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operations);

        } catch (RemoteException | OperationApplicationException e) {
            success = false;
        }

        updateBranchItemQuantities(context, itemList, branch_id, false);

        CategoryUtil.updateBranchCategoriesForAllBranches(context);

        return success;
    }

    /**
     * Goes through the transaction's items and applies the operations to update the quantities
     * of the items in the transaction. If the transaction affects another branch, the corresponding
     * computation is performed. This method also supports reversing a transactions, so any
     * quantity changes can be "un-done".
     *
     * @param context
     * @param transItemList
     * @param current_branch_id
     * @param reverse_transaction
     */
    private static void updateBranchItemQuantities(Context context,
                                                   List<STransaction.STransactionItem> transItemList,
                                                   long current_branch_id,
                                                   boolean reverse_transaction) {
        HashMap<KeyBranchItem, SBranchItem> seenBranchItems = new HashMap<>();
        long company_id = PrefUtil.getCurrentCompanyId(context);

        for (STransaction.STransactionItem transItem : transItemList) {
            switch (transItem.trans_type) {
                case TransItemEntry.TYPE_INCREASE_PURCHASE:
                case TransItemEntry.TYPE_INCREASE_RETURN_ITEM: {
                    SBranchItem branchItem = getBranchItem(context, company_id, seenBranchItems, current_branch_id, transItem.item_id);
                    if (reverse_transaction)
                        branchItem.quantity = branchItem.quantity - transItem.quantity;
                    else
                        branchItem.quantity = branchItem.quantity + transItem.quantity;
                    setBranchItem(context, company_id, seenBranchItems, branchItem);
                    // increase branch item
                    break;
                }

                case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER: {
                    long source_branch, dest_branch;
                    if (transItem.trans_type == TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH) {
                        source_branch = transItem.other_branch_id;
                        dest_branch = current_branch_id;
                    } else {
                        source_branch = current_branch_id;
                        dest_branch = transItem.other_branch_id;
                    }

                    if (reverse_transaction) {
                        long temp_branch = source_branch;
                        source_branch = dest_branch;
                        dest_branch = temp_branch;
                    }

                    SBranchItem sourceItem = getBranchItem(context, company_id, seenBranchItems, source_branch, transItem.item_id);
                    SBranchItem destItem = getBranchItem(context, company_id, seenBranchItems, dest_branch, transItem.item_id);

                    sourceItem.quantity = sourceItem.quantity - transItem.quantity;
                    destItem.quantity = destItem.quantity + transItem.quantity;

                    setBranchItem(context, company_id, seenBranchItems, sourceItem);
                    setBranchItem(context, company_id, seenBranchItems, destItem);

                    break;
                }

                case TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH: {
                    SBranchItem branchItem = getBranchItem(context, company_id, seenBranchItems, current_branch_id, transItem.item_id);
                    if (reverse_transaction)
                        branchItem.quantity = branchItem.quantity + transItem.quantity;
                    else
                        branchItem.quantity = branchItem.quantity - transItem.quantity;
                    setBranchItem(context, company_id, seenBranchItems, branchItem);
                    break;
                }
            }
        }
    }

    private static SBranchItem getBranchItem(Context context, long company_id,
                                             HashMap<KeyBranchItem, SBranchItem> seenBranchItems,
                                             long branch_id, long item_id) {
        KeyBranchItem key = new KeyBranchItem(branch_id, item_id);

        if (seenBranchItems.containsKey(key)) {
            return seenBranchItems.get(key);
        }

        Cursor cursor = context.getContentResolver().
                query(BranchItemEntry.buildBranchItemUri(company_id, branch_id, item_id),
                        SBranchItem.BRANCH_ITEM_COLUMNS,
                        null, null, null);
        SBranchItem item;
        if (cursor != null && cursor.moveToFirst()) {
            item = new SBranchItem(cursor);

            /**
             * IMPORTANT: we haven't still synced the 'created' item, so don't change flag to update.
             * It will create sync problems if we try to send a "create" item as an "update", the
             * server will reply with an error.
             */
            if (item.change_status != ChangeTraceable.CHANGE_STATUS_CREATED)
                item.change_status = ChangeTraceable.CHANGE_STATUS_UPDATED;
        } else {
            // the item doesn't exist in the branch, create a new item
            item = new SBranchItem();
            item.company_id = company_id;
            item.branch_id = branch_id;
            item.item_id = item_id;
            item.quantity = 0;
            item.change_status = ChangeTraceable.CHANGE_STATUS_CREATED;
        }

        if (cursor != null)
            cursor.close();

        seenBranchItems.put(key, item);
        return item;
    }

    static void setBranchItem(Context context, long company_id,
                              HashMap<KeyBranchItem, SBranchItem> seenBranchItems, SBranchItem branchItem) {
        context.getContentResolver().insert(BranchItemEntry.buildBaseUri(company_id),
                DbUtil.setUpdateOnConflict(branchItem.toContentValues()));

        // update the cache
        seenBranchItems.put(new KeyBranchItem(branchItem.branch_id, branchItem.item_id),
                branchItem);
    }

    static class KeyBranchItem {
        long branch_id, item_id;

        public KeyBranchItem(long b_id, long i_id) {
            branch_id = b_id;
            item_id = i_id;
        }
    }
}
