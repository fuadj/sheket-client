package com.mukera.sheket.client.contentprovider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import com.mukera.sheket.client.contentprovider.SheketContract.*;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private SheketDbHelper mDbHelper;

    private static final int CATEGORY = 100;
    private static final int CATEGORY_WITH_ID = 101;
    private static final int TRANSACTION = 200;
    private static final int TRANSACTION_WITH_ID = 201;
    private static final int AFFECTED_ITEM = 300;
    private static final int AFFECTED_ITEM_WITH_TRANSACTION_ID = 301;
    private static final int ITEM = 400;
    private static final int ITEM_WITH_ID = 401;
    private static final int ITEM_IN_CATEGORY = 402;

    private static final SQLiteQueryBuilder sAffectedItemsWithTransactionIdQueryBuilder;

    static {
        sAffectedItemsWithTransactionIdQueryBuilder = new SQLiteQueryBuilder();
        sAffectedItemsWithTransactionIdQueryBuilder.setTables(
                AffectedItemEntry.TABLE_NAME + " INNER JOIN " + TransactionEntry.TABLE_NAME +
                        " ON (" +
                        AffectedItemEntry.TABLE_NAME + "." + AffectedItemEntry.COLUMN_TRANSACTION_ID +
                        " = " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry._ID + ") " +
                        " INNER JOIN " + ItemEntry.TABLE_NAME +
                        " ON (" +
                        AffectedItemEntry.TABLE_NAME + "." + AffectedItemEntry.COLUMN_ITEM_ID +
                        " = " +
                        ItemEntry.TABLE_NAME + "." + ItemEntry._ID + ")"
        );
    }

    private static final String sTransactionWithIdSelection =
            TransactionEntry.TABLE_NAME + "." + TransactionEntry._ID + " = ? ";

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SheketContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, SheketContract.PATH_CATEGORY, CATEGORY);
        matcher.addURI(authority, SheketContract.PATH_CATEGORY + "/#", CATEGORY_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_TRANSACTION, TRANSACTION);
        matcher.addURI(authority, SheketContract.PATH_TRANSACTION + "/#", TRANSACTION_WITH_ID);

        matcher.addURI(authority, SheketContract.PATH_ITEM, ITEM);
        matcher.addURI(authority, SheketContract.PATH_ITEM + "/#", ITEM_WITH_ID);
        matcher.addURI(authority, SheketContract.PATH_ITEM + "/" +
                ItemEntry.CATEGORY_PATH_SEGMENT + "/#", ITEM_IN_CATEGORY);

        matcher.addURI(authority, SheketContract.PATH_AFFECTED_ITEMS, AFFECTED_ITEM);
        matcher.addURI(authority, SheketContract.PATH_AFFECTED_ITEMS + "/" +
                AffectedItemEntry.TRANSACTION_PATH_SEGMENT + "/#",
                AFFECTED_ITEM_WITH_TRANSACTION_ID);

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
            case CATEGORY_WITH_ID:
            case CATEGORY: {
                tableName = CategoryEntry.TABLE_NAME;
                if (uri_match == CATEGORY_WITH_ID) {
                    selection = CategoryEntry._ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                break;
            }

            case TRANSACTION_WITH_ID:
            case TRANSACTION: {
                tableName = TransactionEntry.TABLE_NAME;
                if (uri_match == TRANSACTION_WITH_ID) {
                    selection = TransactionEntry._ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                }
                break;
            }

            case ITEM:
            case ITEM_IN_CATEGORY:
            case ITEM_WITH_ID: {
                tableName = ItemEntry.TABLE_NAME;
                if (uri_match == ITEM_WITH_ID) {
                    selection = ItemEntry._ID + " = ' " + ContentUris.parseId(uri) + " ' ";
                    selectionArgs = null;
                } else if (uri_match == ITEM_IN_CATEGORY) {
                    selection = ItemEntry.COLUMN_CATEGORY_ID + " = ' " +
                            ItemEntry.getCategoryIdFromUri(uri) + " ' ";
                    selectionArgs = null;
                }
                break;
            }

            case AFFECTED_ITEM:
                tableName = AffectedItemEntry.TABLE_NAME;
                break;

            case AFFECTED_ITEM_WITH_TRANSACTION_ID: {
                query_db = false;
                selection = sTransactionWithIdSelection;
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                result = sAffectedItemsWithTransactionIdQueryBuilder.query(
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
            case CATEGORY_WITH_ID:
                return CategoryEntry.CONTENT_ITEM_TYPE;
            case CATEGORY:
                return CategoryEntry.CONTENT_TYPE;

            case TRANSACTION_WITH_ID:
                return TransactionEntry.CONTENT_ITEM_TYPE;
            case TRANSACTION:
                return TransactionEntry.CONTENT_TYPE;

            case ITEM_WITH_ID:
                return ItemEntry.CONTENT_ITEM_TYPE;
            case ITEM_IN_CATEGORY:
            case ITEM:
                return ItemEntry.CONTENT_TYPE;

            case AFFECTED_ITEM_WITH_TRANSACTION_ID:
                return AffectedItemEntry.CONTENT_TYPE;
            case AFFECTED_ITEM:
                return AffectedItemEntry.CONTENT_TYPE;
        }
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri = null;
        switch (match) {
            case CATEGORY: {
                long _id = db.insert(CategoryEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = CategoryEntry.buildCategoryUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case TRANSACTION: {
                long _id = db.insert(TransactionEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = TransactionEntry.buildTransactionUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case ITEM: {
                long _id = db.insert(ItemEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = ItemEntry.buildItemUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            case AFFECTED_ITEM: {
                long _id = db.insert(AffectedItemEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    long trans_id = values.getAsLong(AffectedItemEntry.COLUMN_TRANSACTION_ID);
                    returnUri = TransactionEntry.buildTransactionUri(trans_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
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
            case CATEGORY: tableName = CategoryEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case AFFECTED_ITEM: tableName = AffectedItemEntry.TABLE_NAME; break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
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
            case CATEGORY: tableName = CategoryEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case AFFECTED_ITEM: tableName = AffectedItemEntry.TABLE_NAME; break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
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
            case CATEGORY: tableName = CategoryEntry.TABLE_NAME; break;
            case TRANSACTION: tableName = TransactionEntry.TABLE_NAME; break;
            case ITEM: tableName = ItemEntry.TABLE_NAME; break;
            case AFFECTED_ITEM: tableName = AffectedItemEntry.TABLE_NAME; break;

            default:
                return super.bulkInsert(uri, values);
        }

        db.beginTransaction();
        int returnCount = 0;
        try {
            for (ContentValues value : values) {
                long _id = db.insert(tableName, null, value);
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
