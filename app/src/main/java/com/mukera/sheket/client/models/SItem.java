package com.mukera.sheket.client.models;

import android.database.Cursor;
import android.graphics.ColorMatrix;

import com.mukera.sheket.client.contentprovider.SheketContract.*;

/**
 * Created by gamma on 3/3/16.
 */
public class SItem {
    private static final String LOG_TAG = SItem.class.getSimpleName();

    static String _f(String s) {
        return ItemEntry._full(s);
    }

    public static final String[] ITEM_COLUMNS = {
            _f(ItemEntry._ID),
            _f(ItemEntry.COLUMN_CATEGORY_ID),
            _f(ItemEntry.COLUMN_NAME),
            _f(ItemEntry.COLUMN_LOCATION),
            _f(ItemEntry.COLUMN_DESC),
            _f(ItemEntry.COLUMN_QTY_REMAIN),
            _f(ItemEntry.COLUMN_CODE_TYPE),
            _f(ItemEntry.COLUMN_BAR_CODE),
            _f(ItemEntry.COLUMN_MANUAL_CODE)
    };

    public static final int COL_ID = 0;
    public static final int COL_CATEGORY_ID = 1;
    public static final int COL_NAME = 2;
    public static final int COL_LOC = 3;
    public static final int COL_DESC = 4;
    public static final int COL_QTY = 5;
    public static final int COL_CODE_TYPE = 6;
    public static final int COL_BARCODE = 7;
    public static final int COL_MANUAL = 8;

    public static final int COL_LAST = 9;

    public long id;
    public long category_id;
    public String name;
    public String location;
    public String description;
    public double qty_remain;

    public int code_type;
    public String bar_code;
    public String manual_code;

    public SItem(Cursor cursor) {
        this(cursor, 0);
    }

    public SItem(Cursor cursor, int offset) {
        id = cursor.getLong(COL_ID + offset);
        category_id = cursor.getLong(COL_CATEGORY_ID + offset);
        name = cursor.getString(COL_NAME + offset);
        location = cursor.getString(COL_LOC + offset);
        description = cursor.getString(COL_DESC);
        qty_remain = cursor.getDouble(COL_QTY + offset);
        code_type = cursor.getInt(COL_CODE_TYPE + offset);
        bar_code = cursor.getString(COL_BARCODE + offset);
        manual_code = cursor.getString(COL_MANUAL + offset);
    }
}

