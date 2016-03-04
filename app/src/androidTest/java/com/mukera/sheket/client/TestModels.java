package com.mukera.sheket.client;


import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.mukera.sheket.client.contentprovider.SheketContract.*;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/3/16.
 */
public class TestModels extends AndroidTestCase {
    // because the fields are 'AUTOINCREMENT', we need to save them for query
    private static long sCategoryId = -1;
    private static List<Long> sItemIds = null;

    void setupCategory() {
        ContentValues values = TestProvider.createCategoryValues();
        Uri uri = mContext.getContentResolver().insert(
                CategoryEntry.CONTENT_URI, values);
        sCategoryId = ContentUris.parseId(uri);
        assertTrue(sCategoryId != -1);
    }

    void setupItems() {
        String[] codeArr = {"lfakd", "JM-009", "784596", "12w3"};
        String[] locArr = {"A09", "B23", "C21"};
        double[] qtyArr = {0.0, 100.0, -30.0, 0.0023};

        sItemIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ContentValues values = TestProvider.createItemValues();
            if (((i*17)-7) % 5 > 2) {
                values.put(ItemEntry.COLUMN_CODE_TYPE, ItemEntry.CODE_TYPE_MANUAL);
                values.put(ItemEntry.COLUMN_MANUAL_CODE,
                        codeArr[((i*13) - 3)%codeArr.length]);
                values.put(ItemEntry.COLUMN_CATEGORY_ID, sCategoryId);
            } else {
                values.put(ItemEntry.COLUMN_CODE_TYPE, ItemEntry.COLUMN_BAR_CODE);
                values.put(ItemEntry.COLUMN_MANUAL_CODE,
                        codeArr[((i * 23) + 7) % codeArr.length]);
            }
            values.put(ItemEntry.COLUMN_LOCATION, locArr[(i*13)%locArr.length]);
            values.put(ItemEntry.COLUMN_QTY_REMAIN, qtyArr[i % qtyArr.length]);

            Uri uri = mContext.getContentResolver().insert(
                    ItemEntry.CONTENT_URI, values);
            long item_id = ContentUris.parseId(uri);
            assertTrue(item_id != -1);

            sItemIds.add(item_id);
        }
    }

    @Override
    protected void setUp() throws Exception {
        TestProvider.deleteAllRecords(mContext);
        setupCategory();
        setupItems();
    }

    Cursor _query(Uri uri) {
        return mContext.getContentResolver().query(uri,
                null, null, null, null);
    }

    public void testModelCursorRetrieval() {
        checkItemModel();
        checkTransactionModel();
    }

    void checkItemModel() {
        ContentValues values = TestProvider.createItemValues();
        Uri uri = mContext.getContentResolver().insert(
                ItemEntry.CONTENT_URI, values);
        long item_id = ContentUris.parseId(uri);
        assertTrue(item_id != -1);

        Cursor cursor = _query(ItemEntry.buildItemUri(item_id));
        assertTrue("No value in cursor", cursor.moveToFirst());
        SItem item = new SItem(cursor);
        assertEquals(item.id, item_id);
        assertEquals(item.category_id, sCategoryId);
        assertEquals(item.name, values.getAsString(ItemEntry.COLUMN_NAME));
        assertEquals(item.bar_code, values.getAsString(ItemEntry.COLUMN_BAR_CODE));
        cursor.close();
    }

    void checkTransactionModel() {
        ContentValues transactionValues = TestProvider.createTransactionValues();
        Uri uri = mContext.getContentResolver().insert(
                TransactionEntry.CONTENT_URI, transactionValues);
        long trans_id = ContentUris.parseId(uri);
        assertTrue(trans_id != -1);

        int n = (int)(Math.min(0.3, Math.random()) * sItemIds.size());
        List<ContentValues> values = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            values.add(TestProvider.createAffectedValues(sItemIds.get(i), trans_id));
        }

        ContentValues[] valuesArr = new ContentValues[values.size()];
        values.toArray(valuesArr);
        int count = mContext.getContentResolver().bulkInsert(AffectedItemEntry.CONTENT_URI,
                valuesArr);
        assertEquals(count, values.size());

        Cursor cursor = _query(AffectedItemEntry.buildAffectedItemsWithTransactionUri(trans_id));
        assertTrue("No items with that transaction", cursor.moveToFirst());
        STransaction transaction = new STransaction(cursor, true);
        cursor.close();
        for (STransaction.SAffectedItems affected : transaction.affectedItems) {
            assertTrue("Id not found", sItemIds.contains(affected.item.id));
        }
    }
}
