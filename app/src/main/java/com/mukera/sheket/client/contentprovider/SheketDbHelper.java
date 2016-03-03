package com.mukera.sheket.client.contentprovider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mukera.sheket.client.contentprovider.SheketContract.*;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "mukera.sheket.db";

    public SheketDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (db.isReadOnly()) return;

        db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_CATEGORY_TABLE = "CREATE TABLE " + CategoryEntry.TABLE_NAME + " (" +
                CategoryEntry._ID + " INTEGER PRIMARY KEY, " +
                CategoryEntry.COLUMN_NAME + " TEXT NOT NULL);";

        final String SQL_CREATE_TRANSACTION_TABLE = "CREATE TABLE " + TransactionEntry.TABLE_NAME + " (" +
                TransactionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TransactionEntry.COLUMN_TYPE + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                TransactionEntry.COLUMN_QTY_TOTAL + " INTEGER NOT NULL);";

        final String SQL_CREATE_ITEM_TABLE = "CREATE TABLE " + ItemEntry.TABLE_NAME + " (" +
                ItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ItemEntry.COLUMN_CATEGORY_ID + " INTEGER NOT NULL, " +
                ItemEntry.COLUMN_NAME + " TEXT, " +
                ItemEntry.COLUMN_LOCATION + " TEXT, " +
                ItemEntry.COLUMN_DESC + " TEXT, " +
                ItemEntry.COLUMN_QTY_REMAIN + " REAL NOT NULL, " +

                ItemEntry.COLUMN_CODE_TYPE + " INTEGER NOT NULL, " +
                ItemEntry.COLUMN_BAR_CODE + " TEXT, " +
                ItemEntry.COLUMN_MANUAL_CODE + " TEXT, " +
                " FOREIGN KEY (" + ItemEntry.COLUMN_CATEGORY_ID + ") references " +
                    CategoryEntry.TABLE_NAME + "(" + CategoryEntry._ID + ") ON DELETE CASCADE);";

        final String SQL_CREATE_AFFECTED_TABLE = "CREATE TABLE " + AffectedItemEntry.TABLE_NAME + " (" +
                AffectedItemEntry.COLUMN_TRANSACTION_ID + " INTEGER NOT NULL," +
                AffectedItemEntry.COLUMN_ITEM_ID + " INTEGER NOT NULL," +
                AffectedItemEntry.COLUMN_QTY + " REAL NOT NULL, " +
                " FOREIGN KEY (" + AffectedItemEntry.COLUMN_TRANSACTION_ID + ") REFERENCES " +
                    TransactionEntry.TABLE_NAME + " (" + TransactionEntry._ID + ") ON DELETE CASCADE, " +
                " FOREIGN KEY (" + AffectedItemEntry.COLUMN_ITEM_ID + ") REFERENCES " +
                    ItemEntry.TABLE_NAME + " (" + ItemEntry._ID + ") ON DELETE CASCADE);";

        db.execSQL(SQL_CREATE_CATEGORY_TABLE);
        db.execSQL(SQL_CREATE_TRANSACTION_TABLE);
        db.execSQL(SQL_CREATE_ITEM_TABLE);
        db.execSQL(SQL_CREATE_AFFECTED_TABLE);

        if (!checkIfAnyCategoryExists(db)) {
            ContentValues defaultCategory = new ContentValues();

            defaultCategory.put(CategoryEntry._ID, CategoryEntry.DEFAULT_CATEGORY_ID);
            defaultCategory.put(CategoryEntry.COLUMN_NAME, CategoryEntry.DEFAULT_CATEGORY_NAME);

            long id = db.insert(CategoryEntry.TABLE_NAME, null, defaultCategory);
            if (id <= 0) {      // this means error occurred
                displayErrorAndExit();
            }

            // check if it actually worked
            if (!checkIfAnyCategoryExists(db)) {
                displayErrorAndExit();
            }
        }
    }

    void displayErrorAndExit() {

    }

    boolean checkIfAnyCategoryExists(SQLiteDatabase db) {
        String[] columns = new String[]{CategoryEntry._ID, CategoryEntry.COLUMN_NAME};

        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, columns,
                null, // check if there is ANY category(no selection)
                null, null, null, null);

        boolean exists = cursor.moveToNext();
        cursor.close();
        return exists;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
