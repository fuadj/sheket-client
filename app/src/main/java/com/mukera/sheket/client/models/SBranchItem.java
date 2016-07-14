package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.v4.util.Pair;

import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by gamma on 3/27/16.
 */
public class SBranchItem extends ChangeTraceable {
    public static final String JSON_BRANCH_ITEM_ID = "branch_item_id";
    public static final String JSON_QUANTITY = "quantity";
    public static final String JSON_LOCATION = "item_location";

    static String _f(String s) { return BranchItemEntry._full(s); }

    public static final String[] BRANCH_ITEM_COLUMNS = {
            _f(BranchItemEntry.COLUMN_COMPANY_ID),
            _f(BranchItemEntry.COLUMN_BRANCH_ID),
            _f(BranchItemEntry.COLUMN_ITEM_ID),
            _f(BranchItemEntry.COLUMN_QUANTITY),
            _f(BranchItemEntry.COLUMN_ITEM_LOCATION),
            _f(COLUMN_CHANGE_INDICATOR)
    };

    public static final String[] BRANCH_ITEM_WITH_DETAIL_COLUMNS;
    static {
        int size = BRANCH_ITEM_COLUMNS.length + SItem.ITEM_COLUMNS.length;
        BRANCH_ITEM_WITH_DETAIL_COLUMNS = new String[size];

        System.arraycopy(BRANCH_ITEM_COLUMNS, 0, BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                0, BRANCH_ITEM_COLUMNS.length);
        System.arraycopy(SItem.ITEM_COLUMNS, 0, BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                BRANCH_ITEM_COLUMNS.length, SItem.ITEM_COLUMNS.length);
    }

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_BRANCH_ID = 1;
    public static final int COL_ITEM_ID = 2;
    public static final int COL_QUANTITY = 3;
    public static final int COL_ITEM_LOCATION = 4;
    public static final int COL_CHANGE = 5;

    public static final int COL_LAST = 6;

    public long company_id;
    public long branch_id;
    public long item_id;
    public double quantity;
    public String item_location;

    public SItem item;

    public SBranchItem() {
    }

    public SBranchItem(Cursor cursor) {
        this(cursor, 0, false);
    }

    public SBranchItem(Cursor cursor, boolean fetch_item) {
        this(cursor, 0, fetch_item);
    }

    public SBranchItem(Cursor cursor, int offset) {
        this(cursor, offset, false);
    }

    public SBranchItem(Cursor cursor, int offset, boolean fetch_item) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        branch_id = cursor.getLong(COL_BRANCH_ID + offset);
        item_id = cursor.getLong(COL_ITEM_ID + offset);
        quantity = cursor.getDouble(COL_QUANTITY + offset);
        item_location = cursor.getString(COL_ITEM_LOCATION + offset);
        change_status = cursor.getInt(COL_CHANGE + offset);
        if (fetch_item) {
            item = new SItem(cursor, offset + COL_LAST);
        }
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(BranchItemEntry.COLUMN_COMPANY_ID, company_id);
        values.put(BranchItemEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(BranchItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(BranchItemEntry.COLUMN_QUANTITY, quantity);
        values.put(BranchItemEntry.COLUMN_ITEM_LOCATION, item_location);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    /**
     * This will NOT include the quantity element in the result
     */
    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_BRANCH_ITEM_ID,
                String.format(Locale.US, "%d:%d", branch_id, item_id));
        result.put(JSON_QUANTITY, quantity);
        result.put(JSON_LOCATION, item_location);
        return result;
    }

    public static Pair<SBranchItem, SItem> getBranchItemWithItem(Cursor cursor) {
        SBranchItem branchItem = new SBranchItem(cursor);
        SItem item = new SItem(cursor, COL_LAST);
        return new Pair<>(branchItem, item);
    }
}
