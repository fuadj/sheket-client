package com.mukera.sheket.client.data;

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

    public static final String PATH_COMPANY = "path_company";
    public static final String PATH_BRANCH = "path_branch";
    public static final String PATH_BRANCH_ITEM = "path_branch_item";
    public static final String PATH_ITEM = "path_item";
    public static final String PATH_TRANSACTION = "path_transaction";
    public static final String PATH_TRANS_ITEMS = "path_trans_items";

    // Since the db doesn't support boolean, use these instead!!!
    public static final int TRUE = 1;
    public static final int FALSE = 0;

    public static final boolean toBool(int val) {
        return val == TRUE;
    }

    public static final int toInt(boolean val) {
        return (val ? TRUE : FALSE);
    }

    public static String formatDate(long dateInMilliseconds) {
        Date date = new Date(dateInMilliseconds);
        return DateFormat.getDateInstance().format(date);
    }

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    // add this to contentvalues and set it to true if you want the insert to replace existing stuff.
    public static final String SQL_INSERT_OR_REPLACE = "__sql_insert_or_replace__";

    public static class ChangeTraceable {
        // this flag is used while syncing to see what are the changes
        // since last sync.
        public static final String COLUMN_CHANGE_INDICATOR = "change_status_indicator";

        public static final int CHANGE_STATUS_SYNCED = 1;
        public static final int CHANGE_STATUS_CREATED = 2;
        public static final int CHANGE_STATUS_UPDATED = 3;
        public static final int CHANGE_STATUS_DELETED = 4;

        public int change_status;
    }

    public interface CompanyBase {
        String COLUMN_COMPANY_ID = "company_id";
    }

    public static final class CompanyEntry {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_COMPANY).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_COMPANY;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_COMPANY;

        public static final String TABLE_NAME = "company_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_NAME = "company_name";

        public static Uri buildCompanyUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class BranchEntry implements CompanyBase {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BRANCH).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH;

        public static final String TABLE_NAME = "branch_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_BRANCH_ID = "_id";
        public static final String COLUMN_NAME = "branch_name";
        public static final String COLUMN_LOCATION = "branch_location";

        public static Uri buildBranchUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class BranchItemEntry implements CompanyBase {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BRANCH_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_ITEM;

        public static final String TABLE_NAME = "branch_item_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_BRANCH_ID = "branch_id";
        public static final String COLUMN_ITEM_ID = "item_id";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_ITEM_LOCATION = "item_location";

        public static final String ITEM_PATH_SEGMENT = "item";
        public static final String BRANCH_PATH_SEGMENT = "branch";
        public static final String BRANCH_ITEM_PATH_SEGMENT = "branch_item";

        /**
         * Uri with both {@code branch_id} AND {@code item_id} specified
         */
        public static Uri buildBranchItemUri(long branch_id, long item_id) {
            return CONTENT_URI.buildUpon().appendPath(BRANCH_ITEM_PATH_SEGMENT).
                    appendPath(Long.toString(branch_id)).
                    appendPath(Long.toString(item_id)).build();
        }

        /**
         * Parse out {@code branch_id} from {@code buildBranchItemUri()} generated uri
         */
        public static long getBranchIdFromBranchItemUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        /**
         * Parse out {@code item_id} from {@code buildBranchItemUri()} generated uri
         */
        public static long getItemIdFromBranchItemUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(3));
        }

        /**
         * Uri with {@code branch_id} specified
         */
        public static Uri buildItemsInBranchUri(long branch_id) {
            return CONTENT_URI.buildUpon().appendPath(BRANCH_PATH_SEGMENT).
                    appendPath(Long.toString(branch_id)).build();
        }

        /**
         * Parse out {@code branch_id} from {@code buildItemsInBranchUri(branch_id)} generated uri
         */
        public static long getBranchIdFromBranchUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        /**
         * Uri with {@code item_id} specified
         */
        public static Uri buildItemInAllBranchesUri(long item_id) {
            return CONTENT_URI.buildUpon().appendPath(ITEM_PATH_SEGMENT).
                    appendPath(Long.toString(item_id)).build();
        }

        /**
         * Parse out {@code item_id} from {@code buildItemInAllBranchesUri(item_id)} generated uri
         */
        public static long getItemIdFromItemUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class ItemEntry implements CompanyBase {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;

        public static final String TABLE_NAME = "inventory_items";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_ITEM_ID = "_id";
        public static final String COLUMN_NAME = "coupon_name";
        public static final String COLUMN_MODEL_YEAR = "column_model_year";
        public static final String COLUMN_PART_NUMBER = "column_part_number";
        public static final String COLUMN_BAR_CODE = "bar_code";
        public static final String COLUMN_HAS_BAR_CODE = "has_bar_code";
        public static final String COLUMN_MANUAL_CODE = "manual_code";

        public static Uri buildItemUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class TransactionEntry implements CompanyBase {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANSACTION).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;

        public static final String TABLE_NAME = "transaction_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_TRANS_ID = "_id";
        public static final String COLUMN_USER_ID = "column_user_id";
        public static final String COLUMN_BRANCH_ID = "column_branch_id";
        public static final String COLUMN_DATE = "date";

        public static Uri buildTransactionUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class TransItemEntry implements CompanyBase {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANS_ITEMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_TRANS_ITEMS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_TRANS_ITEMS;

        public static final String TABLE_NAME = "trans_item_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_TRANSACTION_ID = "column_transaction_id";
        public static final String COLUMN_TRANSACTION_TYPE = "column_transaction_type";
        public static final String COLUMN_ITEM_ID = "column_item_id";
        public static final String COLUMN_OTHER_BRANCH_ID = "column_other_branch_id";
        public static final String COLUMN_QTY = "quantity";

        /**
         * Transaction Types
         */
        // These are for increasing stock
        public static final int TRANS_TYPE_PURCHASE_FROM_MARKET = 1;
        public static final int TRANS_TYPE_RESUPPLY_FROM_OTHER_BRANCH = 2;

        // These are for decreasing stock
        public static final int TRANS_TYPE_SELL_FROM_CURRENT_BRANCH = 10;
        public static final int TRANS_TYPE_SELL_FROM_OTHER_BRANCH = 11;
        public static final int TRANS_TYPE_SELL_FROM_MARKET_DIRECTLY = 12;

        public static Uri buildItemsInTransactionUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(id)).build();
        }
    }
}
