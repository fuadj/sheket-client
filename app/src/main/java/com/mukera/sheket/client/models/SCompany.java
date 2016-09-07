package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.mukera.sheket.client.data.SheketContract.*;

/**
 * Created by fuad on 8/30/16.
 */
public class SCompany implements Parcelable {
    static String f(String s) {
        return CompanyEntry._full(s);
    }

    public static final String[] COMPANY_COLUMNS = {
            f(CompanyEntry.COLUMN_COMPANY_ID),
            f(CompanyEntry.COLUMN_USER_ID),
            f(CompanyEntry.COLUMN_NAME),
            f(CompanyEntry.COLUMN_PERMISSION),
            f(CompanyEntry.COLUMN_STATE_BACKUP),
            f(CompanyEntry.COLUMN_PAYMENT_CERTIFICATE),
            f(CompanyEntry.COLUMN_PAYMENT_STATE)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_USER_ID = 1;
    public static final int COL_COMPANY_NAME = 2;
    public static final int COL_PERMISSION = 3;
    public static final int COL_STATE_BKUP = 4;
    public static final int COL_PAYMENT_CERTIFICATE = 5;
    public static final int COL_PAYMENT_STATE = 6;

    public static final int COL_LAST = 7;

    public long company_id;
    public long user_id;
    public String name;
    public String encoded_permission;
    public String state_bkup;
    public String payment_certificate;
    public int payment_state;

    public SCompany() {
    }

    public SCompany(Cursor cursor) {
        this(cursor, 0);
    }

    public SCompany(Cursor cursor, int offset) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        user_id = cursor.getLong(COL_USER_ID + offset);
        name = cursor.getString(COL_COMPANY_NAME + offset);
        encoded_permission = cursor.getString(COL_PERMISSION + offset);
        state_bkup = cursor.getString(COL_STATE_BKUP + offset);
        payment_certificate = cursor.getString(COL_PAYMENT_CERTIFICATE + offset);
        payment_state = cursor.getInt(COL_PAYMENT_STATE + offset);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CompanyEntry.COLUMN_COMPANY_ID, company_id);
        values.put(CompanyEntry.COLUMN_USER_ID, user_id);
        values.put(CompanyEntry.COLUMN_NAME, name);
        values.put(CompanyEntry.COLUMN_PERMISSION, encoded_permission);
        values.put(CompanyEntry.COLUMN_STATE_BACKUP, state_bkup);
        values.put(CompanyEntry.COLUMN_PAYMENT_CERTIFICATE, payment_certificate);
        values.put(CompanyEntry.COLUMN_PAYMENT_STATE, payment_state);

        return values;
    }

    private SCompany(Parcel parcel) {
        company_id = parcel.readLong();
        user_id = parcel.readLong();
        name = parcel.readString();
        encoded_permission = parcel.readString();
        state_bkup = parcel.readString();
        payment_certificate = parcel.readString();
        payment_state = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(user_id);
        dest.writeString(name);
        dest.writeString(encoded_permission);
        dest.writeString(state_bkup);
        dest.writeString(payment_certificate);
        dest.writeInt(payment_state);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Parcelable.Creator<SCompany> CREATOR = new Parcelable.Creator<SCompany>() {
        @Override
        public SCompany createFromParcel(Parcel source) {
            return new SCompany(source);
        }

        @Override
        public SCompany[] newArray(int size) {
            return new SCompany[size];
        }
    };
}
