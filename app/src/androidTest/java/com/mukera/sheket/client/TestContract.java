package com.mukera.sheket.client;

import android.test.AndroidTestCase;

import com.mukera.sheket.client.contentprovider.SheketContract.*;

/**
 * Created by gamma on 3/2/16.
 */
public class TestContract extends AndroidTestCase {
    public void testUriBuilder() {

    }

    String fullPath(String table, String col) {
        return table + "." + col;
    }

    public void testColFullName() {
        assertEquals(fullPath(CategoryEntry.TABLE_NAME, CategoryEntry.COLUMN_NAME),
            CategoryEntry._full(CategoryEntry.COLUMN_NAME));
    }
}
