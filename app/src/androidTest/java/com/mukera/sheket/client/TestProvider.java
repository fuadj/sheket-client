package com.mukera.sheket.client;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;

import com.mukera.sheket.client.contentprovider.SheketContract.*;
import com.mukera.sheket.client.contentprovider.SheketProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gamma on 3/2/16.
 */
public class TestProvider extends AndroidTestCase {
    static final int TEST_CATEGORY_ID = 3;
    static final String TEST_CATEGORY_NAME = "test category";
    static final int TEST_CODE_TYPE = ItemEntry.CODE_TYPE_MANUAL;
    static final String TEST_ITEM_CODE = "21adsfqewr-25";
    static final long TEST_DATE = 40;
    static final String TEST_LOCATION = "a41";
    static final int TEST_QTY_REMAIN = 16;
    static final int TEST_QTY_TRANS = 6;

    static ContentValues createCategoryValues() {
        ContentValues values = new ContentValues();
        values.put(CategoryEntry._ID, TEST_CATEGORY_ID);
        values.put(CategoryEntry.COLUMN_NAME, TEST_CATEGORY_NAME);
        return values;
    }

    static ContentValues createTransactionValues() {
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_TYPE, TransactionEntry.TRANS_TYPE_BUY);
        values.put(TransactionEntry.COLUMN_DATE, TEST_DATE);
        values.put(TransactionEntry.COLUMN_QTY_TOTAL, TEST_QTY_TRANS);
        return values;
    }

    static ContentValues createItemValues() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_CATEGORY_ID, TEST_CATEGORY_ID);
        values.put(ItemEntry.COLUMN_CODE_TYPE, TEST_CODE_TYPE);
        values.put(ItemEntry.COLUMN_MANUAL_CODE, TEST_ITEM_CODE);
        values.put(ItemEntry.COLUMN_LOCATION, TEST_LOCATION);
        values.put(ItemEntry.COLUMN_QTY_REMAIN, TEST_QTY_REMAIN);
        return values;
    }

    static ContentValues createAffectedValues(long item_id, long trans_id) {
        ContentValues values = new ContentValues();
        values.put(AffectedItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(AffectedItemEntry.COLUMN_TRANSACTION_ID, trans_id);
        values.put(AffectedItemEntry.COLUMN_QTY, TEST_QTY_TRANS);
        return values;
    }

    // The target api annotation is needed for the call to keySet -- we wouldn't want
    // to use this in our app, but in a test it's fine to assume a higher target.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void addAllContentValues(ContentValues destination, ContentValues source) {
        for (String key : source.keySet()) {
            destination.put(key, source.getAsString(key));
        }
    }

    static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("The cursor has no values", valueCursor.moveToFirst());

        /**
         * When checking cursors from query of joined tables, there might me
         * duplicate column ids and {@code Cursor.getColumnIndex} won't cut it then.
         * We should check against all possible columns with similar names if ANY of them match.
         */
        Map<String, List<Integer>> columnIndexMap = new HashMap<>();
        int i = 0;
        for (String name : valueCursor.getColumnNames()) {
            Integer I = Integer.valueOf(i++);
            if (columnIndexMap.get(name) == null) {
                columnIndexMap.put(name, new ArrayList<Integer>());
            }
            columnIndexMap.get(name).add(I);
        }

        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();

            List<Integer> idxs = columnIndexMap.get(columnName);
            if (idxs == null || idxs.isEmpty()) continue;

            boolean matched = false;
            String expected = entry.getValue().toString(), got = "";

            for (Integer index : idxs) {
                got = valueCursor.getString(index);
                if (expected.equals(got)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // just use one of the got values
                assertEquals(String.format("Column '%s',\n          Wanted %s, got %s", columnName, expected, got),
                        expected, got);
            }
        }
        valueCursor.close();
    }

    public void testUriMatcher() {
        int trans_id = 10, category_id = 400, item_id = 500;

        Uri uri = CategoryEntry.CONTENT_URI;
        assertEquals("testUriMatcher Category Uri didn't match", m(uri), SheketProvider.CATEGORY);
        uri = CategoryEntry.buildCategoryUri(category_id);
        assertEquals("testUriMatcher CategoryWithId Uri didn't match", m(uri), SheketProvider.CATEGORY_WITH_ID);

        uri = TransactionEntry.CONTENT_URI;
        assertEquals("testUriMatcher Transaction Uri didn't match", m(uri), SheketProvider.TRANSACTION);
        uri = TransactionEntry.buildTransactionUri(trans_id);
        assertEquals("testUriMatcher TransactionWithId Uri didn't match", m(uri), SheketProvider.TRANSACTION_WITH_ID);

        uri = ItemEntry.CONTENT_URI;
        assertEquals("testUriMatcher Item Uri didn't match", m(uri), SheketProvider.ITEM);
        uri = ItemEntry.buildItemUri(item_id);
        assertEquals("testUriMatcher ItemWithId Uri didn't match", m(uri), SheketProvider.ITEM_WITH_ID);
        uri = ItemEntry.buildItemWithCategoryId(category_id);
        assertEquals("testUriMatcher ItemInCategory Uri didn't match", m(uri), SheketProvider.ITEM_IN_CATEGORY);

        uri = AffectedItemEntry.CONTENT_URI;
        assertEquals("testUriMatcher AffectedItem Uri didn't match", m(uri), SheketProvider.AFFECTED_ITEM);
        uri = AffectedItemEntry.buildAffectedItemsWithTransactionUri(trans_id);
        assertEquals("testUriMatcher AffectedItemWithTransactionId Uri didn't match",
                m(uri), SheketProvider.AFFECTED_ITEM_WITH_TRANSACTION_ID);
    }

    int m(Uri uri) { return SheketProvider.match(uri); }

    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                CategoryEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                TransactionEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                ItemEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                AffectedItemEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                CategoryEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                TransactionEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                ItemEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                AffectedItemEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Override
    protected void setUp() throws Exception {
        deleteAllRecords();
    }


    Cursor _query(Uri uri) {
        return mContext.getContentResolver().query(uri,
                null, null, null, null);
    }

    public void testInsertReadProvider() {
        ContentValues values = createCategoryValues();
        Uri uri = mContext.getContentResolver().insert(
                CategoryEntry.CONTENT_URI, values);
        long row_id = ContentUris.parseId(uri);
        assertTrue(row_id != -1);

        Cursor cursor = _query(CategoryEntry.CONTENT_URI);
        validateCursor(cursor, values);

        cursor = _query(CategoryEntry.buildCategoryUri(TEST_CATEGORY_ID));
        validateCursor(cursor, values);

        values = createItemValues();
        uri = mContext.getContentResolver().insert(
                ItemEntry.CONTENT_URI, values);
        long item_id = ContentUris.parseId(uri);
        assertTrue(item_id != -1);

        cursor = _query(ItemEntry.CONTENT_URI);
        validateCursor(cursor, values);

        cursor = _query(ItemEntry.buildItemUri(item_id));
        validateCursor(cursor, values);

        cursor = _query(ItemEntry.buildItemWithCategoryId(TEST_CATEGORY_ID));
        validateCursor(cursor, values);

        values = createTransactionValues();
        uri = mContext.getContentResolver().insert(
                TransactionEntry.CONTENT_URI, values);

        long trans_id = ContentUris.parseId(uri);
        assertTrue(trans_id != -1);

        cursor = _query(TransactionEntry.CONTENT_URI);
        validateCursor(cursor, values);
        cursor = _query(TransactionEntry.buildTransactionUri(trans_id));
        validateCursor(cursor, values);

        values = createAffectedValues(item_id, trans_id);
        uri = mContext.getContentResolver().insert(
                AffectedItemEntry.CONTENT_URI, values);
        assertEquals(trans_id, ContentUris.parseId(uri));
        cursor = _query(AffectedItemEntry
                    .buildAffectedItemsWithTransactionUri(trans_id));
        validateCursor(cursor, values);
    }
}
