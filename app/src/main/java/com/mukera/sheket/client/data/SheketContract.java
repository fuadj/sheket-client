package com.mukera.sheket.client.data;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import com.mukera.sheket.client.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketContract {
    public static final String CONTENT_AUTHORITY = "com.mukera.sheket.client.app";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_COMPANY = "path_company";
    public static final String PATH_MEMBER = "path_member";
    public static final String PATH_BRANCH = "path_branch";
    public static final String PATH_BRANCH_CATEGORY = "path_branch_category";
    public static final String PATH_BRANCH_ITEM = "path_branch_item";
    public static final String PATH_CATEGORY = "path_category";
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

    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Converts Date class to a integer representation, used for easy comparison and database lookup.
     *
     * @param date The input date
     * @return a DB-friendly representation of the date, using the format defined in DATE_FORMAT.
     */
    public static int getDbDateInteger(Date date) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        return Integer.parseInt(sdf.format(date));
    }

    /**
     * Converts a dateText to a int Unix time representation
     *
     * @param dateInt the input date integer
     * @return the Date object
     */
    public static Date getDateFromDb(long dateInt) {
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        try {
            return dbDateFormat.parse("" + dateInt);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Since we are syncing with a backend, we will "overwrite" the local stuff with the fetched
     * data. But this might cause problems in the database when a conflict arises while inserting.
     * This conflict usually results in either being ignored(which we don't want, we want the updated
     * values to be added) or it will replace the local with the server version. Replacing also has
     * problems because a replace is a sequence of "delete - then - insert". When the deletion happens,
     * foreign key dependencies can be violates and un-known behaviour occurs.
     * <p/>
     * So, add this to your {@code ContentValues} when inserting if you want the behaviour to be
     * if already exist update, otherwise insert
     * This solves the problems stated above as it won't delete existing rows.
     */
    public static final String SQL_INSERT_OR_UPDATE = "__sql_insert_or_update__";

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
        /**
         * By setting the dummy company to be 0, we can guarantee that it won't
         * show up in ANY of the companies of a user because all of them have a non-zero id.
         */
        public static final int DUMMY_COMPANY_ID = 0;

        public static int getCompanyId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(1));
        }

        protected static Uri.Builder withBaseCompanyIdUri(Uri base_uri, int companyId) {
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

        public static final String COLUMN_COMPANY_ID = "_id";

        /**
         * This is used to identify the company when paying for the service. This is a {@code String}
         * value that is generated on the backend. We don't locally generate it from the
         * company id because that wouldn't allow us to update payment_id if we've found
         * a better way of encoding the id. So it is generated at the backend and any
         * upgrades can be applied remotely.
         */
        public static final String COLUMN_PAYMENT_ID = "payment_id";

        /**
         * This is only used for locally differentiating different users who
         * use the same phone to login to different companies.
         * We have the user id here and in no other place because controlling
         * the "visible" companies a user can be in controls the rest.(All others
         * have company_id which is it self controlled by which user is looking at
         * the data).
         */
        public static final String COLUMN_USER_ID = "user_id";

        public static final String COLUMN_NAME = "company_name";
        // the user's permission is stored as TEXT
        public static final String COLUMN_PERMISSION = "permission";

        // save state bkup stuff when switching between companies,
        // This was previously used to save revision number, that is why
        // the naming has stuck-ed.
        public static final String COLUMN_STATE_BACKUP = "state_bkup";

        // The signed payment that was generated for
        // {this device, company, user, ...} by the server is stored here
        // If this license isn't valid, the user isn't allowed to use the
        // company until the payment is verified.
        public static final String COLUMN_PAYMENT_LICENSE = "payment_license";

        // int, this holds the state of the payment issued for company.
        // can be one of PAYMENT_* constants.
        public static final String COLUMN_PAYMENT_STATE = "p_s";

        // use can continue using the app
        public static final int PAYMENT_VALID = 1;
        // payment has ended
        public static final int PAYMENT_ENDED = 2;
        // there was a problem with the payment (invalid certificate, date got messed-up, ...)
        public static final int PAYMENT_INVALID = 3;

        public static Uri buildCompanyUri(int id) {
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

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static Uri buildMemberUri(int company_id, int member_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).appendPath(
                    Long.toString(member_id)).build();
        }


        public static int getMemberId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }
    }

    public static final class CategoryEntry extends CompanyBase {
        /**
         * This is the parent of the root category, we had to create it b/c will be filtering categories
         * on their parent ids. And the parent id has a default of {@code ROOT_CATEGORY_ID}.
         * This means even the root category has a parent id of "self". And this might cause
         * the root category to be visible in searches, which is not allowed.
         * SO, create another "dummy" category for the root to refer it to as parent,
         * and this problem will be solved.
         */
        public static final int _ROOT_CATEGORY_PARENT_ID = -2;

        /**
         * The root category is the parent of the "first" level categories.
         * <p/>
         * IMPORTANT:
         * We can't use the "universal" -1 as the id because that is used by the database
         * to signal error.
         */
        public static final int ROOT_CATEGORY_ID = -3;

        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORY).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_CATEGORY;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_CATEGORY;

        public static final String TABLE_NAME = "category_table";

        public static final String COLUMN_CATEGORY_ID = "_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PARENT_ID = "parent_id";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        /**
         * IMPORTANT: when joining tables, you should alias the category table with this and
         * fully qualify every columns you want to select with it.
         */
        // TODO: // FIXME: 10/2/16  this is just a temporary HACK!!!!
        //public static final String PART_CURRENT = "current";
        public static final String PART_CURRENT = TABLE_NAME;

        // If you are using a query that also fetches children, this is that child part
        public static final String PART_CHILD = "child";

        public static String _fullCurrent(String col_name) {
            return PART_CURRENT + "." + col_name;
        }

        public static String _fullChild(String col_name) {
            return PART_CHILD + "." + col_name;
        }

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        private static String QUERY_PARAM_DON_T_FETCH_CHILDREN = "query_param_don_t_fetch_children";
        private static String PARAM_DONT_FETCH = "dont_fetch";
        // FIXME: this is just a hack, find proper way of implementing this
        // TODO: refactor sheket provider to have "current , child" category stuff.
        // make "current the default" and add option to fetch children.
        public static Uri buildBaseUriWithNoChildren(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendQueryParameter(QUERY_PARAM_DON_T_FETCH_CHILDREN, PARAM_DONT_FETCH).build();
        }

        public static boolean shouldNotFetchChildren(Uri uri) {
            String val = uri.getQueryParameter(QUERY_PARAM_DON_T_FETCH_CHILDREN);
            if (val != null && val.equals(PARAM_DONT_FETCH)) {
                return true;
            }
            return false;
        }

        public static Uri buildCategoryUri(int company_id, int category_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(category_id)).build();
        }

        public int getCategoryId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }
    }

    /**
     * Use to set different flags on entities.
     */
    public interface StatusTrackable {
        // set for "normal" mode, it should be defined as the DEFAULT.
        int STATUS_VISIBLE = 1;

        // use when you want to hide entities from UI. Used to simulate deletion without
        // actually removing data
        int STATUS_INVISIBLE = 2;

        /**
         * This column should be defined as an integer type.
         */
        String COLUMN_STATUS_FLAG = "status_flag";

        /**
         * Use this to send and receive when encoding it in JSON format.
         */
        String JSON_STATUS_FLAG = "status_flag";
    }

    public static final class BranchEntry extends CompanyBase implements StatusTrackable {
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
         * <p/>
         * This has the HUGE benefit of updating branch ids with the server ids when
         * syncing. This is only achieved when there is a foreign dependency on the
         * branch id and that has a "cascade on update" clause.
         * <p/>
         * IMPORTANT:
         * We can't user the "universal" -1 as the id because that is used by the database
         * to signal error.
         */
        public static final int DUMMY_BRANCH_ID = -2;

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

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static Uri buildBranchUri(int company_id, int id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).appendPath(
                    Long.toString(id)).build();
        }

        public static int getBranchId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }
    }

    public static final class BranchCategoryEntry extends CompanyBase {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_BRANCH_CATEGORY).
                        build();
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_CATEGORY;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_BRANCH_CATEGORY;

        public static final String TABLE_NAME = "branch_category_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_BRANCH_ID = "_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static final int NO_ID_SET = -1;

        /**
         * if you don't want to specify either { branch_id OR category_id },
         * you should set it to {@code NO_ID_SET}
         * e.g: (company_id, NO_ID_SET, category_id) when you don't specify the branch
         */
        public static Uri buildBranchCategoryUri(int company_id, int branch_id, int cateogry_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(branch_id)).
                    appendPath(Long.toString(cateogry_id)).build();
        }

        public static int getBranchId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }

        public static int getCategoryId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(3));
        }

        /**
         * Use this to check if an id is set
         */
        public static boolean isIdSpecified(Context context, int id) {
            int default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
            // if the id didn't still sync with the server, it will be -ve
            // then it needs to be equal or below default entity id
            return id <= default_id ||
                    id > 0L;
        }

        private static final String QUERY_CATEGORY_CHILDREN = "query_category_children";
        private static final String VALUE_FETCH_CHILDREN = "true";

        /**
         * An an optional query to fetch in all items even if they don't exist in the branch.
         *
         * @param uri The uri that is to be wrapped with this "query_none_existing" option.
         *            This should be a uri generated by one of {@code BranchItem} uri builder methods
         *            as it will be useless if it isn't.
         * @return
         */
        public static Uri buildFetchCategoryWithChildrenUri(Uri uri) {
            return uri.buildUpon().
                    appendQueryParameter(QUERY_CATEGORY_CHILDREN, VALUE_FETCH_CHILDREN).
                    build();
        }

        public static boolean isFetchingChildrenCategories(Uri uri) {
            String val = uri.getQueryParameter(QUERY_CATEGORY_CHILDREN);
            if (val != null && val.equals(VALUE_FETCH_CHILDREN)) {
                return true;
            }
            return false;
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

        // "_id" is the branch id, we have to name it "_id" for
        // cursor loaders to work
        public static final String COLUMN_BRANCH_ID = "_id";
        public static final String COLUMN_ITEM_ID = "item_id";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_ITEM_LOCATION = "item_location";

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static final int NO_ID_SET = -1;

        /**
         * if you don't want to specify either { branch_id OR item_id },
         * you should set it to {@code NO_ID_SET}
         * e.g: (company_id, NO_ID_SET, item_id) when you don't specify the branch
         */
        public static Uri buildBranchItemUri(int company_id, int branch_id, int item_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(branch_id)).
                    appendPath(Long.toString(item_id)).build();
        }

        public static int getBranchId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
        }

        public static int getItemId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(3));
        }

        /**
         * Use this to check is an id was set
         */
        public static boolean isIdSpecified(Context context, int id) {
            int default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
            // if the id didn't still sync with the server, it will be -ve
            // then it needs to be equal or below default entity id
            return id <= default_id ||
                    id > 0L;
        }

        /**
         * Helper methods to simplify uri creation
         */
        public static Uri buildAllItemsInBranchUri(int company_id, int branch_id) {
            return buildBranchItemUri(company_id, branch_id, NO_ID_SET);
        }

        public static Uri buildItemInAllBranches(int company_id, int item_id) {
            return buildBranchItemUri(company_id, NO_ID_SET, item_id);
        }

        private static final String FETCH_NONE_EXISTING_ITEMS = "query_none_existing_items";
        private static final String VALUE_NONE_EXISTING_ITEMS = "true";

        /**
         * An an optional query to fetch in all items even if they don't exist in the branch.
         *
         * @param uri The uri that is to be wrapped with this "query_none_existing" option.
         *            This should be a uri generated by one of {@code BranchItem} uri builder methods
         *            as it will be useless if it isn't.
         * @return
         */
        public static Uri buildFetchNoneExistingItemsUri(Uri uri) {
            return uri.buildUpon().
                    appendQueryParameter(FETCH_NONE_EXISTING_ITEMS, VALUE_NONE_EXISTING_ITEMS).build();
        }

        public static boolean isFetchNoneExistingItemsSpecified(Uri uri) {
            String val = uri.getQueryParameter(FETCH_NONE_EXISTING_ITEMS);
            if (val != null && val.equals(VALUE_NONE_EXISTING_ITEMS)) {
                return true;
            }
            return false;
        }
    }

    public static final class ItemEntry extends CompanyBase implements StatusTrackable {
        private static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + CONTENT_AUTHORITY + "/" + PATH_ITEM;

        public static final String TABLE_NAME = "inventory_table";

        public static String _full(String col_name) {
            return TABLE_NAME + "." + col_name;
        }

        public static final String COLUMN_ITEM_ID = "_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_ITEM_CODE = "item_code";
        public static final String COLUMN_NAME = "item_name";

        public static final String COLUMN_UNIT_OF_MEASUREMENT = "units";
        public static final String COLUMN_HAS_DERIVED_UNIT = "has_derived";
        public static final String COLUMN_DERIVED_UNIT_NAME = "derived_name";
        public static final String COLUMN_DERIVED_UNIT_FACTOR = "derived_factor";

        public static final String COLUMN_REORDER_LEVEL = "reorder_level";

        public static final String COLUMN_MODEL_YEAR = "column_model_year";
        public static final String COLUMN_PART_NUMBER = "column_part_number";
        public static final String COLUMN_BAR_CODE = "bar_code";
        public static final String COLUMN_HAS_BAR_CODE = "has_bar_code";

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        private static final String QUERY_BRANCH_SPECIFIED = "branch_specified";
        private static final String VALUE_BRANCH_SPECIFIED = "true";

        public static Uri buildBaseUriWithBranches(int company_id) {
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

        public static Uri buildItemUri(int company_id, int id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(id)).build();
        }

        public int getItemId(Uri uri) {
            return Integer.parseInt(uri.getPathSegments().get(2));
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

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        private static final String QUERY_ITEMS_SPECIFIED = "item_specified";
        private static final String VALUE_ITEMS_SPECIFIED = "true";

        public static Uri buildBaseUriWithItems(int company_id) {
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
        public static final String COLUMN_TRANS_NOTE = "trans_note";

        public static Uri buildTransactionUri(int company_id, long trans_id) {
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
        public static final String COLUMN_ITEM_NOTE = "item_note";

        /**
         * Transaction Types
         */
        // These are for increasing stock
        public static final int TYPE_INCREASE_PURCHASE = 1;
        public static final int TYPE_INCREASE_RETURN_ITEM = 2;
        public static final int TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH = 3;

        // These are for decreasing stock
        public static final int TYPE_DECREASE_CURRENT_BRANCH = 11;
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
                case TYPE_INCREASE_PURCHASE:
                    return "Buy";
                case TYPE_INCREASE_RETURN_ITEM:
                    return "Returned";
                case TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                    return "Receive";

                case TYPE_DECREASE_CURRENT_BRANCH:
                    return "Sell";
                case TYPE_DECREASE_TRANSFER_TO_OTHER:
                    return "Send";
            }
            return "Undefined TransType";
        }

        public static Uri buildBaseUri(int company_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).build();
        }

        public static final long NO_TRANS_ID_SET = -1;

        /**
         * pass in {@code NO_TRANS_ID_SET} as trans_id to search all transactions
         * in a company.
         *
         * @param company_id
         * @param trans_id
         * @return
         */
        public static Uri buildTransactionItemsUri(int company_id, long trans_id) {
            return withBaseCompanyIdUri(CONTENT_URI, company_id).
                    appendPath(Long.toString(trans_id)).build();
        }

        public static long getTransactionId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(2));
        }

        /**
         * Use this to check is an id was set
         */
        public static boolean isTransactionIdSet(Context context, long id) {
            int default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
            // if the id didn't still sync we the server, it will be -ve
            // then it needs to be equal or below default entity id
            return id <= default_id ||
                    id > 0L;
        }
    }
}
