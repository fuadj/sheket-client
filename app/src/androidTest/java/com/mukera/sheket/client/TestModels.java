package com.mukera.sheket.client;


import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/3/16.
 */
public class TestModels extends AndroidTestCase {
    private static List<Integer> sItemIds = null;

    void setupCompany() {
        ContentValues values = TestProvider.createCompanyValues();
        mContext.getContentResolver().insert(CompanyEntry.CONTENT_URI, values);
    }

    void setupItems() {
        String[] codeArr = {"lfakd", "JM-009", "784596", "12w3"};
        String[] locArr = {"A09", "B23", "C21"};
        double[] qtyArr = {0.0, 100.0, -30.0, 0.0023};

        sItemIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ContentValues values = TestProvider.createItemValues();
            values.put(ItemEntry.COLUMN_HAS_BAR_CODE, SheketContract.toInt(i % 2 == 0));
            values.put(ItemEntry.COLUMN_ITEM_CODE,
                    codeArr[((i*13) + 3)%codeArr.length]);      // a bit if "randomness"

            int item_id = i + 1;
            values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
            Uri uri = mContext.getContentResolver().insert(
                    ItemEntry.buildBaseUri(TestProvider.TEST_COMPANY_ID), values);
            assertEquals("Inserted and got back item not same", item_id, ContentUris.parseId(uri));

            sItemIds.add(item_id);
        }
    }

    @Override
    protected void setUp() throws Exception {
        TestProvider.deleteAllRecords(mContext);
        setupCompany();
        setupItems();
    }

    Cursor _query(Uri uri, String[] projection) {
        return mContext.getContentResolver().query(uri,
                projection, null, null, null);
    }

    public void testModelCursorRetrieval() {
        checkItemModel();
        checkTransactionModel();
    }

    void checkItemModel() {
        ContentValues values = TestProvider.createItemValues();
        int item_id = 1000;
        values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
        Uri uri = mContext.getContentResolver().insert(
                ItemEntry.buildBaseUri(TestProvider.TEST_COMPANY_ID), values);
        assertEquals("Insert item error", item_id, ContentUris.parseId(uri));

        Cursor cursor = _query(ItemEntry.buildItemUri(TestProvider.TEST_COMPANY_ID, item_id), SItem.ITEM_COLUMNS);
        assertTrue("No value in cursor", cursor.moveToFirst());
        SItem item = new SItem(cursor);
        assertEquals("Fetch item error", item.item_id, item_id);
        assertEquals(item.name, values.getAsString(ItemEntry.COLUMN_NAME));
        assertEquals(item.bar_code, values.getAsString(ItemEntry.COLUMN_BAR_CODE));
        cursor.close();
    }

    void checkTransactionModel() {
        ContentValues transactionValues = TestProvider.createTransactionValues();
        Uri uri = mContext.getContentResolver().insert(
                TransactionEntry.buildBaseUri(TestProvider.TEST_COMPANY_ID), transactionValues);
        long trans_id = ContentUris.parseId(uri);
        assertTrue(trans_id != -1);

        int n = (int)(Math.min(0.3, Math.random()) * sItemIds.size());      // add at-least 30% of items
        List<ContentValues> values = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            values.add(TestProvider.createTransactionItems(sItemIds.get(i), trans_id));
        }

        ContentValues[] valuesArr = new ContentValues[values.size()];
        values.toArray(valuesArr);
        int count = mContext.getContentResolver().bulkInsert(TransItemEntry.buildBaseUri(TestProvider.TEST_COMPANY_ID),
                valuesArr);
        assertEquals(count, values.size());

        Cursor cursor = _query(TransItemEntry.buildTransactionItemsUri(TestProvider.TEST_COMPANY_ID, trans_id),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS);
        assertTrue("No items with that transaction", cursor.moveToFirst());
        STransaction transaction = new STransaction(cursor, true);
        cursor.close();
        for (STransaction.STransactionItem affected : transaction.transactionItems) {
            assertTrue("Id not found", sItemIds.contains(affected.item.item_id));
        }
    }

    public void testModelParcelable() {
    }
}
