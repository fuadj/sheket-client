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
public class SItem extends UUIDSyncable implements Parcelable {
    public static final String JSON_ITEM_ID = "item_id";
    public static final String JSON_ITEM_UUID = "client_uuid";
    public static final String JSON_ITEM_NAME = "item_name";

    public static final String JSON_UNIT_OF_MEASUREMENT = "units";
    public static final String JSON_HAS_DERIVED_UNIT = "has_derived_unit";
    public static final String JSON_DERIVED_NAME = "derived_name";
    public static final String JSON_DERIVED_FACTOR = "derived_factor";
    public static final String JSON_REORDER_LEVEL = "reorder_level";

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

            _f(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT),
            _f(ItemEntry.COLUMN_HAS_DERIVED_UNIT),
            _f(ItemEntry.COLUMN_DERIVED_UNIT_NAME),
            _f(ItemEntry.COLUMN_DERIVED_UNIT_FACTOR),

            _f(ItemEntry.COLUMN_REORDER_LEVEL),

            _f(ItemEntry.COLUMN_MODEL_YEAR),
            _f(ItemEntry.COLUMN_PART_NUMBER),
            _f(ItemEntry.COLUMN_BAR_CODE),
            _f(ItemEntry.COLUMN_HAS_BAR_CODE),
            _f(ItemEntry.COLUMN_MANUAL_CODE),
            _f(COLUMN_CHANGE_INDICATOR),
            _f(COLUMN_UUID)
    };

    // columns of "SItem" + "SBranchItem" + "SBranch" combined!!!
    public static final String[] ITEM_WITH_BRANCH_DETAIL_COLUMNS;

    static {
        int items_size = ITEM_COLUMNS.length;
        int branch_items_size = SBranchItem.BRANCH_ITEM_COLUMNS.length;
        int branch_size = SBranch.BRANCH_COLUMNS.length;

        int total_size = items_size + branch_items_size + branch_size;
        ITEM_WITH_BRANCH_DETAIL_COLUMNS = new String[total_size];

        System.arraycopy(ITEM_COLUMNS, 0, ITEM_WITH_BRANCH_DETAIL_COLUMNS,
                0, items_size);
        System.arraycopy(SBranchItem.BRANCH_ITEM_COLUMNS, 0, ITEM_WITH_BRANCH_DETAIL_COLUMNS,
                items_size, branch_items_size);
        System.arraycopy(SBranch.BRANCH_COLUMNS, 0, ITEM_WITH_BRANCH_DETAIL_COLUMNS,
                items_size + branch_items_size, branch_size);
    }

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_ITEM_ID = 1;
    public static final int COL_NAME = 2;

    public static final int COL_UNIT_OF_MEASUREMENT = 3;
    public static final int COL_HAS_DERIVED_UNIT = 4;
    public static final int COL_DERIVED_UNIT_NAME = 5;
    public static final int COL_DERIVED_UNIT_FACTOR = 6;
    public static final int COL_REORDER_LEVEL = 7;

    public static final int COL_MODEL_YEAR = 8;
    public static final int COL_PART_NUMBER = 9;
    public static final int COL_BAR_CODE = 10;
    public static final int COL_HAS_BAR_CODE = 11;
    public static final int COL_MANUAL_CODE = 12;
    public static final int COL_CHANGE_INDICATOR = 13;
    public static final int COL_CLIENT_UUID = 14;

    public static final int COL_LAST = 15;

    public long company_id;
    public long item_id;
    public String name;

    public int unit_of_measurement;
    public boolean has_derived_unit;
    public String derived_name;
    public double derived_factor;
    public double reorder_level;

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

        unit_of_measurement = cursor.getInt(COL_UNIT_OF_MEASUREMENT + offset);
        has_derived_unit = SheketContract.toBool(cursor.getInt(COL_HAS_DERIVED_UNIT + offset));

        derived_name = cursor.getString(COL_DERIVED_UNIT_NAME + offset);
        derived_factor = cursor.getDouble(COL_DERIVED_UNIT_FACTOR + offset);
        reorder_level = cursor.getDouble(COL_REORDER_LEVEL + offset);

        model_year = cursor.getString(COL_MODEL_YEAR + offset);
        part_number = cursor.getString(COL_PART_NUMBER + offset);
        bar_code = cursor.getString(COL_BAR_CODE + offset);
        has_bar_code = SheketContract.toBool(cursor.getInt(COL_HAS_BAR_CODE + offset));
        manual_code = cursor.getString(COL_MANUAL_CODE + offset);
        change_status = cursor.getInt(COL_CHANGE_INDICATOR + offset);
        client_uuid = cursor.getString(COL_CLIENT_UUID + offset);
    }

    private SItem(Parcel parcel) {
        company_id = parcel.readLong();
        item_id = parcel.readLong();
        name = parcel.readString();
        unit_of_measurement = parcel.readInt();
        has_derived_unit = SheketContract.toBool(parcel.readInt());
        derived_name = parcel.readString();
        derived_factor = parcel.readDouble();
        reorder_level = parcel.readDouble();
        model_year = parcel.readString();
        part_number = parcel.readString();
        bar_code = parcel.readString();
        has_bar_code = SheketContract.toBool(parcel.readInt());
        manual_code = parcel.readString();
        change_status = parcel.readInt();
        client_uuid = parcel.readString();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_COMPANY_ID, company_id);
        values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(ItemEntry.COLUMN_NAME, name);

        values.put(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT, unit_of_measurement);
        values.put(ItemEntry.COLUMN_HAS_DERIVED_UNIT,
                SheketContract.toInt(has_derived_unit));
        values.put(ItemEntry.COLUMN_DERIVED_UNIT_NAME, derived_name);
        values.put(ItemEntry.COLUMN_DERIVED_UNIT_FACTOR, derived_factor);
        values.put(ItemEntry.COLUMN_REORDER_LEVEL, reorder_level);

        values.put(ItemEntry.COLUMN_MODEL_YEAR, model_year);
        values.put(ItemEntry.COLUMN_PART_NUMBER, part_number);
        values.put(ItemEntry.COLUMN_BAR_CODE, bar_code);
        values.put(ItemEntry.COLUMN_HAS_BAR_CODE,
                SheketContract.toInt(has_bar_code));
        values.put(ItemEntry.COLUMN_MANUAL_CODE, manual_code);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        values.put(COLUMN_UUID, client_uuid);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_ITEM_ID, item_id);
        result.put(JSON_ITEM_NAME, name);
        result.put(JSON_UNIT_OF_MEASUREMENT, unit_of_measurement);
        result.put(JSON_HAS_DERIVED_UNIT, has_derived_unit);
        result.put(JSON_DERIVED_NAME, derived_name);
        result.put(JSON_DERIVED_FACTOR, derived_factor);
        result.put(JSON_REORDER_LEVEL, reorder_level);
        result.put(JSON_MODEL_YEAR, model_year);
        result.put(JSON_PART_NUMBER, part_number);
        result.put(JSON_BAR_CODE, bar_code);
        result.put(JSON_MANUAL_CODE, manual_code);
        result.put(JSON_HAS_BAR_CODE, has_bar_code);
        result.put(JSON_ITEM_UUID, client_uuid);
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(item_id);
        dest.writeString(name);
        dest.writeInt(unit_of_measurement);
        dest.writeInt(SheketContract.toInt(has_derived_unit));
        dest.writeString(derived_name);
        dest.writeDouble(derived_factor);
        dest.writeDouble(reorder_level);
        dest.writeString(model_year);
        dest.writeString(part_number);
        dest.writeString(bar_code);
        dest.writeInt(SheketContract.toInt(has_bar_code));
        dest.writeString(manual_code);
        dest.writeInt(change_status);
        dest.writeString(client_uuid);
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

