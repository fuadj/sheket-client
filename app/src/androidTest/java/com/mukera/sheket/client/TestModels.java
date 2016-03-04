package com.mukera.sheket.client;

import android.content.ContentValues;
import android.test.AndroidTestCase;

import com.mukera.sheket.client.contentprovider.SheketContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/3/16.
 */
public class TestModels extends AndroidTestCase {

    public static List<ContentValues> createExtendedAffectedValues(long item_id, long trans_id) {
        double[] qtyArr = {23, 43, 7, 0, 12, 54, 75};
        List<ContentValues> result = new ArrayList<>();

        for (double qty : qtyArr) {
            ContentValues value = TestProvider.createAffectedValues(item_id, trans_id);
            value.put(SheketContract.AffectedItemEntry.COLUMN_QTY, qty);
            result.add(value);
        }
        return result;
    }

    @Override
    protected void setUp() throws Exception {
        TestProvider.deleteAllRecords(mContext);
    }

    void testModelCursorRetrival() {

    }
}
