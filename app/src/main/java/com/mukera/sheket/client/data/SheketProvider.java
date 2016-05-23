package com.mukera.sheket.client.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.mukera.sheket.client.data.SheketContract.*;

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

    private static final SQLiteQueryBuilder sTransactionItemsWithTransactionIdAndItemDetailQueryBuilder;
    private static final SQLiteQueryBuilder sBranchItemWithItemDetailQueryBuilder;
    private static final SQLiteQueryBuilder sItemWithBranchQueryBuilder;

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

        sBranchItemWithItemDetailQueryBuilder = new SQLiteQueryBuilder();
        sBranchItemWithItemDetailQueryBuilder.setTables(
                BranchItemEntry.TABLE_NAME + " inner join " + ItemEntry.TABLE_NAME +
                        " ON ( " +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) +
                        " = " +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + ") "
        );

        sItemWithBranchQueryBuilder = new SQLiteQueryBuilder();
        // items_table LEFT JOIN
        //      (branch_items_table INNER JOIN branches_table ON branch_id)
        // ON (item_id)
        /*
        sItemWithBranchQueryBuilder.setTables(
                ItemEntry.TABLE_NAME + " LEFT JOIN " +
                        BranchItemEntry.TABLE_NAME + " INNER JOIN " + BranchEntry.TABLE_NAME +
                        " ON (" +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID) +
                        " = " +
                        BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ) " +
                        " ON (" +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) +
                        " = " +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) + ")"
        );
        */
        sItemWithBranchQueryBuilder.setTables(
                ItemEntry.TABLE_NAME + " LEFT JOIN " +
                        BranchItemEntry.TABLE_NAME + " ON (" +
                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = " + BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) + ") " +
                        " LEFT JOIN " +
                        BranchEntry.TABLE_NAME + " ON ( " +
                        BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID) + " = " + BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + ")"
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
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SheketDbHelper(getContext());
        return true;
    }

    String withAppendedCompanyIdSelection(String selection, String col_company_id) {
        return (selection != null ? (selection + " AND ") : "") +
                col_company_id + " = ?";
    }

    String[] withAppendedCompanyIdSelectionArgs(String[] args, long company_id) {
        if (args == null) {
            return new String[]{Long.toString(company_id)};
        }
        String[] appended = new String[args.length + 1];
        System.arraycopy(args, 0, appended, 0, args.length);
        appended[appended.length - 1] = Long.toString(company_id);
        return appended;
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
                    selection = CompanyEntry.COLUMN_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
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
                tableName = CategoryEntry.TABLE_NAME;
                if (uri_match == CATEGORY_WITH_ID) {
                    selection = CategoryEntry.COLUMN_CATEGORY_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                column_company_id = CategoryEntry._full(CategoryEntry.COLUMN_COMPANY_ID);
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
                if (uri_match == BRANCH_ITEM_WITH_ID) {
                    long branch_id = BranchItemEntry.getBranchId(uri);
                    long item_id = BranchItemEntry.getItemId(uri);

                    boolean branch_set = BranchItemEntry.isIdSpecified(getContext(), branch_id);
                    boolean item_set = BranchItemEntry.isIdSpecified(getContext(), item_id);

                    if (branch_set && item_set) {
                        selection = String.format("%s = ? AND %s = ?",
                                BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID),
                                BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID));
                        selectionArgs = new String[]{
                                Long.toString(branch_id),
                                Long.toString(item_id)
                        };
                    } else if (branch_set) {
                        selection = String.format("%s = ?",
                                BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID));
                        selectionArgs = new String[]{
                                Long.toString(branch_id)
                        };
                    } else if (item_set) {
                        selection = String.format("%s = ?",
                                BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID));
                        selectionArgs = new String[]{
                                Long.toString(item_id)
                        };
                    }
                }

                selection = withAppendedCompanyIdSelection(selection, BranchItemEntry._full(BranchItemEntry.COLUMN_COMPANY_ID));
                selectionArgs = withAppendedCompanyIdSelectionArgs(selectionArgs, company_id);

                result = sBranchItemWithItemDetailQueryBuilder.query(
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

            case BRANCH_ITEM_WITH_ID:
                long branch_id = BranchItemEntry.getBranchId(uri);
                long item_id = BranchItemEntry.getItemId(uri);
                if (BranchItemEntry.isIdSpecified(getContext(), branch_id) &&
                        BranchItemEntry.isIdSpecified(getContext(), item_id)) {
                    return BranchItemEntry.CONTENT_ITEM_TYPE;
                } else {
                    return BranchItemEntry.CONTENT_TYPE;
                }
            case BRANCH_ITEM:
                return BranchItemEntry.CONTENT_TYPE;

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

        boolean replace = false;
        if (values.containsKey(SheketContract.SQL_INSERT_OR_REPLACE)) {
            replace = values.getAsBoolean(SheketContract.SQL_INSERT_OR_REPLACE);
            values = new ContentValues(values);
            // remove the replace element
            values.remove(SheketContract.SQL_INSERT_OR_REPLACE);
        }

        int match = sUriMatcher.match(uri);

        long company_id = -1;
        if (match != COMPANY) {
            company_id = CompanyBase.getCompanyId(uri);
        }

        Uri returnUri = null;
        final long insert_error = -1;
        switch (match) {
            case COMPANY: {
                long _id = db.insert(CompanyEntry.TABLE_NAME, null, values);
                if (_id != insert_error) {
                    returnUri = CompanyEntry.buildCompanyUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case MEMBER: {
                long _id;
                if (replace) {
                    _id = db.replace(MemberEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(MemberEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    returnUri = MemberEntry.buildMemberUri(company_id, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case CATEGORY: {
                long _id;
                if (replace) {
                    _id = db.replace(CategoryEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(CategoryEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    returnUri = CategoryEntry.buildCategoryUri(company_id, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case BRANCH: {
                long _id;
                if (replace) {
                    _id = db.replace(BranchEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(BranchEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    returnUri = BranchEntry.buildBranchUri(company_id, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case BRANCH_ITEM: {
                long _id;
                if (replace) {
                    _id = db.replace(BranchItemEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(BranchItemEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    long branch_id = values.getAsLong(BranchItemEntry.COLUMN_BRANCH_ID);
                    long item_id = values.getAsLong(BranchItemEntry.COLUMN_ITEM_ID);
                    returnUri = BranchItemEntry.buildBranchItemUri(company_id, branch_id, item_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case ITEM: {
                long _id;
                if (replace) {
                    _id = db.replace(ItemEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(ItemEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    returnUri = ItemEntry.buildItemUri(company_id, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case TRANSACTION: {
                long _id;
                if (replace) {
                    _id = db.replace(TransactionEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(TransactionEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    returnUri = TransactionEntry.buildTransactionUri(company_id, _id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case TRANSACTION_ITEM: {
                long _id;
                if (replace) {
                    _id = db.replace(TransItemEntry.TABLE_NAME, null, values);
                } else {
                    _id = db.insert(TransItemEntry.TABLE_NAME, null, values);
                }
                if (_id != insert_error) {
                    long trans_id = values.getAsLong(TransItemEntry.COLUMN_TRANSACTION_ID);
                    returnUri = TransactionEntry.buildTransactionUri(company_id, trans_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            default:
                throw new UnsupportedOperationException("Insert, unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
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
                boolean replace = false;
                if (value.containsKey(SheketContract.SQL_INSERT_OR_REPLACE)) {
                    replace = value.getAsBoolean(SheketContract.SQL_INSERT_OR_REPLACE);
                    value = new ContentValues(value);
                    // remove the replace element
                    value.remove(SheketContract.SQL_INSERT_OR_REPLACE);
                }

                long _id;
                if (replace) {
                    _id = db.replace(tableName, null, value);
                } else {
                    _id = db.insert(tableName, null, value);
                }
                if (_id != -1) {
                    returnCount++;
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
