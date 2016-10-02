package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.network.BranchCategory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by fuad on 7/14/16.
 */
public class SBranchCategory extends ChangeTraceable {
    public static final String JSON_BRANCH_CATEGORY_BOTH_ID = "branch_category_id";
    public static final String JSON_BRANCH_ID = "branch_id";
    public static final String JSON_CATEGORY_ID = "category_id";

    static String _f(String s) { return BranchCategoryEntry._full(s); }

    public static final String[] BRANCH_CATEGORY_COLUMNS = {
            _f(BranchCategoryEntry.COLUMN_COMPANY_ID),
            _f(BranchCategoryEntry.COLUMN_BRANCH_ID),
            _f(BranchCategoryEntry.COLUMN_CATEGORY_ID),
            _f(COLUMN_CHANGE_INDICATOR)
    };

    /**
     * This query projection has its first part similar to that of a
     * category query projection. So you can also just use the first
     * part to parse out an SCategory and be done with it.
     */
    public static final String[] BRANCH_CATEGORY_WITH_CATEGORY_DETAIL_COLUMNS;
    static {
        int size = BRANCH_CATEGORY_COLUMNS.length +
                SCategory.CATEGORY_COLUMNS.length;
        BRANCH_CATEGORY_WITH_CATEGORY_DETAIL_COLUMNS = new String[size];

        System.arraycopy(SCategory.CATEGORY_COLUMNS, 0,
                BRANCH_CATEGORY_WITH_CATEGORY_DETAIL_COLUMNS,
                0, SCategory.CATEGORY_COLUMNS.length);
        System.arraycopy(BRANCH_CATEGORY_COLUMNS, 0,
                BRANCH_CATEGORY_WITH_CATEGORY_DETAIL_COLUMNS,
                SCategory.CATEGORY_COLUMNS.length,
                BRANCH_CATEGORY_COLUMNS.length);
    }

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_BRANCH_ID = 1;
    public static final int COL_CATEGORY_ID = 2;
    public static final int COL_CHANGE = 3;

    public static final int COL_LAST = 4;

    public long company_id;
    public long branch_id;
    public long category_id;

    public SCategory category;

    public SBranchCategory() {
    }

    public SBranchCategory(BranchCategory gRPC_Branch_Category) {
        branch_id = gRPC_Branch_Category.getBranchId();
        category_id = gRPC_Branch_Category.getCategoryId();
    }

    public SBranchCategory(Cursor cursor) {
        this(cursor, 0, false);
    }

    public SBranchCategory(Cursor cursor, int offset, boolean fetch_category) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        branch_id = cursor.getLong(COL_BRANCH_ID + offset);
        category_id = cursor.getLong(COL_CATEGORY_ID + offset);

        change_status = cursor.getInt(COL_CHANGE + offset);
        if (fetch_category) {
            category = new SCategory(cursor, offset + COL_LAST);
        }
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(BranchCategoryEntry.COLUMN_COMPANY_ID, company_id);
        values.put(BranchCategoryEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(BranchCategoryEntry.COLUMN_CATEGORY_ID, category_id);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_BRANCH_CATEGORY_BOTH_ID,
                String.format(Locale.US, "%d:%d", branch_id, category_id));
        return json;
    }

    public BranchCategory.Builder toGRPCBuilder() {
        return BranchCategory.newBuilder().
                setBranchId((int)branch_id).
                setCategoryId((int)category_id);
    }
}
