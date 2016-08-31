package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;

/**
 * Created by fuad on 8/30/16.
 */
public class SCompany {
    static String f(String s) { return CompanyEntry._full(s); }

    public static final String[] COMPANY_COLUMNS = {
            f(CompanyEntry.COLUMN_COMPANY_ID),
            f(CompanyEntry.COLUMN_NAME),
            f(CompanyEntry.COLUMN_PERMISSION),
            f(CompanyEntry.COLUMN_STATE_BACKUP)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_COMPANY_NAME = 1;
    public static final int COL_PERMISSION = 2;
    public static final int COL_STATE_BKUP = 3;

    public long company_id;
    public String name;
    public String encoded_permission;
    public String state_bkup;

    public SCompany() {}

    public SCompany(Cursor cursor) {
        this(cursor, 0);
    }

    public SCompany(Cursor cursor, int offset) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        name = cursor.getString(COL_COMPANY_NAME + offset);
        encoded_permission = cursor.getString(COL_PERMISSION + offset);
        state_bkup = cursor.getString(COL_STATE_BKUP + offset);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CompanyEntry.COLUMN_COMPANY_ID, company_id);
        values.put(CompanyEntry.COLUMN_NAME, name);
        values.put(CompanyEntry.COLUMN_PERMISSION, encoded_permission);
        values.put(CompanyEntry.COLUMN_STATE_BACKUP, state_bkup);
        return values;
    }
}
