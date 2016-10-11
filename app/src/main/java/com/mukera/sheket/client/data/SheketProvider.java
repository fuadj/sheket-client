package com.mukera.sheket.client.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mukera.sheket.client.data.SheketContract.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private SheketDbHelper mDbHelper;

    private static final int COMPANY = 100;
    private static final int COMPANY_WITH_ID = 101;

    private static final int BRANCH = 200;
    private static final int BRANCH_WITH_ID = 201;

    private static final int BRANCH_ITEM = 300;
    private static final int BRANCH_ITEM_WITH_ID = 301;

    private static final int ITEM = 400;
    private static final int ITEM_WITH_ID = 401;

    private static final int TRANSACTION = 500;
    private static final int TRANSACTION_WITH_ID = 501;

    private static final int TRANSACTION_ITEM = 600;
    private static final int TRANSACTION_ITEM_WITH_TRANSACTION_ID = 601;

    private static final int MEMBER = 700;
    private static final int MEMBER_WITH_ID = 701;

    private static final int CATEGORY = 800;
    private static final int CATEGORY_WITH_ID = 801;

    private static final int BRANCH_CATEGORY = 900;
    private static final int BRANCH_CATEGORY_WITH_ID = 901;

    private static final SQLiteQueryBuilder sTransactionItemsWithTransactionIdAndItemDetailQueryBuilder;
    private static final SQLiteQueryBuilder sBranchItemFetchOnlyExistingItemQueryBuilder;
    private static final SQLiteQueryBuilder sBranchCategoryQueryBuilder;
    private static final SQLiteQueryBuilder sBranchCategoryWithCategoryChildrenQueryBuilder;
    private static final SQLiteQueryBuilder sItemWithBranchQueryBuilder;
    private static final SQLiteQueryBuilder sCategoryWithChildrenQueryBuilder;

    static {
        sTransactionItemsWithTransactionIdAndItemDetailQueryBuilder = new SQLiteQueryBuilder();
        sTransactionItemsWithTransactionIdAndItemDetailQueryBuilder.setTables(
                TransactionEntry.TABLE_NAME + " INNER JOIN " + TransItemEntry.TABLE_NAME +
                        " ON (" +
                        TransItemEntry._full(TransItemEntry.COLUMN_TRANSACTION_ID) +
                        " = " +
                        TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID) + ") " +
                        " INNER JOIN " + ItemEntry.TABLE_NAME +
                        " ON (" +
                        TransItemEntry._full(TransItemEntry.COLUMN_ITEM_ID) +
                        " = " +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + ")"
        );

        sBranchItemFetchOnlyExistingItemQueryBuilder = new SQLiteQueryBuilder();
        sBranchItemFetchOnlyExistingItemQueryBuilder.setTables(
                BranchItemEntry.TABLE_NAME + " INNER JOIN " + ItemEntry.TABLE_NAME +
                        " ON ( " +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) +
                        " = " +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + ") "
        );

        sItemWithBranchQueryBuilder = new SQLiteQueryBuilder();
        sItemWithBranchQueryBuilder.setTables(
                ItemEntry.TABLE_NAME + " LEFT JOIN " +
                        BranchItemEntry.TABLE_NAME + " ON (" +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = " + BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) + ") " +
                        " LEFT JOIN " +
                        BranchEntry.TABLE_NAME + " ON ( " +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID) + " = " + BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + ")"
        );
        sCategoryWithChildrenQueryBuilder = new SQLiteQueryBuilder();
        sCategoryWithChildrenQueryBuilder.setTables(
                String.format(Locale.US, "%s AS %s LEFT JOIN %s AS %s ON %s = %s",
                        CategoryEntry.TABLE_NAME,
                        CategoryEntry.PART_CURRENT,
                        CategoryEntry.TABLE_NAME,
                        CategoryEntry.PART_CHILD,
                        CategoryEntry._fullChild(CategoryEntry.COLUMN_PARENT_ID),
                        CategoryEntry._fullCurrent(CategoryEntry.COLUMN_CATEGORY_ID))
        );

        sBranchCategoryQueryBuilder = new SQLiteQueryBuilder();
        sBranchCategoryQueryBuilder.setTables(
                String.format(Locale.US,
                        "%s %s INNER JOIN %s ON %s = %s",
                        CategoryEntry.TABLE_NAME,
                        CategoryEntry.PART_CURRENT,
                        BranchCategoryEntry.TABLE_NAME,
                        CategoryEntry._fullCurrent(CategoryEntry.COLUMN_CATEGORY_ID),
                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID))
        );

        sBranchCategoryWithCategoryChildrenQueryBuilder = new SQLiteQueryBuilder();
        sBranchCategoryWithCategoryChildrenQueryBuilder.setTables(
                /**
                 * We are aliasing the Category table so we can use the {@code SCategory}'s
                 * projection. See {@code SCategory} for detail.
                 *
                 * We are fetching the category WITH ITS CHILDREN, that is why we are doing
                 * a left join of category with itself.
                 */
                String.format(Locale.US,
                        " (%s) INNER JOIN %s ON %s = %s",
                        String.format(Locale.US, "%s AS %s LEFT JOIN %s AS %s ON %s = %s",
                                CategoryEntry.TABLE_NAME,
                                CategoryEntry.PART_CURRENT,
                                CategoryEntry.TABLE_NAME,
                                CategoryEntry.PART_CHILD,
                                CategoryEntry._fullChild(CategoryEntry.COLUMN_PARENT_ID),
                                CategoryEntry._fullCurrent(CategoryEntry.COLUMN_CATEGORY_ID)),
                        BranchCategoryEntry.TABLE_NAME,
                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID),
                        CategoryEntry._fullCurrent(CategoryEntry.COLUMN_CATEGORY_ID))
        );
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SheketContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, SheketContract.PATH_COMPANY, COMPANY);
        /**
         * IMPORTANT: '#' have been replaced with '*' to allow matching -ve numbers.
         * This might create a bug matching non-number stuff(like text)
         */
        matcher.addURI(authority, SheketContract.PATH_COMPANY + "/*", COMPANY_WITH_ID);

        /**
         * All of the first segment of "/*" is the company id
         */
        matcher.addURI(authority, SheketContract.PATH_BRANCH + "/*", BRANCH);
        matcher.addURI(authority, SheketContract.PATH_BRANCH + "/*/*", BRANCH_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM + "/*", BRANCH_ITEM);
        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM + "/*/*/*", BRANCH_ITEM_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_ITEM + "/*", ITEM);
        matcher.addURI(authority, SheketContract.PATH_ITEM + "/*/*", ITEM_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_TRANSACTION + "/*", TRANSACTION);
        matcher.addURI(authority, SheketContract.PATH_TRANSACTION + "/*/*", TRANSACTION_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_TRANS_ITEMS + "/*", TRANSACTION_ITEM);
        matcher.addURI(authority, SheketContract.PATH_TRANS_ITEMS + "/*/*",
                TRANSACTION_ITEM_WITH_TRANSACTION_ID);

        matcher.addURI(authority, SheketContract.PATH_MEMBER + "/*", MEMBER);
        matcher.addURI(authority, SheketContract.PATH_MEMBER + "/*/*", MEMBER_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_CATEGORY + "/*", CATEGORY);
        matcher.addURI(authority, SheketContract.PATH_CATEGORY + "/*/*", CATEGORY_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_BRANCH_CATEGORY + "/*", BRANCH_CATEGORY);
        matcher.addURI(authority, SheketContract.PATH_BRANCH_CATEGORY + "/*/*/*", BRANCH_CATEGORY_WITH_ID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SheketDbHelper(getContext());
        return true;
    }

    private boolean is_empty(String s) {
        if (TextUtils.isEmpty(s)) return true;
        return s.trim().isEmpty();
    }

    String withAppendedSelection(String prev_selection, String new_selection) {
        if (is_empty(prev_selection))
            return new_selection;
        if (is_empty(new_selection))
            return prev_selection;
        return prev_selection + " AND " + new_selection;
    }

    String[] withAppendedSelectionArgs(String[] prev_args, String[] new_args) {
        if (new_args == null) return prev_args;
        if (prev_args == null) return new_args;

        String[] combined_args = new String[prev_args.length + new_args.length];
        System.arraycopy(prev_args, 0, combined_args, 0, prev_args.length);
        System.arraycopy(new_args, 0, combined_args, prev_args.length, new_args.length);
        return combined_args;
    }

    String withAppendedCompanyIdSelection(String selection, String col_company_id) {
        return withAppendedSelection(selection, col_company_id + " = ?");
    }

    String[] withAppendedCompanyIdSelectionArgs(String[] args, long company_id) {
        String[] company_args = new String[]{Long.toString(company_id)};
        return withAppendedSelectionArgs(args, company_args);
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor result = null;
        String tableName = null;

        boolean query_db = true, append_company_id = true;
        int uri_match = sUriMatcher.match(uri);

        long company_id = -1;
        if (uri_match != COMPANY) { // this is the only time a company_id won't be specified
            company_id = CompanyBase.getCompanyId(uri);
        }
        String column_company_id = null;

        switch (uri_match) {
            case COMPANY_WITH_ID:
            case COMPANY: {
                tableName = CompanyEntry.TABLE_NAME;
                if (uri_match == COMPANY_WITH_ID) {
                    selection = CompanyEntry.COLUMN_COMPANY_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                append_company_id = false;
                break;
            }

            case MEMBER_WITH_ID:
            case MEMBER: {
                tableName = MemberEntry.TABLE_NAME;
                if (uri_match == MEMBER_WITH_ID) {
                    selection = MemberEntry.COLUMN_MEMBER_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                column_company_id = MemberEntry._full(MemberEntry.COLUMN_COMPANY_ID);
                break;
            }

            case CATEGORY_WITH_ID:
            case CATEGORY: {
                boolean should_fetch_children = !CategoryEntry.shouldNotFetchChildren(uri);
                if (should_fetch_children) {
                    query_db = false;
                    tableName = CategoryEntry.TABLE_NAME;
                    if (uri_match == CATEGORY_WITH_ID) {
                        selection = withAppendedSelection(selection,
                                CategoryEntry.COLUMN_CATEGORY_ID + " = ' " + ContentUris.parseId(uri) + " ' ");
                        selectionArgs = withAppendedSelectionArgs(selectionArgs, null);
                    }

                    selection = withAppendedCompanyIdSelection(selection,
                            CategoryEntry._fullCurrent(CategoryEntry.COLUMN_COMPANY_ID));
                    selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs,
                            company_id);

                    result = sCategoryWithChildrenQueryBuilder.query(
                            mDbHelper.getReadableDatabase(),
                            projection,
                            selection, selectionArgs,
                            null, null, sortOrder);
                } else {
                    tableName = CategoryEntry.TABLE_NAME;
                    if (uri_match == CATEGORY_WITH_ID) {
                        selection = CategoryEntry.COLUMN_CATEGORY_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                        selectionArgs = null;
                    }
                    column_company_id = CategoryEntry._full(CategoryEntry.COLUMN_COMPANY_ID);
                }
                break;
            }

            case BRANCH_WITH_ID:
            case BRANCH: {
                tableName = BranchEntry.TABLE_NAME;
                if (uri_match == BRANCH_WITH_ID) {
                    selection = BranchEntry.COLUMN_BRANCH_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                column_company_id = BranchEntry._full(BranchEntry.COLUMN_COMPANY_ID);
                break;
            }

            case BRANCH_ITEM_WITH_ID:
            case BRANCH_ITEM: {
                query_db = false;

                // Should we also fetch items that don't exist inside the branch
                boolean fetch_none_branch_items =
                        BranchItemEntry.isFetchNoneExistingItemsSpecified(uri);

                // should we fetch a particular item in ALL branches, even if it
                // doesn't exist in them
                boolean fetch_item_in_all_branches = false;

                int branch_id = (uri_match == BRANCH_ITEM) ? BranchItemEntry.NO_ID_SET : BranchItemEntry.getBranchId(uri);
                int item_id = (uri_match == BRANCH_ITEM) ? BranchItemEntry.NO_ID_SET : BranchItemEntry.getItemId(uri);

                boolean branch_set = BranchItemEntry.isIdSpecified(getContext(), branch_id);
                boolean item_set = BranchItemEntry.isIdSpecified(getContext(), item_id);

                if (uri_match == BRANCH_ITEM_WITH_ID) {
                    String custom_selection = "";
                    List<String> args = new ArrayList<>();

                    boolean did_select_branch = false;
                    if (branch_set) {
                        if (!fetch_none_branch_items) {
                            did_select_branch = true;
                            custom_selection +=
                                    String.format(Locale.US, "%s = ?",
                                            BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID));
                            args.add(Long.toString(branch_id));
                        }
                    }
                    if (item_set) {
                        if (branch_set) {
                            custom_selection += (did_select_branch ? " AND " : "") +
                                    String.format(Locale.US, "%s = ?",
                                            BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID));
                            args.add(Long.toString(item_id));
                        } else {
                            /**
                             * if the branch was NOT set, it means we want to fetch the item
                             * in ALL branches, regardless it if exists in them or NOT.
                             * NOTE: we don't select with branch_item as it is null for the
                             * branches the item doesn't exist in.
                             */
                            fetch_item_in_all_branches = true;

                            custom_selection +=
                                    String.format(Locale.US, "%s = ?",
                                            ItemEntry._full(ItemEntry.COLUMN_ITEM_ID));
                            args.add(Long.toString(item_id));
                        }
                    }

                    selection = withAppendedSelection(selection,
                            custom_selection);

                    // if neither is set, why bother
                    if (did_select_branch || item_set) {
                        selectionArgs = withAppendedSelectionArgs(selectionArgs,
                                // use the template-ized method to avoid ClassCastException
                                args.toArray(new String[args.size()]));
                    }
                }

                /**
                 * Here we are using the ItemEntry.company_id instead of BranchItemEntry.column id because that
                 * might be null when LEFT JOINING.
                 */
                selection = withAppendedCompanyIdSelection(selection, ItemEntry._full(ItemEntry.COLUMN_COMPANY_ID));
                selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);

                SQLiteQueryBuilder builder;
                if (fetch_item_in_all_branches) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables(
                            /**
                             * See http://stackoverflow.com/questions/38610739/join-3-tables-without-losing-ability-to-refer-each-table
                             * for more details.
                             */
                            String.format(Locale.US,
                                    "%s CROSS JOIN %s LEFT JOIN %s ",
                                    ItemEntry.TABLE_NAME,
                                    /**
                                     * NOTE: Since we are doing a CROSS JOIN between items and branches, the dummy branch
                                     * will also appear in the result. If we don't filter it now,
                                     * we won't be able to remove it from the result by just selecting with
                                     * the company id, since that will only look at the company id from the item table
                                     * which will pass the test even for the dummy branch.
                                     */
                                    String.format(
                                            Locale.US,
                                            " (select * from %s where %s = %s AND %s != %s) %s ",
                                            BranchEntry.TABLE_NAME,
                                            BranchEntry._full(BranchEntry.COLUMN_COMPANY_ID),
                                            // we want to filter the branches in THIS COMPANY ONLY
                                            String.valueOf(company_id),
                                            BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID),
                                            String.valueOf(BranchEntry.DUMMY_BRANCH_ID),
                                            BranchEntry.TABLE_NAME
                                    ),
                                    BranchItemEntry.TABLE_NAME) +

                                    // The join conditions.
                                    String.format(Locale.US,
                                            " ON %s = %s AND %s = %s",
                                            ItemEntry._full(ItemEntry.COLUMN_ITEM_ID),
                                            BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID),

                                            BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID),
                                            BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID))
                    );
                } else if (fetch_none_branch_items) {
                    builder = new SQLiteQueryBuilder();
                    builder.setTables(
                            /**
                             * IMPORTANT: We are first filtering the branch we want from the BranchItemTable
                             * because if we do that after the selection in a where clause,
                             * e.g: "select ItemEntry LEFT JOIN BranchItemEntry where branch_id = ?",
                             * we will be ignoring ALL rows that that NULL values in the BranchItemEntry columns
                             * that are the result of LEFT JOINING. This is because the where clause is
                             * applied on the result of the selection. And the comparison "branch_id = ?"
                             * will be false for the NULL rows and they will be NOT BE IN THE RESULT.
                             * This is effectively doing an INNER JOIN-ing because the LEFT JOINed results
                             * that don't have values for the right tables are being removed.
                             */
                            ItemEntry.TABLE_NAME + " LEFT JOIN " +
                                    String.format(Locale.US,
                                            " (select * from %s where %s = %d) %s ",
                                            BranchItemEntry.TABLE_NAME,
                                            BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID),
                                            branch_id,
                                            BranchItemEntry.TABLE_NAME) +
                                    " ON ( " +
                                    ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) +
                                    " = " +
                                    BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) +
                                    ") "
                    );
                } else {
                    builder = sBranchItemFetchOnlyExistingItemQueryBuilder;
                }
                result = builder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection, selectionArgs,
                        null, null, sortOrder);

                break;
            }

            case BRANCH_CATEGORY_WITH_ID:
            case BRANCH_CATEGORY: {
                query_db = false;
                if (uri_match == BRANCH_CATEGORY_WITH_ID) {
                    int branch_id = BranchCategoryEntry.getBranchId(uri);
                    int category_id = BranchCategoryEntry.getCategoryId(uri);

                    boolean branch_set = BranchCategoryEntry.isIdSpecified(getContext(), branch_id);
                    boolean category_set = BranchCategoryEntry.isIdSpecified(getContext(), category_id);

                    String branch_category_selection = "";
                    List<String> args = new ArrayList<>();
                    if (branch_set) {
                        branch_category_selection +=
                                String.format(Locale.US, "%s = ?",
                                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_BRANCH_ID));
                        args.add(Long.toString(branch_id));
                    }
                    if (category_set) {
                        branch_category_selection += (branch_set ? " AND " : "");
                        branch_category_selection +=
                                String.format(Locale.US, "%s = ?",
                                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID));
                        args.add(Long.toString(category_id));
                    }
                    selection = withAppendedSelection(selection,
                            branch_category_selection);

                    // if neither is set, why bother
                    if (branch_set || category_set) {
                        selectionArgs = withAppendedSelectionArgs(selectionArgs,
                                // use the template-ized method to avoid ClassCastException
                                args.toArray(new String[args.size()]));
                    }
                }

                selection = withAppendedCompanyIdSelection(selection,
                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_COMPANY_ID));
                selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);
                SQLiteQueryBuilder builder;
                if (BranchCategoryEntry.isFetchingChildrenCategories(uri)) {
                    builder = sBranchCategoryWithCategoryChildrenQueryBuilder;
                } else {
                    builder = sBranchCategoryQueryBuilder;
                }
                result = builder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection, selectionArgs,
                        null, null, sortOrder);
                break;
            }

            case ITEM: {
                // should we join with the branch's list?
                if (ItemEntry.isBranchesSpecified(uri)) {
                    query_db = false;
                    selection = withAppendedCompanyIdSelection(selection, ItemEntry._full(ItemEntry.COLUMN_COMPANY_ID));
                    selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);

                    result = sItemWithBranchQueryBuilder.query(
                            mDbHelper.getReadableDatabase(),
                            projection,
                            selection, selectionArgs,
                            null, null, sortOrder);
                } else {
                    tableName = ItemEntry.TABLE_NAME;
                    column_company_id = ItemEntry._full(ItemEntry.COLUMN_COMPANY_ID);
                }
                break;
            }

            case ITEM_WITH_ID: {
                tableName = ItemEntry.TABLE_NAME;
                selection = ItemEntry.COLUMN_ITEM_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                selectionArgs = null;
                column_company_id = ItemEntry._full(ItemEntry.COLUMN_COMPANY_ID);
                break;
            }

            case TRANSACTION_WITH_ID:
            case TRANSACTION: {
                tableName = TransactionEntry.TABLE_NAME;
                if (uri_match == TRANSACTION_WITH_ID) {
                    selection = TransactionEntry.COLUMN_TRANS_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                column_company_id = TransactionEntry._full(TransactionEntry.COLUMN_COMPANY_ID);
                break;
            }

            case TRANSACTION_ITEM:
                tableName = TransItemEntry.TABLE_NAME;
                column_company_id = TransItemEntry._full(TransItemEntry.COLUMN_COMPANY_ID);
                break;

            case TRANSACTION_ITEM_WITH_TRANSACTION_ID: {
                query_db = false;
                boolean transaction_selected = TransItemEntry.
                        isTransactionIdSet(getContext(), TransItemEntry.getTransactionId(uri));
                if (transaction_selected) {
                    selection = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TRANS_ID + " = ? ";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                }

                selection = withAppendedCompanyIdSelection(selection, TransItemEntry._full(TransItemEntry.COLUMN_COMPANY_ID));
                selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);

                result = sTransactionItemsWithTransactionIdAndItemDetailQueryBuilder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection, selectionArgs,
                        null, null, sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (query_db) {
            if (append_company_id) {
                selection = withAppendedCompanyIdSelection(selection, column_company_id);
                selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);
            }

            result = mDbHelper.getReadableDatabase().query(
                    tableName,
                    projection,
                    selection,
                    selectionArgs,
                    null, null,
                    sortOrder);
        }
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case COMPANY_WITH_ID:
                return CompanyEntry.CONTENT_ITEM_TYPE;
            case COMPANY:
                return CompanyEntry.CONTENT_TYPE;

            case MEMBER_WITH_ID:
                return MemberEntry.CONTENT_ITEM_TYPE;
            case MEMBER:
                return MemberEntry.CONTENT_TYPE;

            case CATEGORY_WITH_ID:
                return CategoryEntry.CONTENT_ITEM_TYPE;
            case CATEGORY:
                return CategoryEntry.CONTENT_TYPE;

            case BRANCH_WITH_ID:
                return BranchEntry.CONTENT_ITEM_TYPE;
            case BRANCH:
                return BranchEntry.CONTENT_TYPE;

            case BRANCH_ITEM_WITH_ID: {
                int branch_id = BranchItemEntry.getBranchId(uri);
                int item_id = BranchItemEntry.getItemId(uri);
                if (BranchItemEntry.isIdSpecified(getContext(), branch_id) &&
                        BranchItemEntry.isIdSpecified(getContext(), item_id)) {
                    return BranchItemEntry.CONTENT_ITEM_TYPE;
                } else {
                    return BranchItemEntry.CONTENT_TYPE;
                }
            }
            case BRANCH_ITEM:
                return BranchItemEntry.CONTENT_TYPE;

            case BRANCH_CATEGORY_WITH_ID: {
                int branch_id = BranchCategoryEntry.getBranchId(uri);
                int category_id = BranchCategoryEntry.getBranchId(uri);
                if (BranchCategoryEntry.isIdSpecified(getContext(), branch_id) &&
                        BranchCategoryEntry.isIdSpecified(getContext(), category_id)) {
                    return BranchCategoryEntry.CONTENT_ITEM_TYPE;
                } else {
                    return BranchCategoryEntry.CONTENT_TYPE;
                }
            }
            case BRANCH_CATEGORY:
                return BranchCategoryEntry.CONTENT_TYPE;

            case ITEM_WITH_ID:
                return ItemEntry.CONTENT_ITEM_TYPE;
            case ITEM:
                return ItemEntry.CONTENT_TYPE;

            case TRANSACTION_WITH_ID:
                return TransactionEntry.CONTENT_ITEM_TYPE;
            case TRANSACTION:
                return TransactionEntry.CONTENT_TYPE;

            case TRANSACTION_ITEM_WITH_TRANSACTION_ID:
                return TransItemEntry.CONTENT_TYPE;
            case TRANSACTION_ITEM:
                return TransItemEntry.CONTENT_TYPE;
        }
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        boolean update = false;
        if (values.containsKey(SheketContract.SQL_INSERT_OR_UPDATE)) {
            update = values.getAsBoolean(SheketContract.SQL_INSERT_OR_UPDATE);
            // create a local copy so we won't mess up the user's object
            values = new ContentValues(values);
            // remove the update element
            values.remove(SheketContract.SQL_INSERT_OR_UPDATE);
        }

        int match = sUriMatcher.match(uri);

        int company_id = -1;
        if (match != COMPANY) {
            company_id = CompanyBase.getCompanyId(uri);
        } else {
            if (values.containsKey(CompanyEntry.COLUMN_COMPANY_ID)) {
                company_id = values.getAsInteger(CompanyEntry.COLUMN_COMPANY_ID);
            }
        }

        Uri returnUri = null;

        if (match == COMPANY) {
            /**
             * Since all data is DIRECTLY foreign keyed to the company table, we should be
             * EXTREMELY CAREFUL not to delete any row from the table. WHen the user syncs
             * with a company that he already is a member of, it will try to add it here.
             * We've set an ON CONFLICT IGNORE so as not to delete any existing rows.
             * We have tried to add the company using {@code db.insertWithOnConflict} method
             * passing in {@code SQLiteDatabase.CONFLICT_IGNORE} conflictResolutionAlgorithm
             * to enforce the constraint and return to us the previously added company id.
             * But, sadly android is not working and is returning -1 signaling an error.
             * This results in a COMPLETE app shutdown as the user can't sync no-more
             * thinking there was an error inserting the company. So, the suggested
             * solution is to first query the db for the company id and only try to
             * insert it if it doesn't exist.
             *
             * See http://stackoverflow.com/questions/13391915/why-does-insertwithonconflict-conflict-ignore-return-1-error for more.
             */

            // first try and query to see if the company already exists
            Cursor cursor = query(CompanyEntry.buildCompanyUri(company_id),
                    null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) { // the company exists!!!
                cursor.close();
                returnUri = CompanyEntry.buildCompanyUri(company_id);
            } else {
                if (cursor != null) {
                    cursor.close();
                }

                long _id = db.insertWithOnConflict(CompanyEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (_id != -1) {
                    returnUri = CompanyEntry.buildCompanyUri(company_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
            }
        } else {
            SelectionInfo info = getTableNameAndSelectionForContentValue(match, values);
            String tableName = info.tableName;
            String selection = info.selection;
            String columnId = info.columnId;

            // we haven't matched any of the tables
            if (tableName == null)
                throw new UnsupportedOperationException("Insert, unknown uri: " + uri);

            /**
             * Read why we are doing update-then-insert to update rows that already exist.
             * http://stackoverflow.com/questions/11686645/android-sqlite-insert-update-table-columns-to-keep-the-identifier
             */
            if (update) {
                db.update(tableName, values, selection, null);
            }

            int _id = (int)db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_IGNORE);

            // if not found and we can select, try to select it out
            if (_id == -1 && selection != null && columnId != null) {
                /**
                 * Refer to http://stackoverflow.com/questions/13391915/why-does-insertwithonconflict-conflict-ignore-return-1-error
                 */
                SQLiteStatement stmt = db.compileStatement(
                        String.format(Locale.US,
                                "select %s from %s where %s",
                                columnId, tableName, selection));
                try {
                    _id = (int)stmt.simpleQueryForLong();
                } finally {
                    stmt.close();
                }
            }

            // if it is still not known after all this, give up
            if (_id == -1) {
                throw new android.database.SQLException("Failed to insert row into " + uri);
            }

            switch (match) {
                case MEMBER:
                    returnUri = MemberEntry.buildMemberUri(company_id, _id);
                    break;
                case CATEGORY:
                    returnUri = CategoryEntry.buildCategoryUri(company_id, _id);
                    break;
                case BRANCH:
                    returnUri = BranchEntry.buildBranchUri(company_id, _id);
                    break;
                case BRANCH_ITEM: {
                    int branch_id = values.getAsInteger(BranchItemEntry.COLUMN_BRANCH_ID);
                    int item_id = values.getAsInteger(BranchItemEntry.COLUMN_ITEM_ID);
                    returnUri = BranchItemEntry.buildBranchItemUri(company_id, branch_id, item_id);
                    break;
                }
                case BRANCH_CATEGORY: {
                    int branch_id = values.getAsInteger(BranchCategoryEntry.COLUMN_BRANCH_ID);
                    int category_id = values.getAsInteger(BranchCategoryEntry.COLUMN_CATEGORY_ID);
                    returnUri = BranchCategoryEntry.buildBranchCategoryUri(company_id, branch_id, category_id);
                    break;
                }
                case ITEM:
                    returnUri = ItemEntry.buildItemUri(company_id, _id);
                    break;
                case TRANSACTION:
                    returnUri = TransactionEntry.buildTransactionUri(company_id, _id);
                    break;
                case TRANSACTION_ITEM: {
                    long trans_id = values.getAsLong(TransItemEntry.COLUMN_TRANSACTION_ID);
                    returnUri = TransactionEntry.buildTransactionUri(company_id, trans_id);
                }
            }
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }


    static class SelectionInfo {
        public String tableName;

        // the name of the column that has the row id(usually the primary key)
        // for tables with multiple columns as the primary key, one of them will be selected
        public String columnId;

        // the selection that can be used to identify a particular row
        public String selection;
    }

    /**
     * Returns a {@code SelectionInfo} for the provided ContentValues. It will contain the selection
     * that can be used to select a particular row with can be uniquely identified by columns
     * in the ContentValues.
     */
    private SelectionInfo getTableNameAndSelectionForContentValue(int uri_match, ContentValues values) {
        SelectionInfo info = new SelectionInfo();
        info.tableName = info.columnId = info.selection = null;

        switch (uri_match) {
            case MEMBER: {
                info.tableName = MemberEntry.TABLE_NAME;
                if (values.containsKey(MemberEntry.COLUMN_MEMBER_ID)) {
                    info.columnId = MemberEntry.COLUMN_MEMBER_ID;
                    info.selection = MemberEntry.COLUMN_MEMBER_ID + " = " + values.getAsString(MemberEntry.COLUMN_MEMBER_ID);
                }
                break;
            }

            case CATEGORY: {
                info.tableName = CategoryEntry.TABLE_NAME;
                if (values.containsKey(CategoryEntry.COLUMN_CATEGORY_ID)) {
                    info.columnId = CategoryEntry.COLUMN_CATEGORY_ID;
                    info.selection = CategoryEntry.COLUMN_CATEGORY_ID + " = " + values.getAsString(CategoryEntry.COLUMN_CATEGORY_ID);
                }
                break;
            }

            case BRANCH: {
                info.tableName = BranchEntry.TABLE_NAME;
                if (values.containsKey(BranchEntry.COLUMN_BRANCH_ID)) {
                    info.columnId = BranchEntry.COLUMN_BRANCH_ID;
                    info.selection = BranchEntry.COLUMN_BRANCH_ID + " = " + values.getAsString(BranchEntry.COLUMN_BRANCH_ID);
                }
                break;
            }

            case BRANCH_ITEM: {
                info.tableName = BranchItemEntry.TABLE_NAME;
                if (values.containsKey(BranchItemEntry.COLUMN_BRANCH_ID) &&
                        values.containsKey(BranchItemEntry.COLUMN_ITEM_ID)) {

                    // we've can choose either of the columns
                    info.columnId = BranchItemEntry.COLUMN_BRANCH_ID;

                    info.selection = String.format(Locale.US,
                            "%s = %s AND %s = %s",
                            BranchItemEntry.COLUMN_BRANCH_ID, values.getAsString(BranchItemEntry.COLUMN_BRANCH_ID),
                            BranchItemEntry.COLUMN_ITEM_ID, values.getAsString(BranchItemEntry.COLUMN_ITEM_ID));
                }
                break;
            }

            case BRANCH_CATEGORY: {
                info.tableName = BranchCategoryEntry.TABLE_NAME;
                if (values.containsKey(BranchCategoryEntry.COLUMN_BRANCH_ID) &&
                        values.containsKey(BranchCategoryEntry.COLUMN_CATEGORY_ID)) {
                    info.columnId = BranchCategoryEntry.COLUMN_BRANCH_ID;
                    info.selection = String.format(Locale.US,
                            "%s = %s AND %s = %s",
                            BranchCategoryEntry.COLUMN_BRANCH_ID, values.getAsString(BranchCategoryEntry.COLUMN_BRANCH_ID),
                            BranchCategoryEntry.COLUMN_CATEGORY_ID, values.getAsString(BranchCategoryEntry.COLUMN_CATEGORY_ID));
                }
                break;
            }

            case ITEM: {
                info.tableName = ItemEntry.TABLE_NAME;
                if (values.containsKey(ItemEntry.COLUMN_ITEM_ID)) {
                    info.columnId = ItemEntry.COLUMN_ITEM_ID;
                    info.selection = ItemEntry.COLUMN_ITEM_ID + " = " + values.getAsString(ItemEntry.COLUMN_ITEM_ID);
                }
                break;
            }

            case TRANSACTION: {
                info.tableName = TransactionEntry.TABLE_NAME;
                if (values.containsKey(TransactionEntry.COLUMN_TRANS_ID)) {
                    info.columnId = TransactionEntry.COLUMN_TRANS_ID;
                    info.selection = TransactionEntry.COLUMN_TRANS_ID + " = " + values.getAsString(TransactionEntry.COLUMN_TRANS_ID);
                }
                break;
            }

            case TRANSACTION_ITEM: {
                info.tableName = TransItemEntry.TABLE_NAME;
                // we can't select a transaction items b/c a transaction doesn't uniquely identify it
                break;
            }
        }

        return info;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        String tableName;
        switch (match) {
            case COMPANY:
                tableName = CompanyEntry.TABLE_NAME;
                break;
            case MEMBER:
                tableName = MemberEntry.TABLE_NAME;
                break;
            case CATEGORY:
                tableName = CategoryEntry.TABLE_NAME;
                break;
            case BRANCH:
                tableName = BranchEntry.TABLE_NAME;
                break;
            case BRANCH_ITEM:
                tableName = BranchItemEntry.TABLE_NAME;
                break;
            case BRANCH_CATEGORY:
                tableName = BranchCategoryEntry.TABLE_NAME;
                break;
            case ITEM:
                tableName = ItemEntry.TABLE_NAME;
                break;
            case TRANSACTION:
                tableName = TransactionEntry.TABLE_NAME;
                break;
            case TRANSACTION_ITEM:
                tableName = TransItemEntry.TABLE_NAME;
                break;

            default:
                throw new UnsupportedOperationException("Delete, unknown uri: " + uri);
        }

        rowsDeleted = db.delete(tableName, selection, selectionArgs);
        // Because a null deletes all rows
        if (selection == null || rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        String tableName;
        switch (match) {
            case COMPANY:
                tableName = CompanyEntry.TABLE_NAME;
                break;
            case CATEGORY:
                tableName = CategoryEntry.TABLE_NAME;
                break;
            case MEMBER:
                tableName = MemberEntry.TABLE_NAME;
                break;
            case BRANCH:
                tableName = BranchEntry.TABLE_NAME;
                break;
            case BRANCH_ITEM:
                tableName = BranchItemEntry.TABLE_NAME;
                break;
            case BRANCH_CATEGORY:
                tableName = BranchCategoryEntry.TABLE_NAME;
                break;
            case ITEM:
                tableName = ItemEntry.TABLE_NAME;
                break;
            case TRANSACTION:
                tableName = TransactionEntry.TABLE_NAME;
                break;
            case TRANSACTION_ITEM:
                tableName = TransItemEntry.TABLE_NAME;
                break;

            default:
                throw new UnsupportedOperationException("Update, unknown uri: " + uri);
        }

        rowsUpdated = db.update(tableName, values, selection, selectionArgs);
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        String tableName;
        switch (match) {
            case COMPANY:
                tableName = CompanyEntry.TABLE_NAME;
                break;
            case MEMBER:
                tableName = MemberEntry.TABLE_NAME;
                break;
            case CATEGORY:
                tableName = CategoryEntry.TABLE_NAME;
                break;
            case BRANCH:
                tableName = BranchEntry.TABLE_NAME;
                break;
            case BRANCH_ITEM:
                tableName = BranchItemEntry.TABLE_NAME;
                break;
            case BRANCH_CATEGORY:
                tableName = BranchCategoryEntry.TABLE_NAME;
                break;
            case ITEM:
                tableName = ItemEntry.TABLE_NAME;
                break;
            case TRANSACTION:
                tableName = TransactionEntry.TABLE_NAME;
                break;
            case TRANSACTION_ITEM:
                tableName = TransItemEntry.TABLE_NAME;
                break;

            default:
                return super.bulkInsert(uri, values);
        }


        db.beginTransaction();
        int returnCount = 0;
        try {
            for (ContentValues value : values) {
                boolean update = false;
                String selection = null;
                if (value.containsKey(SheketContract.SQL_INSERT_OR_UPDATE)) {
                    update = value.getAsBoolean(SheketContract.SQL_INSERT_OR_UPDATE);

                    SelectionInfo info = getTableNameAndSelectionForContentValue(match, value);
                    selection = info.selection;

                    value = new ContentValues(value);
                    // remove the update element
                    value.remove(SheketContract.SQL_INSERT_OR_UPDATE);
                }

                int rows_updated = 0;
                if (update && selection != null) {
                    rows_updated = db.update(tableName, value, selection, null);
                }
                if (rows_updated == 0) {
                    long _id = db.insert(tableName, null, value);
                    if (_id != -1) {
                        returnCount++;
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }

}
