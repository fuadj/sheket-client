package com.mukera.sheket.client.data;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import com.mukera.sheket.client.R;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketContract {
    public static final String CONTENT_AUTHORITY = "com.mukera.sheket.client.app";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_COMPANY = "path_company";
    public static final String PATH_MEMBER = "path_member";
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

    public static class UUIDSyncable extends ChangeTraceable {
        public static final String COLUMN_UUID = "client_uuid";

        public String client_uuid;
    }

    public static abstract class CompanyBase {
        public static final String COLUMN_COMPANY_ID = "company_id";

        public static long getCompanyId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        protected static Uri.Builder withBaseCompanyIdUri(Uri base_uri, long companyId) {
            return base_uri.buildUpon().appendPath(Long.toString(companyId));
        }
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
        // the user's permission is stored as TEXT
        public static final String COLUMN_PERMISSION = "permission";

        // holds revisions related to the company
        public static final String COLUMN_REVISIONS = "revision";

        public static Uri buildCompanyUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class MemberEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MEMBER).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_MEMBER;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_MEMBER;

        public static final String TABLE_NAME = "members_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_MEMBER_ID = "_id";
        public static final String COLUMN_MEMBER_NAME = "member_name";
        public static final String COLUMN_MEMBER_PERMISSION = "member_permission";

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static Uri buildMemberUri(long company_id, long member_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).appendPath(
                    Long.toString(member_id)).build();
        }


        public static long getMemberId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class BranchEntry extends CompanyBase {
        /**
         * This is the branch id used in transactions when the transaction
         * doesn't involve another branch. This dummy branch is created in
         * the database and can't be viewed by users. This was needed to be created
         * to allow transaction tables to have a foreign key dependency on the
         * branches table. It was not possible to create the foreign key
         * because if a transaction doesn't affect another branch, we can't put
         * a non existing branch id on a column that has foreign key dependency.
         * So, this dummy branch serves the purpose of the "null" branch where
         * transaction that don't affect another branch point to this branch.
         *
         * This has the HUGE benefit of updating branch ids with the server ids when
         * syncing. This is only achieved when there is a foreign dependency on the
         * branch id and that has a "cascade on update" clause.
         */
        public static final long DUMMY_BRANCH_ID = -2;
        /**
         * By setting the dummy branch's company to be 0, we can guarantee that it won't
         * show up in ANY of the companies of a user because all of them have a non-zero id.
         */
        public static final long DUMMY_COMPANY_ID = 0;

        private static final Uri CONTENT_URI =
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

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static Uri buildBranchUri(long company_id, long id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).appendPath(
                    Long.toString(id)).build();
        }

        public static long getBranchId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class BranchItemEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BRANCH_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_ITEM;

        public static final String TABLE_NAME = "branch_item_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        // it is named "_id" to allow {@code CursorLoader} to work
        public static final String COLUMN_BRANCH_ID = "_id";
        public static final String COLUMN_ITEM_ID = "item_id";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_ITEM_LOCATION = "item_location";

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static final long NO_ID_SET = -1;
        /**
         * if you don't want to specify either { branch_id OR item_id },
         * you should set it to {@code NO_ID_SET}
         * e.g: (company_id, NO_ID_SET, item_id) when you don't specify the branch
         */
        public static Uri buildBranchItemUri(long company_id, long branch_id, long item_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(branch_id)).
                    appendPath(Long.toString(item_id)).build();
        }

        public static long getBranchId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static long getItemId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(3));
        }

        /**
         * Use this to check is an id was set
         */
        public static boolean isIdSpecified(Context context, long id) {
            long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
            // if the id didn't still sync we the server, it will be -ve
            // then it needs to be equal or below default entity id
            return id <= default_id ||
                    id > 0L;
        }

        /**
         * Helper methods to simplify uri creation
         */
        public static Uri buildAllItemsInBranchUri(long company_id, long branch_id) {
            return buildBranchItemUri(company_id, branch_id, NO_ID_SET);
        }

        public static Uri buildItemInAllBranches(long company_id, long item_id) {
            return buildBranchItemUri(company_id, NO_ID_SET, item_id);
        }
    }

    public static final class ItemEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
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
        public static final String COLUMN_NAME = "item_name";
        public static final String COLUMN_MODEL_YEAR = "column_model_year";
        public static final String COLUMN_PART_NUMBER = "column_part_number";
        public static final String COLUMN_BAR_CODE = "bar_code";
        public static final String COLUMN_HAS_BAR_CODE = "has_bar_code";
        public static final String COLUMN_MANUAL_CODE = "manual_code";

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        private static final String QUERY_BRANCH_SPECIFIED = "branch_specified";
        private static final String VALUE_BRANCH_SPECIFIED = "true";
        public static Uri buildBaseUriWithBranches(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendQueryParameter(QUERY_BRANCH_SPECIFIED, VALUE_BRANCH_SPECIFIED).
                    build();
        }

        public static boolean isBranchesSpecified(Uri uri) {
            String val = uri.getQueryParameter(QUERY_BRANCH_SPECIFIED);
            if (val != null && val.equals(VALUE_BRANCH_SPECIFIED)) {
                return true;
            }
            return false;
        }

        public static Uri buildItemUri(long company_id, long id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(id)).build();
        }

        public long getItemId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class TransactionEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANSACTION).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_TRANSACTION;

        public static final String TABLE_NAME = "transaction_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        private static final String QUERY_ITEMS_SPECIFIED = "item_specified";
        private static final String VALUE_ITEMS_SPECIFIED = "true";
        public static Uri buildBaseUriWithItems(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendQueryParameter(QUERY_ITEMS_SPECIFIED, VALUE_ITEMS_SPECIFIED).
                    build();
        }

        public static boolean isItemsSpecified(Uri uri) {
            String val = uri.getQueryParameter(QUERY_ITEMS_SPECIFIED);
            if (val != null && val.equals(VALUE_ITEMS_SPECIFIED)) {
                return true;
            }
            return false;
        }

        public static final String COLUMN_TRANS_ID = "_id";
        public static final String COLUMN_USER_ID = "column_user_id";
        public static final String COLUMN_BRANCH_ID = "column_branch_id";
        public static final String COLUMN_DATE = "date";

        public static Uri buildTransactionUri(long company_id, long trans_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(trans_id)).build();
        }

        public static long getTransactionId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }
    }

    public static final class TransItemEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRANS_ITEMS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_TRANS_ITEMS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_TRANS_ITEMS;

        public static final String TABLE_NAME = "trans_item_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_TRANSACTION_ID = "_id";
        public static final String COLUMN_TRANSACTION_TYPE = "column_transaction_type";
        public static final String COLUMN_ITEM_ID = "column_item_id";
        public static final String COLUMN_OTHER_BRANCH_ID = "column_other_branch_id";
        public static final String COLUMN_QTY = "quantity";

        /**
         * Transaction Types
         */
        // These are for increasing stock
        public static final int TYPE_INCREASE_PURCHASE = 1;
        public static final int TYPE_INCREASE_RETURN_ITEM = 2;
        public static final int TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH = 3;

        // These are for decreasing stock
        public static final int TYPE_DECREASE_CURRENT_BRANCH = 10;
        public static final int TYPE_DECREASE_DIRECT_PURCHASE = 11;
        public static final int TYPE_DECREASE_TRANSFER_TO_OTHER = 12;

        public static boolean isIncrease(int trans_type) {
            switch (trans_type) {
                case TYPE_INCREASE_PURCHASE:
                case TYPE_INCREASE_RETURN_ITEM:
                case TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                    return true;
                default:
                    return false;
            }
        }

        public static String getStringForm(int trans_type) {
            switch (trans_type) {
                case TYPE_INCREASE_PURCHASE: return "Purchase";
                case TYPE_INCREASE_RETURN_ITEM: return "Returned";
                case TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH: return "Transfer";

                case TYPE_DECREASE_CURRENT_BRANCH: return "Sell Branch Item";
                case TYPE_DECREASE_DIRECT_PURCHASE: return "Sell Direct Purchase";
                case TYPE_DECREASE_TRANSFER_TO_OTHER: return "Transfer";
            }
            return "Undefined TransType";
        }

        public static Uri buildBaseUri(long company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        /**
         * pass in a -ve trans_id to search all transactions with corresponding items
         */
        public static Uri buildTransactionItemsUri(long company_id, long trans_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(trans_id)).build();
        }

        public static long getTransactionId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        public static final long NO_TRANS_ID_SET = -1;

        /**
         * Use this to check is an id was set
         */
        public static boolean isTransactionIdSet(Context context, long id) {
            long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
            // if the id didn't still sync we the server, it will be -ve
            // then it needs to be equal or below default entity id
            return id <= default_id ||
                    id > 0L;
        }
    }
}
