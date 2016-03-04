package com.mukera.sheket.client.models;

import android.database.Cursor;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import com.mukera.sheket.client.contentprovider.SheketContract.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/3/16.
 */
public class STransaction {
    private static final String LOG_TAG = STransaction.class.getSimpleName();

    static String _f(String s) {
        return TransactionEntry._full(s);
    }

    public static final String[] TRANSACTION_COLUMNS = {
            _f(TransactionEntry._ID),
            _f(TransactionEntry.COLUMN_TYPE),
            _f(TransactionEntry.COLUMN_DATE),
            _f(TransactionEntry.COLUMN_QTY_TOTAL)
    };

    public static final String[] TRANSACTION_JOIN_ITEMS_COLUMNS;
    static {
        int trans_size = TRANSACTION_COLUMNS.length;
        int affected_size = SAffectedItems.AFFECTED_COLUMNS.length;
        int item_size = SItem.ITEM_COLUMNS.length;

        int total_size = affected_size + item_size + trans_size;
        TRANSACTION_JOIN_ITEMS_COLUMNS = new String[total_size];

        System.arraycopy(TRANSACTION_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                0, trans_size);
        System.arraycopy(SAffectedItems.AFFECTED_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                trans_size, affected_size);
        System.arraycopy(SItem.ITEM_COLUMNS, 0, TRANSACTION_JOIN_ITEMS_COLUMNS,
                trans_size + affected_size, item_size);
    }

    public static final int COL_ID = 0;
    public static final int COL_TYPE = 1;
    public static final int COL_DATE = 2;
    public static final int COL_QTY_TOTAL = 3;

    // use this to retrieve next columns in a joined query
    public static final int COL_LAST = 4;

    public long id;
    public int type;
    public long date;
    public double total_qty;

    public List<SAffectedItems> affectedItems;

    public STransaction(Cursor cursor) {
        this(cursor, 0, false);
    }

    public STransaction(Cursor cursor, boolean fetch_affected) {
        this(cursor, 0, fetch_affected);
    }

    public STransaction(Cursor cursor, int offset, boolean fetch_affected) {
        id = cursor.getLong(COL_ID + offset);
        type = cursor.getInt(COL_TYPE + offset);
        date = cursor.getLong(COL_DATE + offset);
        total_qty = cursor.getDouble(COL_QTY_TOTAL + offset);

        affectedItems = new ArrayList<>();

        if (fetch_affected) {
            do {
                affectedItems.add(
                        new SAffectedItems(cursor, offset + COL_LAST, true));
            } while (cursor.moveToNext());
        }
    }

    public static final class SAffectedItems {
        static String _f(String s) { return AffectedItemEntry._full(s); }
        public static final String[] AFFECTED_COLUMNS = {
                _f(AffectedItemEntry.COLUMN_TRANSACTION_ID),
                _f(AffectedItemEntry.COLUMN_ITEM_ID),
                _f(AffectedItemEntry.COLUMN_QTY)
        };

        public static final int COL_TRANS_ID = 0;
        public static final int COL_ITEM_ID = 1;
        // We really don't use the above 2, its just b/c they appear
        // in the results from the queries. The other stuff is after
        // them, so they are important.

        public static final int COL_QTY = 2;

        public static final int COL_LAST = 3;

        public SItem item;
        public double qty;

        public SAffectedItems(Cursor cursor, int offset, boolean fetch_item) {
            qty = cursor.getDouble(COL_QTY + offset);
            if (fetch_item) {
                item = new SItem(cursor, offset + COL_LAST);
            }
        }
    }
}
