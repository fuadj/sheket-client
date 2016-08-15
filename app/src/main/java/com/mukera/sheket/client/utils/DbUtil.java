package com.mukera.sheket.client.utils;

import android.content.ContentValues;

import com.mukera.sheket.client.data.SheketContract;

/**
 * Created by gamma on 4/9/16.
 */
public class DbUtil {
    /**
     * Adds to the content values the option to update the item if it already
     * exists in the database.
     */
    public static ContentValues setUpdateOnConflict(ContentValues values) {
        values.put(SheketContract.SQL_INSERT_OR_UPDATE, true);
        return values;
    }

}
