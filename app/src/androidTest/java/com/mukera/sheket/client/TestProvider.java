package com.mukera.sheket.client;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SPermission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gamma on 3/2/16.
 */
public class TestProvider extends AndroidTestCase {
    static final long TEST_COMPANY_ID = 2;
    static final String TEST_COMPANY_name = "test company";
    static final String TEST_COMPANY_PERMISSION;
    static {
        SPermission p = new SPermission();
        p.setPermissionType(SPermission.PERMISSION_TYPE_ALL_ACCESS);
        TEST_COMPANY_PERMISSION = p.Encode();
    }

    static final long TEST_BRANCH_ID = 10;
    static final String TEST_BRANCH_NAME = "test branch";

    static final long TEST_ITEM_ID = 140;
    static final String TEST_ITEM_NAME = "test item";
    static final String TEST_ITEM_CODE = "21adsfqewr-25";
    static final int TEST_HAS_BARCODE = SheketContract.FALSE;
    static final long TEST_DATE = 40;

    static final String TEST_LOCATION = "a41";
    static final double TEST_QTY_REMAIN = 16.7;

    static final int TEST_QTY_TRANS = 6;
    static final int TEST_CHANGE_STATE_SYNCED = 1;
    static final int TEST_USER_ID = 10;
    static final int TEST_TRANS_TYPE = TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH;

    public static ContentValues createCompanyValues() {
        ContentValues values = new ContentValues();
        values.put(CompanyEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(CompanyEntry.COLUMN_NAME, TEST_COMPANY_name);
        values.put(CompanyEntry.COLUMN_PERMISSION, TEST_COMPANY_PERMISSION);
        return values;
    }

    public static ContentValues addChangeState(ContentValues values) {
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, TEST_CHANGE_STATE_SYNCED);
        return values;
    }

    public static ContentValues createBranchValues() {
        ContentValues values = new ContentValues();
        values.put(BranchEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(BranchEntry.COLUMN_BRANCH_ID, TEST_BRANCH_ID);
        values.put(BranchEntry.COLUMN_NAME, TEST_BRANCH_NAME);
        addChangeState(values);
        return values;
    }

    public static ContentValues createBranchItemValues() {
        ContentValues values = new ContentValues();
        values.put(BranchItemEntry.COLUMN_BRANCH_ID, TEST_BRANCH_ID);
        values.put(BranchItemEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(BranchItemEntry.COLUMN_ITEM_ID, TEST_ITEM_ID);
        values.put(BranchItemEntry.COLUMN_QUANTITY, TEST_QTY_REMAIN);
        addChangeState(values);
        return values;
    }

    public static ContentValues createItemValues() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_CODE, TEST_ITEM_CODE);
        values.put(ItemEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(ItemEntry.COLUMN_NAME, TEST_ITEM_NAME);
        values.put(ItemEntry.COLUMN_HAS_BAR_CODE, TEST_HAS_BARCODE);
        values.put(ItemEntry.COLUMN_ITEM_ID, TEST_ITEM_ID);
        addChangeState(values);
        return values;
    }

    public static ContentValues createTransactionValues() {
        ContentValues values = new ContentValues();
        values.put(TransactionEntry.COLUMN_DATE, TEST_DATE);
        values.put(TransactionEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(TransactionEntry.COLUMN_USER_ID, TEST_USER_ID);
        addChangeState(values);
        return values;
    }

    public static ContentValues createTransactionItems(long item_id, long trans_id) {
        ContentValues values = new ContentValues();
        values.put(TransItemEntry.COLUMN_COMPANY_ID, TEST_COMPANY_ID);
        values.put(TransItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(TransItemEntry.COLUMN_TRANSACTION_ID, trans_id);
        values.put(TransItemEntry.COLUMN_QTY, TEST_QTY_TRANS);
        values.put(TransItemEntry.COLUMN_TRANSACTION_TYPE, TEST_TRANS_TYPE);
        addChangeState(values);
        return values;
    }

    // The target api annotation is needed for the call to keySet -- we wouldn't want
    // to use this in our app, but in a test it's fine to assume a higher target.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void addAllContentValues(ContentValues destination, ContentValues source) {
        for (String key : source.keySet()) {
            destination.put(key, source.getAsString(key));
        }
    }

    public static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {
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

    public static void deleteAllRecords(Context context) {
        // TODO: check how to correctly implement FOREIGN KEY dependency
        // TODO: i.e: if it is "on delete cascade" it is RISKY!!!
        context.getContentResolver().delete(
                CompanyEntry.CONTENT_URI,
                null,
                null
        );
        context.getContentResolver().delete(
                TransItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null
        );
        context.getContentResolver().delete(
                TransactionEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null
        );
        context.getContentResolver().delete(
                BranchItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null
        );
        context.getContentResolver().delete(
                ItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null
        );
        context.getContentResolver().delete(
                BranchEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null
        );

        Cursor cursor = context.getContentResolver().query(
                CompanyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = context.getContentResolver().query(
                BranchEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = context.getContentResolver().query(
                BranchItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = context.getContentResolver().query(
                ItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = context.getContentResolver().query(
                TransactionEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = context.getContentResolver().query(
                TransItemEntry.buildBaseUri(TEST_COMPANY_ID),
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    static int counter = 0;
    @Override
    protected void setUp() throws Exception {
        deleteAllRecords(mContext);
        counter++;
    }

    Cursor _query(Uri uri) {
        return mContext.getContentResolver().query(uri,
                null, null, null, null);
    }

    void log(String m) {
        Log.d("Test", m + counter);
    }

    public void testInsertReadProvider() {
        log("testInsert");
        ContentValues values = createCompanyValues();
        Uri uri = mContext.getContentResolver().insert(
                CompanyEntry.CONTENT_URI, values);
        long row_id = ContentUris.parseId(uri);
        assertTrue(row_id != -1);

        Cursor cursor = _query(CompanyEntry.CONTENT_URI);
        validateCursor(cursor, values);

        cursor = _query(CompanyEntry.buildCompanyUri(row_id));
        validateCursor(cursor, values);

        values = createBranchValues();
        uri = mContext.getContentResolver().insert(
                BranchEntry.buildBaseUri(TEST_COMPANY_ID), values);
        row_id = ContentUris.parseId(uri);
        assertTrue(row_id != -1);

        cursor = _query(BranchEntry.buildBaseUri(TEST_COMPANY_ID));
        validateCursor(cursor, values);

        cursor = _query(BranchEntry.buildBranchUri(TEST_COMPANY_ID, row_id));
        validateCursor(cursor, values);

        values = createItemValues();
        uri = mContext.getContentResolver().insert(
                ItemEntry.buildBaseUri(TEST_COMPANY_ID), values);
        long item_id = ContentUris.parseId(uri);
        assertTrue(item_id != -1);

        cursor = _query(ItemEntry.buildBaseUri(TEST_COMPANY_ID));
        validateCursor(cursor, values);

        cursor = _query(ItemEntry.buildItemUri(TEST_COMPANY_ID, item_id));
        validateCursor(cursor, values);

        values = createBranchItemValues();
        uri = mContext.getContentResolver().insert(
                BranchItemEntry.buildBaseUri(TEST_COMPANY_ID), values);

        cursor = _query(BranchItemEntry.buildBaseUri(TEST_COMPANY_ID));
        validateCursor(cursor, values);

        cursor = _query(uri);
        validateCursor(cursor, values);

        cursor = _query(BranchItemEntry.buildBranchItemUri(TEST_COMPANY_ID, TEST_BRANCH_ID, TEST_ITEM_ID));
        validateCursor(cursor, values);

        cursor = getContext().getContentResolver().
                query(BranchItemEntry.buildBranchItemUri(TEST_COMPANY_ID, TEST_BRANCH_ID, TEST_ITEM_ID),
                        SBranchItem.BRANCH_ITEM_COLUMNS, null, null, null);
        validateCursor(cursor, values);

        values = createTransactionValues();
        uri = mContext.getContentResolver().insert(
                TransactionEntry.buildBaseUri(TEST_COMPANY_ID), values);

        long trans_id = TransactionEntry.getTransactionId(uri);
        assertTrue(trans_id != -1);

        cursor = _query(TransactionEntry.buildBaseUri(TEST_COMPANY_ID));
        validateCursor(cursor, values);
        cursor = _query(TransactionEntry.buildTransactionUri(TEST_COMPANY_ID, trans_id));
        validateCursor(cursor, values);

        values = createTransactionItems(item_id, trans_id);
        uri = mContext.getContentResolver().insert(
                TransItemEntry.buildBaseUri(TEST_COMPANY_ID), values);
        assertEquals(trans_id, ContentUris.parseId(uri));

        cursor = _query(TransItemEntry.
                buildTransactionItemsUri(TEST_COMPANY_ID, trans_id));
        validateCursor(cursor, values);
    }

    String _type(Uri uri) {
        return mContext.getContentResolver().getType(uri);
    }

    public void testGetType() {
        log("testGetType");
        long test_company_id = 1;

        String type = _type(CompanyEntry.CONTENT_URI);
        assertEquals(CompanyEntry.CONTENT_TYPE, type);

        type = _type(BranchEntry.buildBaseUri(test_company_id));
        assertEquals(BranchEntry.CONTENT_TYPE, type);

        long testBranch = 1;
        type = _type(BranchEntry.buildBranchUri(test_company_id, testBranch));
        assertEquals(BranchEntry.CONTENT_ITEM_TYPE, type);

        long testItem = 13;
        type = _type(ItemEntry.buildItemUri(test_company_id, testItem));
        assertEquals(ItemEntry.CONTENT_ITEM_TYPE, type);

        type = _type(ItemEntry.buildBaseUri(test_company_id));
        assertEquals(ItemEntry.CONTENT_TYPE, type);

        type = _type(BranchItemEntry.buildBranchItemUri(test_company_id, testBranch, testItem));
        assertEquals(BranchItemEntry.CONTENT_ITEM_TYPE, type);

        type = _type(BranchItemEntry.buildBranchItemUri(test_company_id, testBranch, BranchItemEntry.NO_ID_SET));
        assertEquals(BranchItemEntry.CONTENT_TYPE, type);

        type = _type(BranchItemEntry.buildBranchItemUri(test_company_id, BranchItemEntry.NO_ID_SET, testItem));
        assertEquals(BranchItemEntry.CONTENT_TYPE, type);

        type = _type(BranchItemEntry.buildBranchItemUri(test_company_id,
                BranchItemEntry.NO_ID_SET, BranchItemEntry.NO_ID_SET));
        assertEquals(BranchItemEntry.CONTENT_TYPE, type);

        type = _type(BranchItemEntry.buildAllItemsInBranchUri(test_company_id, testBranch));
        assertEquals(BranchItemEntry.CONTENT_TYPE, type);

        type = _type(BranchItemEntry.buildItemInAllBranches(test_company_id, testItem));
        assertEquals(BranchItemEntry.CONTENT_TYPE, type);

        type = _type(TransactionEntry.buildBaseUri(test_company_id));
        assertEquals(TransactionEntry.CONTENT_TYPE, type);

        long testTransaction = 13;
        type = _type(TransactionEntry.buildTransactionUri(test_company_id, testTransaction));
        assertEquals(TransactionEntry.CONTENT_ITEM_TYPE, type);

        type = _type(ItemEntry.buildBaseUri(test_company_id));
        assertEquals(ItemEntry.CONTENT_TYPE, type);

        type = _type(TransItemEntry.buildBaseUri(test_company_id));
        assertEquals(TransItemEntry.CONTENT_TYPE, type);

        type = _type(TransItemEntry.buildTransactionItemsUri(test_company_id, testTransaction));
        assertEquals(TransItemEntry.CONTENT_TYPE, type);
    }

    Uri _insert(Uri uri, ContentValues values) {
        return mContext.getContentResolver().insert(uri, values);
    }

    int _update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return mContext.getContentResolver().update(uri, values, selection, selectionArgs);
    }

    public void testDeleteRecordsAtEnd() {
        log("testDelete");
        deleteAllRecords(mContext);
    }

    public void testUpdate() {
        log("testUpdate");
        ContentValues values = createCompanyValues();

        Uri uri = _insert(CompanyEntry.CONTENT_URI, values);
        long company_id = ContentUris.parseId(uri);
        assertTrue(company_id != -1);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(CompanyEntry.COLUMN_NAME, "Changed name");

        int updated = _update(CompanyEntry.CONTENT_URI, updatedValues,
                CompanyEntry.COLUMN_COMPANY_ID + " = ?",
                new String[]{Long.toString(company_id)});
        assertEquals(updated, 1);
        Cursor cursor = _query(CompanyEntry.buildCompanyUri(company_id));
        validateCursor(cursor, updatedValues);

        values = createBranchValues();
        uri = _insert(BranchEntry.buildBaseUri(company_id), values);
        assertTrue(ContentUris.parseId(uri) != -1);

        values = createItemValues();
        uri = _insert(ItemEntry.buildBaseUri(company_id), values);
        long item_id = ContentUris.parseId(uri);
        assertTrue(item_id > 0);

        values = createBranchItemValues();
        uri = mContext.getContentResolver().insert(
                BranchItemEntry.buildBaseUri(company_id), values);

        cursor = _query(uri);
        validateCursor(cursor, values);

        values = createTransactionValues();
        uri = _insert(TransactionEntry.buildBaseUri(company_id), values);
        long trans_id = ContentUris.parseId(uri);
        assertTrue(trans_id > 0);
    }
}
