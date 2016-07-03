package com.mukera.sheket.client.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mukera.sheket.client.data.SheketContract.*;

import java.util.ArrayList;

/**
 * Created by gamma on 3/2/16.
 */
public class SheketDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "com.sheket.client.db";

    public SheketDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (db.isReadOnly()) return;

        db.execSQL("PRAGMA foreign_keys = ON");
    }

    String cascadeUpdate(String table_name, String col_name) {
        return String.format(" integer REFERENCES %s(%s) ON UPDATE CASCADE, ", table_name, col_name);
    }

    String cascadeUpdateAndDelete(String table_name, String col_name) {
        return String.format(" integer REFERENCES %s(%s) ON UPDATE CASCADE ON DELETE CASCADE, ", table_name,
                col_name);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String sql_create_company_table = "create table if not exists " + CompanyEntry.TABLE_NAME + " ( " +
                CompanyEntry.COLUMN_ID + " integer primary key ON CONFLICT REPLACE, " +
                CompanyEntry.COLUMN_NAME + " text not null, " +
                CompanyEntry.COLUMN_PERMISSION + " text not null, " +
                // This can be empty because it might be the user's first time
                CompanyEntry.COLUMN_REVISIONS + " text);";

        final String sql_create_members_table = "create table if not exists " + MemberEntry.TABLE_NAME + " ( " +
                MemberEntry.COLUMN_COMPANY_ID + " integer not null, " +
                MemberEntry.COLUMN_MEMBER_ID + " integer not null, " +
                MemberEntry.COLUMN_MEMBER_NAME + " text not null, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                MemberEntry.COLUMN_MEMBER_PERMISSION + " text not null, " +
                " UNIQUE (" +
                MemberEntry.COLUMN_COMPANY_ID + ", " + MemberEntry.COLUMN_MEMBER_ID + ") ON CONFLICT REPLACE);";

        final String sql_create_branch_table = "create table if not exists " + BranchEntry.TABLE_NAME + " ( " +
                BranchEntry.COLUMN_BRANCH_ID + " integer primary key ON CONFLICT REPLACE, " +
                BranchEntry.COLUMN_COMPANY_ID + " integer not null, " +
                BranchEntry.COLUMN_NAME + " text not null, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                UUIDSyncable.COLUMN_UUID + " text, " +
                BranchEntry.COLUMN_LOCATION + " text);";

        final String sql_create_category_table = "create table if not exists " + CategoryEntry.TABLE_NAME + " ( " +
                CategoryEntry.COLUMN_CATEGORY_ID + " integer primary key on conflict replace, " +
                CategoryEntry.COLUMN_COMPANY_ID + " integer not null, " +
                CategoryEntry.COLUMN_NAME + " text not null, " +

                String.format("%s INTEGER DEFAULT %s REFERENCES %s(%s) ON UPDATE CASCADE ON DELETE SET DEFAULT, ",
                        CategoryEntry.COLUMN_PARENT_ID,
                        Long.toString(CategoryEntry.ROOT_CATEGORY_ID),
                        CategoryEntry.TABLE_NAME,
                        CategoryEntry.COLUMN_CATEGORY_ID) +

                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                UUIDSyncable.COLUMN_UUID + " text);";


        final String sql_create_item_table = "create table if not exists " + ItemEntry.TABLE_NAME + " ( " +
                ItemEntry.COLUMN_ITEM_ID + " integer primary key ON CONFLICT REPLACE, " +
                ItemEntry.COLUMN_COMPANY_ID + " integer not null, " +

                String.format("%s INTEGER DEFAULT %s REFERENCES %s(%s) ON UPDATE CASCADE ON DELETE SET DEFAULT, ",
                        ItemEntry.COLUMN_CATEGORY_ID,
                        Long.toString(CategoryEntry.ROOT_CATEGORY_ID),
                        CategoryEntry.TABLE_NAME,
                        CategoryEntry.COLUMN_CATEGORY_ID) +

                ItemEntry.COLUMN_ITEM_CODE + " text not null, " +
                ItemEntry.COLUMN_NAME + " text, " +

                ItemEntry.COLUMN_UNIT_OF_MEASUREMENT + " integer not null, " +
                // b/c sqlite doesn't support boolean type
                ItemEntry.COLUMN_HAS_DERIVED_UNIT + " integer not null, " +
                ItemEntry.COLUMN_DERIVED_UNIT_NAME + " text, " +
                ItemEntry.COLUMN_DERIVED_UNIT_FACTOR + " real, " +

                ItemEntry.COLUMN_REORDER_LEVEL + " real, " +

                ItemEntry.COLUMN_MODEL_YEAR + " text, " +
                ItemEntry.COLUMN_PART_NUMBER + " text, " +
                ItemEntry.COLUMN_BAR_CODE + " text, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                UUIDSyncable.COLUMN_UUID + " text, " +
                // b/c there is not bool type in sqlite
                ItemEntry.COLUMN_HAS_BAR_CODE + " integer null);";

        final String sql_create_branch_item_table = "create table if not exists " + BranchItemEntry.TABLE_NAME + " ( " +
                BranchItemEntry.COLUMN_COMPANY_ID + " integer not null, " +
                BranchItemEntry.COLUMN_BRANCH_ID + cascadeUpdate(BranchEntry.TABLE_NAME, BranchEntry.COLUMN_BRANCH_ID) +
                BranchItemEntry.COLUMN_ITEM_ID + cascadeUpdate(ItemEntry.TABLE_NAME, ItemEntry.COLUMN_ITEM_ID) +
                BranchItemEntry.COLUMN_ITEM_LOCATION + " text, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                BranchItemEntry.COLUMN_QUANTITY + " real, " +
                " UNIQUE( " +
                BranchItemEntry.COLUMN_COMPANY_ID + ", " + BranchItemEntry.COLUMN_BRANCH_ID + ", " +
                BranchItemEntry.COLUMN_ITEM_ID + ") ON CONFLICT REPLACE);";

        final String sql_create_transaction_table = "create table if not exists " + TransactionEntry.TABLE_NAME + " ( " +
                TransactionEntry.COLUMN_TRANS_ID + " integer primary key ON CONFLICT REPLACE, " +
                TransactionEntry.COLUMN_COMPANY_ID + " integer not null, " +
                TransactionEntry.COLUMN_BRANCH_ID + cascadeUpdate(BranchEntry.TABLE_NAME, BranchEntry.COLUMN_BRANCH_ID) +
                TransactionEntry.COLUMN_USER_ID + " integer not null, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                UUIDSyncable.COLUMN_UUID + " text, " +
                TransactionEntry.COLUMN_TRANS_NOTE + " text, " +
                TransactionEntry.COLUMN_DATE + " integer not null);";

        final String sql_create_transaction_items_table = "create table if not exists " + TransItemEntry.TABLE_NAME + " ( " +
                TransItemEntry.COLUMN_COMPANY_ID + " integer not null, " +

                TransItemEntry.COLUMN_TRANSACTION_ID + cascadeUpdateAndDelete(TransactionEntry.TABLE_NAME, TransactionEntry.COLUMN_TRANS_ID) +

                TransItemEntry.COLUMN_ITEM_ID + cascadeUpdate(ItemEntry.TABLE_NAME, ItemEntry.COLUMN_ITEM_ID) +
                TransItemEntry.COLUMN_TRANSACTION_TYPE + " integer not null, " +

                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +

                /**
                 * This guarantees any updated branch id's are also appropriately updated here
                 * Although a transaction might not affect another branch, this column
                 * will always refer to an actual row in the branches table. If in-fact the transaction
                 * doesn't affect another branch, it will point to the dummy branch.
                 */
                TransItemEntry.COLUMN_OTHER_BRANCH_ID + cascadeUpdate(BranchEntry.TABLE_NAME, BranchEntry.COLUMN_BRANCH_ID) +

                TransItemEntry.COLUMN_QTY + " real not null, " +
                TransItemEntry.COLUMN_ITEM_NOTE + " text);";

        db.execSQL(sql_create_company_table);
        db.execSQL(sql_create_members_table);
        db.execSQL(sql_create_branch_table);

        db.execSQL(sql_create_category_table);
        db.execSQL(sql_create_item_table);

        db.execSQL(sql_create_branch_item_table);
        db.execSQL(sql_create_transaction_table);
        db.execSQL(sql_create_transaction_items_table);

        try {
            createDummyBranch(db);
            createRootCategory(db);
        } catch (SheketDbException e) {
            Log.e("SheketDbHelper", e.getMessage());
        }
    }

    static class SheketDbException extends Exception {
        public SheketDbException() { super(); }
        public SheketDbException(String detailMessage) { super(detailMessage); }
        public SheketDbException(String detailMessage, Throwable throwable) { super(detailMessage, throwable); }
        public SheketDbException(Throwable throwable) { super(throwable); }
    }

    void createDummyBranch(SQLiteDatabase db) throws SheketDbException {
        Cursor cursor = db.query(BranchEntry.TABLE_NAME, new String[]{BranchEntry.COLUMN_BRANCH_ID},
                BranchEntry.COLUMN_BRANCH_ID + " = ?",
                new String[]{String.valueOf(BranchEntry.DUMMY_BRANCH_ID)},
                null, null, null);
        if (cursor == null) {
            throw new SheketDbException("CreateDummyBranch error");
        }
        if (!cursor.moveToFirst()) {    // if the branch doesn't exist, create it
            cursor.close();
            ContentValues values = new ContentValues();
            // By Setting the company_id to 0, we can guarantee that no company
            // will EVER see this dummy branch.
            values.put(BranchEntry.COLUMN_COMPANY_ID, CompanyBase.DUMMY_COMPANY_ID);
            values.put(BranchEntry.COLUMN_BRANCH_ID, BranchEntry.DUMMY_BRANCH_ID);
            values.put(BranchEntry.COLUMN_NAME, "Dummy Branch");
            // we don't want this to sync with the server
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                    ChangeTraceable.CHANGE_STATUS_SYNCED);
            db.insert(BranchEntry.TABLE_NAME, null, values);
        } else {
            cursor.close();
        }
    }

    void createRootCategory(SQLiteDatabase db) throws SheketDbException {
        Cursor cursor = db.query(CategoryEntry.TABLE_NAME, new String[]{CategoryEntry.COLUMN_CATEGORY_ID},
                CategoryEntry.COLUMN_CATEGORY_ID + " = ?",
                new String[]{String.valueOf(CategoryEntry.ROOT_CATEGORY_ID)},
                null, null, null);
        if (cursor == null)  {
            throw new SheketDbException("CreateRootCategory query error");
        }
        if (!cursor.moveToFirst()) {    // the category doesn't exist
            cursor.close();

            ContentValues values = new ContentValues();

            /**
             * Since we've set the DEFAULT parent id to be {@code ROOT_CATEGORY}, it will be
             * used if we don't specify a parent id in the ContentValues. So we first create the
             * root's parent. It will initially have it's parent set to the {@code ROOT_CATEGORY}.
             * We then change that to refer to itself(i.e: the root's parent will have it's parent set to itself).
             * We finally create the root and set its parent to the parent. I know, it is confusing.
             */
            values.put(CategoryEntry.COLUMN_COMPANY_ID, CompanyBase.DUMMY_COMPANY_ID);
            values.put(CategoryEntry.COLUMN_CATEGORY_ID, CategoryEntry._ROOT_CATEGORY_PARENT_ID);
            values.put(CategoryEntry.COLUMN_NAME, "__root's parent__");
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_SYNCED);

            long row_id = db.insert(CategoryEntry.TABLE_NAME, null, values);
            if (row_id == -1) {
                throw new SheketDbException("CreateRootCategory insert root's category parent error");
            }

            // now update the root's parent to have its parent set to itself
            values.put(CategoryEntry.COLUMN_PARENT_ID, CategoryEntry._ROOT_CATEGORY_PARENT_ID);
            long num_updated = db.update(CategoryEntry.TABLE_NAME, values,
                    CategoryEntry.COLUMN_CATEGORY_ID + " = ?",
                    new String[]{String.valueOf(CategoryEntry._ROOT_CATEGORY_PARENT_ID)});
            if (num_updated != 1) {
                throw new SheketDbException("CreateRootCategory update root's category parent error");
            }

            values = new ContentValues();
            values.put(CategoryEntry.COLUMN_COMPANY_ID, CompanyBase.DUMMY_COMPANY_ID);
            values.put(CategoryEntry.COLUMN_CATEGORY_ID, CategoryEntry.ROOT_CATEGORY_ID);
            values.put(CategoryEntry.COLUMN_NAME, "__root category__");
            values.put(CategoryEntry.COLUMN_PARENT_ID, CategoryEntry._ROOT_CATEGORY_PARENT_ID);
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_SYNCED);

            row_id = db.insert(CategoryEntry.TABLE_NAME, null, values);
            if (row_id == -1) {
                throw new SheketDbException("CreateRootCategory insert category root error");
            }
        } else {
            cursor.close();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    /**
     * Used by AndroidDatabaseManager
     */
    public ArrayList<Cursor> getData(String Query) {
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[]{"mesage"};
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2 = new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);

        try {
            String maxQuery = Query;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);

            //add value to cursor2
            Cursor2.addRow(new Object[]{"Success"});

            alc.set(1, Cursor2);
            if (null != c && c.getCount() > 0) {
                alc.set(0, c);
                c.moveToFirst();

                return alc;
            }
            return alc;
        } catch (SQLException sqlEx) {
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + sqlEx.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        } catch (Exception ex) {
            Log.d("printing exception", ex.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + ex.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        }
    }
}
