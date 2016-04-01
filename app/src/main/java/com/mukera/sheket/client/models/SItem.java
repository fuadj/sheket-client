package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gamma on 3/3/16.
 */
public class SItem extends ChangeTraceable implements Parcelable {
    public static final String JSON_ITEM_ID = "item_id";
    public static final String JSON_MODEL_YEAR = "model_year";
    public static final String JSON_PART_NUMBER = "part_number";
    public static final String JSON_BAR_CODE = "bar_code";
    public static final String JSON_MANUAL_CODE = "manual_code";
    public static final String JSON_HAS_BAR_CODE = "has_bar_code";

    private static final String LOG_TAG = SItem.class.getSimpleName();

    static String _f(String s) {
        return ItemEntry._full(s);
    }

    public static final String[] ITEM_COLUMNS = {
            _f(ItemEntry.COLUMN_COMPANY_ID),
            _f(ItemEntry.COLUMN_ITEM_ID),
            _f(ItemEntry.COLUMN_NAME),
            _f(ItemEntry.COLUMN_MODEL_YEAR),
            _f(ItemEntry.COLUMN_PART_NUMBER),
            _f(ItemEntry.COLUMN_BAR_CODE),
            _f(ItemEntry.COLUMN_HAS_BAR_CODE),
            _f(ItemEntry.COLUMN_MANUAL_CODE),
            _f(COLUMN_CHANGE_INDICATOR)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_ITEM_ID = 1;
    public static final int COL_NAME = 2;
    public static final int COL_MODEL_YEAR = 3;
    public static final int COL_PART_NUMBER = 4;
    public static final int COL_BAR_CODE = 5;
    public static final int COL_HAS_BAR_CODE = 6;
    public static final int COL_MANUAL_CODE = 7;
    public static final int COL_CHANGE_INDICATOR = 8;

    public static final int COL_LAST = 9;

    public long company_id;
    public long item_id;
    public String name;
    public String model_year;
    public String part_number;
    public String bar_code;
    public boolean has_bar_code;
    public String manual_code;

    public SItem() {
    }

    public SItem(Cursor cursor) {
        this(cursor, 0);
    }

    public SItem(Cursor cursor, int offset) {
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        item_id = cursor.getLong(COL_ITEM_ID + offset);
        name = cursor.getString(COL_NAME + offset);
        model_year = cursor.getString(COL_MODEL_YEAR + offset);
        part_number = cursor.getString(COL_PART_NUMBER + offset);
        bar_code = cursor.getString(COL_BAR_CODE + offset);
        has_bar_code = SheketContract.toBool(cursor.getInt(COL_HAS_BAR_CODE + offset));
        manual_code = cursor.getString(COL_MANUAL_CODE + offset);
        change_status = cursor.getInt(COL_CHANGE_INDICATOR + offset);
    }

    private SItem(Parcel parcel) {
        company_id = parcel.readLong();
        item_id = parcel.readLong();
        name = parcel.readString();
        model_year = parcel.readString();
        part_number = parcel.readString();
        bar_code = parcel.readString();
        has_bar_code = SheketContract.toBool(parcel.readInt());
        manual_code = parcel.readString();
        change_status = parcel.readInt();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_COMPANY_ID, company_id);
        values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(ItemEntry.COLUMN_NAME, name);
        values.put(ItemEntry.COLUMN_MODEL_YEAR, model_year);
        values.put(ItemEntry.COLUMN_PART_NUMBER, part_number);
        values.put(ItemEntry.COLUMN_BAR_CODE, bar_code);
        // convert to int before sending it to content provider
        values.put(ItemEntry.COLUMN_HAS_BAR_CODE,
                SheketContract.toInt(has_bar_code));
        values.put(ItemEntry.COLUMN_MANUAL_CODE, manual_code);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_ITEM_ID, item_id);
        result.put(JSON_MODEL_YEAR, model_year);
        result.put(JSON_PART_NUMBER, part_number);
        result.put(JSON_BAR_CODE, bar_code);
        result.put(JSON_MANUAL_CODE, manual_code);
        result.put(JSON_HAS_BAR_CODE, has_bar_code);
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(item_id);
        dest.writeString(name);
        dest.writeString(model_year);
        dest.writeString(part_number);
        dest.writeString(bar_code);
        dest.writeInt(SheketContract.toInt(has_bar_code));
        dest.writeString(manual_code);
        dest.writeInt(change_status);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }


    public static final Parcelable.Creator<SItem> CREATOR = new Parcelable.Creator<SItem>() {
        @Override
        public SItem createFromParcel(Parcel source) {
            return new SItem(source);
        }

        @Override
        public SItem[] newArray(int size) {
            return new SItem[size];
        }
    };
}

