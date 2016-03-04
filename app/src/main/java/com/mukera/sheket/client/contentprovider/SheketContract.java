package com.mukera.sheket.client.contentprovider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketContract {
    public static final String CONTENT_AUTHORITY = "com.mukera.sheket.client.app";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_CATEGORY = "path_category";
    public static final String PATH_TRANSACTION = "path_transaction";
    public static final String PATH_AFFECTED_ITEMS = "path_affected_items";
    public static final String PATH_ITEM = "path_item";

    public static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";


    public static final class CategoryEntry implements BaseColumns {
        // This is a 'virtual' category, it doesn't actually exist in the db,
        // Just a common constant
        public static final int DEFAULT_CATEGORY_ID = -1;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORY).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_CATEGORY;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_CATEGORY;

        public static final String TABLE_NAME = "category";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        //public static final String _ID = "category_id";
        public static final String COLUMN_NAME = "category_name";

        public static Uri buildCategoryUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class TransactionEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANSACTION).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;

        public static final String TABLE_NAME = "transaction_table";

        //public static final String _ID = "transaction_id";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final int TRANS_TYPE_BUY = 1;
        public static final int TRANS_TYPE_SELL = 2;

        // The Type of transaction as defined in the constants above
        public static final String COLUMN_TYPE = "transaction_type";

        public static final String COLUMN_DATE = "date";

        public static final String COLUMN_QTY_TOTAL = "qty_total";

        public static Uri buildTransactionUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class AffectedItemEntry implements BaseColumns{
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_AFFECTED_ITEMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_AFFECTED_ITEMS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_AFFECTED_ITEMS;

        public static final String TABLE_NAME = "affected_items";
        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_TRANSACTION_ID = "trans_id";
        public static final String COLUMN_ITEM_ID = "item_id";
        public static final String COLUMN_QTY = "quantity";

        public static final String TRANSACTION_PATH_SEGMENT = "transaction";

        public static Uri buildAffectedItemsWithTransactionUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(TRANSACTION_PATH_SEGMENT)
                    .appendPath(Long.toString(id)).build();
        }

        public static long getTransactionIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class ItemEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;

        public static final String TABLE_NAME = "inventory_items";

        //public static final String _ID = "item_id";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_NAME = "coupon_name";
        public static final String COLUMN_LOCATION = "location";
        public static final String COLUMN_DESC = "desc";
        public static final String COLUMN_QTY_REMAIN = "qty_remain";

        public static final int CODE_TYPE_BAR_CODE = 1;
        public static final int CODE_TYPE_MANUAL = 2;
        public static final int CODE_TYPE_BOTH = 3;

        public static final String COLUMN_CODE_TYPE = "code_type";

        public static final String COLUMN_BAR_CODE = "bar_code";
        public static final String COLUMN_MANUAL_CODE = "manual_code";

        public static final String CATEGORY_PATH_SEGMENT = "category";

        public static Uri buildItemUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildItemWithCategoryId(long id) {
            return CONTENT_URI.buildUpon().appendPath(CATEGORY_PATH_SEGMENT)
                    .appendPath(Long.toString(id)).build();
        }

        public static long getCategoryIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }
}
