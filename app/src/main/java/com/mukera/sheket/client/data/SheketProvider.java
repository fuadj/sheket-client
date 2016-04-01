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
    private static final int BRANCH_ITEM_WITH_BRANCH_AND_ITEM_ID = 301;
    private static final int BRANCH_ITEM_WITH_BRANCH_ID = 302;
    private static final int BRANCH_ITEM_WITH_ITEM_ID = 303;

    private static final int ITEM = 400;
    private static final int ITEM_WITH_ID = 401;

    private static final int TRANSACTION = 500;
    private static final int TRANSACTION_WITH_ID = 501;

    private static final int TRANSACTION_ITEM = 600;
    private static final int TRANSACTION_ITEM_WITH_TRANSACTION_ID = 601;

    private static final SQLiteQueryBuilder sTransactionItemsWithTransactionIdQueryBuilder;
    private static final SQLiteQueryBuilder sBranchItemWithItemDetailQueryBuilder;

    static {
        sTransactionItemsWithTransactionIdQueryBuilder = new SQLiteQueryBuilder();
        sTransactionItemsWithTransactionIdQueryBuilder.setTables(
                TransItemEntry.TABLE_NAME + " INNER JOIN " + TransactionEntry.TABLE_NAME +
                        " ON (" +
                        TransItemEntry.TABLE_NAME + "." + TransItemEntry.COLUMN_TRANSACTION_ID +
                        " = " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TRANS_ID + ") " +
                        " INNER JOIN " + ItemEntry.TABLE_NAME +
                        " ON (" +
                        TransItemEntry.TABLE_NAME + "." + TransItemEntry.COLUMN_ITEM_ID +
                        " = " +
                        ItemEntry.TABLE_NAME + "." + ItemEntry.COLUMN_ITEM_ID + ")"
        );

        sBranchItemWithItemDetailQueryBuilder = new SQLiteQueryBuilder();
        sBranchItemWithItemDetailQueryBuilder.setTables(
                BranchItemEntry.TABLE_NAME + " inner join " + ItemEntry.TABLE_NAME +
                        " ON ( " +
                        BranchItemEntry.TABLE_NAME + "." + BranchItemEntry.COLUMN_ITEM_ID +
                        " = " +
                        ItemEntry.TABLE_NAME + "." + ItemEntry.COLUMN_ITEM_ID + ") "
        );
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SheketContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, SheketContract.PATH_COMPANY, COMPANY);
        matcher.addURI(authority, SheketContract.PATH_COMPANY + "/#", COMPANY_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_BRANCH, BRANCH);
        matcher.addURI(authority, SheketContract.PATH_BRANCH + "/#", BRANCH_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM, BRANCH_ITEM);
        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM + "/" +
                BranchItemEntry.ITEM_PATH_SEGMENT + "/#",
                BRANCH_ITEM_WITH_ITEM_ID);
        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM + "/" +
                BranchItemEntry.BRANCH_PATH_SEGMENT + "/#",
                BRANCH_ITEM_WITH_BRANCH_ID);
        matcher.addURI(authority, SheketContract.PATH_BRANCH_ITEM + "/" +
                BranchItemEntry.BRANCH_ITEM_PATH_SEGMENT + "/#/#",
                BRANCH_ITEM_WITH_BRANCH_AND_ITEM_ID);

        matcher.addURI(authority, SheketContract.PATH_ITEM, ITEM);
        matcher.addURI(authority, SheketContract.PATH_ITEM + "/#", ITEM_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_TRANSACTION, TRANSACTION);
        matcher.addURI(authority, SheketContract.PATH_TRANSACTION + "/#", TRANSACTION_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_TRANS_ITEMS, TRANSACTION_ITEM);
        matcher.addURI(authority, SheketContract.PATH_TRANS_ITEMS + "/#",
                TRANSACTION_ITEM_WITH_TRANSACTION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SheketDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor result = null;
        String tableName = null;

        boolean query_db = true;
        int uri_match = sUriMatcher.match(uri);
        switch (uri_match) {
            case COMPANY_WITH_ID:
            case COMPANY: {
                tableName = CompanyEntry.TABLE_NAME;
                if (uri_match == COMPANY_WITH_ID) {
                    selection = CompanyEntry.COLUMN_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
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
                break;
            }

            case BRANCH_ITEM_WITH_BRANCH_AND_ITEM_ID:
            case BRANCH_ITEM_WITH_BRANCH_ID:
            case BRANCH_ITEM_WITH_ITEM_ID:
            case BRANCH_ITEM: {
                query_db = false;
                if (uri_match == BRANCH_ITEM_WITH_BRANCH_AND_ITEM_ID) {
                    selection = String.format("%s = ? AND %s = ?",
                            BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID),
                            BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID));
                    selectionArgs = new String[]{
                            Long.toString(BranchItemEntry.getBranchIdFromBranchItemUri(uri)),
                            Long.toString(BranchItemEntry.getItemIdFromBranchItemUri(uri))
                    };
                } else if (uri_match == BRANCH_ITEM_WITH_BRANCH_ID) {
                    selection = String.format("%s = ?",
                            BranchItemEntry._full(BranchItemEntry.COLUMN_BRANCH_ID));
                    selectionArgs = new String[]{
                            Long.toString(BranchItemEntry.getBranchIdFromBranchUri(uri))
                    };
                } else if (uri_match == BRANCH_ITEM_WITH_ITEM_ID) {
                    selection = String.format("%s = ?",
                            BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID));
                    selectionArgs = new String[]{
                            Long.toString(BranchItemEntry.getItemIdFromItemUri(uri))
                    };
                }
                result = sBranchItemWithItemDetailQueryBuilder.query(
                        mDbHelper.getReadableDatabase(),
                        projection,
                        selection, selectionArgs,
                        null, null, sortOrder);
                break;
            }

            case ITEM:
            case ITEM_WITH_ID: {
                tableName = ItemEntry.TABLE_NAME;
                if (uri_match == ITEM_WITH_ID) {
                    selection = ItemEntry.COLUMN_ITEM_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                break;
            }

            case TRANSACTION_WITH_ID:
            case TRANSACTION: {
                tableName = TransactionEntry.TABLE_NAME;
                if (uri_match == TRANSACTION_WITH_ID) {
                    selection = TransactionEntry.COLUMN_TRANS_ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                break;
            }

            case TRANSACTION_ITEM:
                tableName = TransItemEntry.TABLE_NAME;
                break;

            case TRANSACTION_ITEM_WITH_TRANSACTION_ID: {
                query_db = false;
                selection = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TRANS_ID + " = ? ";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                result = sTransactionItemsWithTransactionIdQueryBuilder.query(
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

            case BRANCH_WITH_ID:
                return BranchEntry.CONTENT_ITEM_TYPE;
            case BRANCH:
                return BranchEntry.CONTENT_TYPE;

            case BRANCH_ITEM_WITH_BRANCH_AND_ITEM_ID:
                return BranchItemEntry.CONTENT_ITEM_TYPE;
            case BRANCH_ITEM_WITH_ITEM_ID:
                return BranchItemEntry.CONTENT_TYPE;
            case BRANCH_ITEM_WITH_BRANCH_ID:
                return BranchItemEntry.CONTENT_TYPE;
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
        Uri returnUri = null;
        switch (match) {
            case COMPANY: {
                long _id = db.insert(CompanyEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = CompanyEntry.buildCompanyUri(_id);
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
                if (_id > 0) {
                    returnUri = BranchEntry.buildBranchUri(_id);
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
                if (_id > 0) {
                    long branch_id = values.getAsLong(BranchItemEntry.COLUMN_BRANCH_ID);
                    long item_id = values.getAsLong(BranchItemEntry.COLUMN_ITEM_ID);
                    returnUri = BranchItemEntry.buildBranchItemUri(branch_id, item_id);
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
                if (_id > 0) {
                    returnUri = ItemEntry.buildItemUri(_id);
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
                if (_id > 0) {
                    returnUri = TransactionEntry.buildTransactionUri(_id);
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
                if (_id > 0) {
                    long trans_id = values.getAsLong(TransItemEntry.COLUMN_TRANSACTION_ID);
                    returnUri = TransactionEntry.buildTransactionUri(trans_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            default:
                throw new UnsupportedOperationException("Insert, unknown uri: " + uri);
        }
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        String tableName;
        switch (match) {
            case COMPANY: tableName = CompanyEntry.TABLE_NAME; break;
            case BRANCH: tableName = BranchEntry.TABLE_NAME; break;
            case BRANCH_ITEM: tableName = BranchItemEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case TRANSACTION_ITEM: tableName = TransItemEntry.TABLE_NAME; break;

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
            case COMPANY: tableName = CompanyEntry.TABLE_NAME; break;
            case BRANCH: tableName = BranchEntry.TABLE_NAME; break;
            case BRANCH_ITEM: tableName = BranchItemEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case TRANSACTION_ITEM: tableName = TransItemEntry.TABLE_NAME; break;

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
            case COMPANY: tableName = CompanyEntry.TABLE_NAME; break;
            case BRANCH: tableName = BranchEntry.TABLE_NAME; break;
            case BRANCH_ITEM: tableName = BranchItemEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case TRANSACTION_ITEM: tableName = TransItemEntry.TABLE_NAME; break;

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
