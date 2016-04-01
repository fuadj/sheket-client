package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gamma on 3/27/16.
 */
public class SBranch extends ChangeTraceable {

    public static final String JSON_BRANCH_ID = "branch_id";
    public static final String JSON_NAME = "name";
    public static final String JSON_LOCATION = "location";

    static String _f(String s) { return BranchEntry._full(s); }

    public static final String[] BRANCH_COLUMNS = {
            _f(BranchEntry.COLUMN_COMPANY_ID),
            _f(BranchEntry.COLUMN_BRANCH_ID),
            _f(BranchEntry.COLUMN_NAME),
            _f(BranchEntry.COLUMN_LOCATION),
            _f(COLUMN_CHANGE_INDICATOR)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_BRANCH_ID = 1;
    public static final int COL_NAME = 2;
    public static final int COL_LOCATION = 3;
    public static final int COL_CHANGE = 4;

    public static final int COL_LAST = 5;

    public long company_id;
    public long branch_id;
    public String branch_name;
    public String branch_location;

    public SBranch() {
    }

    public SBranch(Cursor cursor) {
        this(cursor, 0);
    }
    public SBranch(Cursor cursor, int offset) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        branch_id = cursor.getLong(COL_BRANCH_ID + offset);
        branch_name = cursor.getString(COL_NAME + offset);
        branch_location = cursor.getString(COL_LOCATION + offset);
        change_status = cursor.getInt(COL_CHANGE + offset);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(BranchEntry.COLUMN_COMPANY_ID, company_id);
        values.put(BranchEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(BranchEntry.COLUMN_NAME, branch_name);
        values.put(BranchEntry.COLUMN_LOCATION, branch_location);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_BRANCH_ID, branch_id);
        result.put(JSON_NAME, branch_name);
        result.put(JSON_LOCATION, branch_location);
        return result;
    }
}
