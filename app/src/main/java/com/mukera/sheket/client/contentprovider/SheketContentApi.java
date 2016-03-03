package com.mukera.sheket.client.contentprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.mukera.sheket.client.contentprovider.SheketContract.*;

/**
 * Created by gamma on 3/3/16.
 */
public class SheketContentApi {
    private static int updateTable(Context context, Uri uri, String col_id, long _id, ContentValues newVals) {
        return context.getContentResolver().update(uri, newVals,
                col_id + " = ?", new String[]{String.valueOf(_id)});
    }

    public static int updateCategory(Context context, long cat_id, ContentValues newValues) {
        return updateTable(context, CategoryEntry.CONTENT_URI,
                CategoryEntry._ID, cat_id, newValues);
    }

    public static int updateTransaction(Context context, long trans_id, ContentValues newValues) {
        return updateTable(context, TransactionEntry.CONTENT_URI,
                TransactionEntry._ID, trans_id, newValues);
    }

    public static int updateItem(Context context, long item_id, ContentValues newValues) {
        return updateTable(context, ItemEntry.CONTENT_URI,
                ItemEntry._ID, item_id, newValues);
    }

    public static int updateAffectedItems(Context context, long trans_id,
                                          long item_id, ContentValues newValues) {
        final String selection = AffectedItemEntry.COLUMN_TRANSACTION_ID + " = ? AND " +
                AffectedItemEntry.COLUMN_ITEM_ID + " = ?";
        return context.getContentResolver().update(AffectedItemEntry.CONTENT_URI,
                newValues,
                selection,
                new String[]{String.valueOf(trans_id), String.valueOf(item_id)}
        );
    }

}
