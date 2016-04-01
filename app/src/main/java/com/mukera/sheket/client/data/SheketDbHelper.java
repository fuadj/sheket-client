package com.mukera.sheket.client.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mukera.sheket.client.data.SheketContract.*;

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

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String sql_create_company_table = "create table if not exists " + CompanyEntry.TABLE_NAME + " ( " +
                CompanyEntry.COLUMN_ID + " integer primary key, " +
                CompanyEntry.COLUMN_NAME + " text not null);";

        final String sql_create_branch_table = "create table if not exists " + BranchEntry.TABLE_NAME + " ( " +
                BranchEntry.COLUMN_BRANCH_ID + " integer primary key, " +
                BranchEntry.COLUMN_COMPANY_ID + " integer not null, " +
                BranchEntry.COLUMN_NAME + " text not null, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                BranchEntry.COLUMN_LOCATION + " text);";

        final String sql_create_item_table = "create table if not exists " + ItemEntry.TABLE_NAME + " ( " +
                ItemEntry.COLUMN_ITEM_ID + " integer primary key, " +
                ItemEntry.COLUMN_COMPANY_ID + " integer not null, " +
                ItemEntry.COLUMN_NAME + " text not null, " +
                ItemEntry.COLUMN_MODEL_YEAR + " text, " +
                ItemEntry.COLUMN_PART_NUMBER + " text, " +
                ItemEntry.COLUMN_MANUAL_CODE + " text, " +
                ItemEntry.COLUMN_BAR_CODE + " text, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                // b/c there is not bool type in sqlite
                ItemEntry.COLUMN_HAS_BAR_CODE + " integer not null);";

        final String sql_create_branch_item_table = "create table if not exists " + BranchItemEntry.TABLE_NAME + " ( " +
                BranchItemEntry.COLUMN_COMPANY_ID + " integer not null, " +
                BranchItemEntry.COLUMN_BRANCH_ID + cascadeUpdate(BranchEntry.TABLE_NAME, BranchEntry.COLUMN_BRANCH_ID) +
                BranchItemEntry.COLUMN_ITEM_ID + cascadeUpdate(ItemEntry.TABLE_NAME, ItemEntry.COLUMN_ITEM_ID) +
                BranchItemEntry.COLUMN_ITEM_LOCATION + " text, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                BranchItemEntry.COLUMN_QUANTITY + " real);";

        final String sql_create_transaction_table = "create table if not exists " + TransactionEntry.TABLE_NAME + " ( " +
                TransactionEntry.COLUMN_TRANS_ID + " integer primary key, " +
                TransactionEntry.COLUMN_COMPANY_ID + " integer not null, " +
                TransactionEntry.COLUMN_BRANCH_ID + cascadeUpdate(BranchEntry.TABLE_NAME, BranchEntry.COLUMN_BRANCH_ID) +
                TransactionEntry.COLUMN_USER_ID + " integer not null, " +
                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +
                TransactionEntry.COLUMN_DATE + " integer not null);";

        final String sql_create_transaction_items_table = "create table if not exists " + TransItemEntry.TABLE_NAME + " ( " +
                TransItemEntry.COLUMN_COMPANY_ID + " integer not null, " +
                TransItemEntry.COLUMN_TRANSACTION_ID + cascadeUpdate(TransactionEntry.TABLE_NAME, TransactionEntry.COLUMN_TRANS_ID) +
                TransItemEntry.COLUMN_ITEM_ID + cascadeUpdate(ItemEntry.TABLE_NAME, ItemEntry.COLUMN_ITEM_ID) +
                TransItemEntry.COLUMN_TRANSACTION_TYPE + " integer not null, " +

                // This can't be automatically updated on foreign key changes because
                // Not always do transactions involve other branch, so we can't enforce a foreign key,
                // We only set this id if another branch was affected, and that doesn't always happen
                TransItemEntry.COLUMN_OTHER_BRANCH_ID + " integer, " +

                ChangeTraceable.COLUMN_CHANGE_INDICATOR + " integer not null, " +

                TransItemEntry.COLUMN_QTY + " real not null);";

        db.execSQL(sql_create_company_table);
        db.execSQL(sql_create_branch_table);
        db.execSQL(sql_create_branch_item_table);
        db.execSQL(sql_create_item_table);
        db.execSQL(sql_create_transaction_table);
        db.execSQL(sql_create_transaction_items_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
