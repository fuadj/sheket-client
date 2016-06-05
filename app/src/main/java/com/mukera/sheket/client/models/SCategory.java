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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fuad on 5/21/16.
 */
public class SCategory extends UUIDSyncable implements Parcelable {

    public static final String JSON_CATEGORY_ID = "category_id";
    public static final String JSON_CATEGORY_UUID = "client_uuid";
    public static final String JSON_NAME = "name";
    public static final String JSON_PARENT_ID = "parent_id";

    static String _fP(String s) { return CategoryEntry._fullParent(s); }
    static String _fC(String s) { return CategoryEntry._fullChild(s); }

    public static final String[] CATEGORY_COLUMNS = {
            // Parent category columns
            _fP(CategoryEntry.COLUMN_COMPANY_ID),
            _fP(CategoryEntry.COLUMN_CATEGORY_ID),
            _fP(CategoryEntry.COLUMN_NAME),
            _fP(CategoryEntry.COLUMN_PARENT_ID),
            _fP(COLUMN_CHANGE_INDICATOR),
            _fP(COLUMN_UUID),

            // Child category columns
            _fC(CategoryEntry.COLUMN_COMPANY_ID),
            _fC(CategoryEntry.COLUMN_CATEGORY_ID),
            _fC(CategoryEntry.COLUMN_NAME),
            _fC(CategoryEntry.COLUMN_PARENT_ID),
            _fC(COLUMN_CHANGE_INDICATOR),
            _fC(COLUMN_UUID),
    };

    public static final int COL_P_COMPANY_ID = 0;
    public static final int COL_P_CATEGORY_ID = 1;
    public static final int COL_P_NAME = 2;
    public static final int COL_P_PARENT_ID = 3;
    public static final int COL_P_CHANGE_INDICATOR = 4;
    public static final int COL_P_CLIENT_UUID = 5;

    public static final int COL_C_COMPANY_ID = 6;
    public static final int COL_C_CATEGORY_ID = 7;
    public static final int COL_C_NAME = 8;
    public static final int COL_C_PARENT_ID = 9;
    public static final int COL_C_CHANGE_INDICATOR = 10;
    public static final int COL_C_CLIENT_UUID = 11;

    public static final int COL_LAST = 12;

    public long company_id;
    public long category_id;
    public String name;
    public long parent_id;

    public List<SCategory> childrenCategories;

    public SCategory() {}

    public SCategory(Cursor cursor) {
        this(cursor, 0, true, true);
    }

    private static final int NO_CATEGORY_FOUND_ID = 0;
    public SCategory(Cursor cursor, int offset, boolean is_parent, boolean fetch_children) {
        if (is_parent) {
            company_id = cursor.getLong(COL_P_COMPANY_ID + offset);
            category_id = cursor.getLong(COL_P_CATEGORY_ID + offset);
            name = Utils.toTitleCase(cursor.getString(COL_P_NAME + offset));
            parent_id = cursor.getLong(COL_P_PARENT_ID + offset);

            change_status = cursor.getInt(COL_P_CHANGE_INDICATOR + offset);
            client_uuid = cursor.getString(COL_P_CLIENT_UUID + offset);

            childrenCategories = new ArrayList<>();
            if (fetch_children) {
                do {
                    SCategory child = new SCategory(cursor, offset, false, false);
                    if (child.category_id == NO_CATEGORY_FOUND_ID) {
                        break;
                    }

                    // we've finished this 'parent' category and move to next, so STOP!!!
                    if (category_id != child.parent_id) {
                        cursor.moveToPrevious();
                        break;
                    }
                    childrenCategories.add(child);
                } while (cursor.moveToNext());
            }
        } else {
            if (cursor.getType(COL_C_CATEGORY_ID) == Cursor.FIELD_TYPE_NULL) {
                category_id = NO_CATEGORY_FOUND_ID;
                return;
            }

            company_id = cursor.getLong(COL_C_COMPANY_ID + offset);
            category_id = cursor.getLong(COL_C_CATEGORY_ID + offset);
            name = Utils.toTitleCase(cursor.getString(COL_C_NAME + offset));
            parent_id = cursor.getLong(COL_C_PARENT_ID + offset);

            change_status = cursor.getInt(COL_C_CHANGE_INDICATOR + offset);
            client_uuid = cursor.getString(COL_C_CLIENT_UUID + offset);

            childrenCategories = null;
        }
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
