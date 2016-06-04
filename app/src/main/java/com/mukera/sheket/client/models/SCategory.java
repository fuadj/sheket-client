package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.ConditionVariable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mukera.sheket.client.controller.util.Utils;
import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by fuad on 5/21/16.
 */
public class SCategory extends UUIDSyncable implements Parcelable {

    public static final String JSON_CATEGORY_ID = "category_id";
    public static final String JSON_CATEGORY_UUID = "client_uuid";
    public static final String JSON_NAME = "name";
    public static final String JSON_PARENT_ID = "parent_id";

    static String _f(String s) { return CategoryEntry._full(s); }
    public static final String[] CATEGORY_COLUMNS = {
            _f(CategoryEntry.COLUMN_COMPANY_ID),
            _f(CategoryEntry.COLUMN_CATEGORY_ID),
            _f(CategoryEntry.COLUMN_NAME),
            _f(CategoryEntry.COLUMN_PARENT_ID),
            _f(COLUMN_CHANGE_INDICATOR),
            _f(COLUMN_UUID)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_CATEGORY_ID = 1;
    public static final int COL_NAME = 2;
    public static final int COL_PARENT_ID = 3;
    public static final int COL_CHANGE_INDICATOR = 4;
    public static final int COL_CLIENT_UUID = 5;

    public static final int COL_LAST = 6;

    public long company_id;
    public long category_id;
    public String name;
    public long parent_id;

    public SCategory() {}

    public SCategory(Cursor cursor) {
        this(cursor, 0);
    }

    public SCategory(Cursor cursor, int offset) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        category_id = cursor.getLong(COL_CATEGORY_ID + offset);
        name = Utils.toTitleCase(cursor.getString(COL_NAME + offset));
        parent_id = cursor.getLong(COL_PARENT_ID + offset);

        change_status = cursor.getInt(COL_CHANGE_INDICATOR + offset);
        client_uuid = cursor.getString(COL_CLIENT_UUID + offset);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CategoryEntry.COLUMN_COMPANY_ID, company_id);
        values.put(CategoryEntry.COLUMN_CATEGORY_ID, category_id);
        values.put(CategoryEntry.COLUMN_NAME, name);
        values.put(CategoryEntry.COLUMN_PARENT_ID, parent_id);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        values.put(COLUMN_UUID, client_uuid);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(JSON_NAME, name);
        obj.put(JSON_CATEGORY_ID, category_id);
        obj.put(JSON_PARENT_ID, parent_id);
        obj.put(JSON_CATEGORY_UUID, client_uuid);
        return obj;
    }

    private SCategory(Parcel parcel) {
        company_id = parcel.readLong();
        category_id = parcel.readLong();
        name = parcel.readString();
        parent_id = parcel.readLong();
        change_status = parcel.readInt();
        client_uuid = parcel.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(category_id);
        dest.writeString(name);
        dest.writeLong(parent_id);
        dest.writeInt(change_status);
        dest.writeString(client_uuid);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }


    public static final Parcelable.Creator<SCategory> CREATOR = new Parcelable.Creator<SCategory>() {
        @Override
        public SCategory createFromParcel(Parcel source) {
            return new SCategory(source);
        }

        @Override
        public SCategory[] newArray(int size) {
            return new SCategory[size];
        }
    };
}
