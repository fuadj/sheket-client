package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Pair;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gamma on 3/3/16.
 */
public class SItem extends UUIDSyncable implements Parcelable {
    public static final String JSON_ITEM_ID = "item_id";
    public static final String JSON_ITEM_UUID = "client_uuid";

    public static final String JSON_ITEM_CODE = "item_code";
    public static final String JSON_ITEM_NAME = "item_name";
    public static final String JSON_ITEM_CATEGORY = "category_id";

    public static final String JSON_UNIT_OF_MEASUREMENT = "units";
    public static final String JSON_HAS_DERIVED_UNIT = "has_derived_unit";
    public static final String JSON_DERIVED_NAME = "derived_name";
    public static final String JSON_DERIVED_FACTOR = "derived_factor";
    public static final String JSON_REORDER_LEVEL = "reorder_level";

    public static final String JSON_MODEL_YEAR = "model_year";
    public static final String JSON_PART_NUMBER = "part_number";
    public static final String JSON_BAR_CODE = "bar_code";
    public static final String JSON_HAS_BAR_CODE = "has_bar_code";

    private static final String LOG_TAG = SItem.class.getSimpleName();

    static String _f(String s) {
        return ItemEntry._full(s);
    }

    public static final String[] ITEM_COLUMNS = {
            _f(ItemEntry.COLUMN_COMPANY_ID),
            _f(ItemEntry.COLUMN_ITEM_ID),
            _f(ItemEntry.COLUMN_NAME),
            _f(ItemEntry.COLUMN_ITEM_CODE),
            _f(ItemEntry.COLUMN_CATEGORY_ID),

            _f(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT),
            _f(ItemEntry.COLUMN_HAS_DERIVED_UNIT),
            _f(ItemEntry.COLUMN_DERIVED_UNIT_NAME),
            _f(ItemEntry.COLUMN_DERIVED_UNIT_FACTOR),

            _f(ItemEntry.COLUMN_REORDER_LEVEL),

            _f(ItemEntry.COLUMN_MODEL_YEAR),
            _f(ItemEntry.COLUMN_PART_NUMBER),
            _f(ItemEntry.COLUMN_BAR_CODE),
            _f(ItemEntry.COLUMN_HAS_BAR_CODE),
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
    public static final int COL_MANUAL_CODE = 3;
    public static final int COL_CATEGORY = 4;

    public static final int COL_UNIT_OF_MEASUREMENT = 5;
    public static final int COL_HAS_DERIVED_UNIT = 6;
    public static final int COL_DERIVED_UNIT_NAME = 7;
    public static final int COL_DERIVED_UNIT_FACTOR = 8;
    public static final int COL_REORDER_LEVEL = 9;

    public static final int COL_MODEL_YEAR = 10;
    public static final int COL_PART_NUMBER = 11;
    public static final int COL_BAR_CODE = 12;
    public static final int COL_HAS_BAR_CODE = 13;
    public static final int COL_CHANGE_INDICATOR = 14;
    public static final int COL_CLIENT_UUID = 15;

    public static final int COL_LAST = 16;

    public long company_id;
    public long item_id;
    public String name;
    public String item_code;
    public long category;

    public int unit_of_measurement;
    public boolean has_derived_unit;
    public String derived_name;
    public double derived_factor;
    public double reorder_level;

    public String model_year;
    public String part_number;
    public String bar_code;
    public boolean has_bar_code;

    /**
     * When joining multiple tables through LEFT/RIGHT joins, if the item
     * table is the empty table, you can check that by checking if the item_id
     * is {@code NO_ITEM_FOUND}.
     */
    public static final int NO_ITEM_FOUND = 0;

    /**
     * These fields {total_quantity, available_branches} aren't really stored with the item table,
     * they are just a convenience when we are fetching it with branch items.
     */
    public double total_quantity;
    public List<Pair<SBranchItem, SBranch>> available_branches;

    public SItem() {
    }

    public SItem(Cursor cursor) {
        this(cursor, 0, false);
    }

    public SItem(Cursor cursor, int offset) {
        this(cursor, offset, false);
    }

    public SItem(Cursor cursor, boolean fetch_branch_items) {
        this(cursor, 0, fetch_branch_items);
    }

    /**
     * If you are fetching it with branch_items, it is when you've made a left join
     * with the branch and branch_item tables. Because the left join has many rows
     * items that exist in many branches, the cursor will be moved.
     * @param cursor
     * @param offset
     * @param fetch_branch_items
     */
    public SItem(Cursor cursor, int offset, boolean fetch_branch_items) {
        if (cursor.isNull(COL_ITEM_ID + offset)) {
            item_id = NO_ITEM_FOUND;
            return;
        }

        item_id = cursor.getLong(COL_ITEM_ID + offset);
        company_id = cursor.getLong(COL_COMPANY_ID + offset);
        name = cursor.getString(COL_NAME + offset);
        item_code = cursor.getString(COL_MANUAL_CODE + offset);
        category = cursor.getLong(COL_CATEGORY + offset);

        unit_of_measurement = cursor.getInt(COL_UNIT_OF_MEASUREMENT + offset);
        has_derived_unit = SheketContract.toBool(cursor.getInt(COL_HAS_DERIVED_UNIT + offset));

        derived_name = cursor.getString(COL_DERIVED_UNIT_NAME + offset);
        derived_factor = cursor.getDouble(COL_DERIVED_UNIT_FACTOR + offset);
        reorder_level = cursor.getDouble(COL_REORDER_LEVEL + offset);

        model_year = cursor.getString(COL_MODEL_YEAR + offset);
        part_number = cursor.getString(COL_PART_NUMBER + offset);
        bar_code = cursor.getString(COL_BAR_CODE + offset);
        has_bar_code = SheketContract.toBool(cursor.getInt(COL_HAS_BAR_CODE + offset));
        change_status = cursor.getInt(COL_CHANGE_INDICATOR + offset);
        client_uuid = cursor.getString(COL_CLIENT_UUID + offset);

        total_quantity = 0.d;
        available_branches = new ArrayList<>();

        if (fetch_branch_items) {
            while (true) {
                SBranchItem branchItem = new SBranchItem(cursor, COL_LAST + offset, false);
                if (branchItem.branch_id == SBranchItem.NO_BRANCH_ITEM_FOUND)
                    break;
                SBranch branch = new SBranch(cursor, COL_LAST + SBranchItem.COL_LAST);
                available_branches.add(new Pair<>(branchItem, branch));
                total_quantity += branchItem.quantity;

                if (!cursor.moveToNext()) // we've hit the end
                    break;
                long next_item_id = cursor.getLong(COL_ITEM_ID + offset);
                if (next_item_id != item_id) {      // we've moved to the territory of another item, get back
                    cursor.moveToPrevious();
                    break;
                }
            }
        }
    }

    /**
     * This cursor is required because the result of the JOINED query with the branch table will
     * have as many rows of the same item for each branch it exists in. This will result
     * in duplicate rows in the UI with the same item. To prevent this, we override the methods
     * in the cursor to only return the "unique" items.
     */
    public static class ItemWithAvailableBranchesCursor extends CursorWrapper {
        /**
         * This is a mapping from "n-th-unique" item --to--> the starting position in the cursor.
         * Since we've overloaded the {@code getCount()}, the number of unique items
         * is <= the total rows inside the cursor. This maps from the "smaller" unique items
         * to their starting positions. This is because the cursor is the result of LEFT JOINING
         * the item table to branch table,
         * there will be many rows with the same item id for different branches. This map
         * holds the positions of the "starting" positions of the items. The size of this
         * map also tells us how many "unique" items there are.
         */
        private Map<Integer, Integer> mItemStartPosition;

        public ItemWithAvailableBranchesCursor(Cursor cursor) {
            super(cursor);
            mItemStartPosition = getItemStartPositionsInCursor(cursor);
            cursor.moveToFirst();
        }

        @Override
        public int getCount() {
            return mItemStartPosition.size();
        }

        @Override
        public boolean moveToPosition(int position) {
            int n_th_item_start_position = mItemStartPosition.get(position);
            return super.moveToPosition(n_th_item_start_position);
        }
    }

    /**
     * When the cursor is the result of JOINED query, there can be multiple rows with the same item.
     *      (e.g: when fetching items with branch_items, there will be a row with
     *      the same item id for each branch the item exists in).
     * This function returns the starting positions for each item. The start position if the
     * boundary where the last item finishes and a new item starts.
     * @return      a mapping from the n-th item to the starting position in the cursor.
     */
    private static Map<Integer, Integer> getItemStartPositionsInCursor(Cursor cursor) {
        Map<Integer, Integer> starting_positions = new HashMap<>();

        if (!cursor.moveToFirst())
            return starting_positions;

        int cursor_index = 0;
        long prev_item_id = -1;

        do {
            if (cursor.isNull(COL_ITEM_ID)) break;

            long item_id = cursor.getLong(COL_ITEM_ID);
            if (item_id != prev_item_id) {
                /**
                 * It is a mapping from the n-th item to the position inside the cursor.
                 * So the n-th item is the "n-th-unique" item we've seen so far.
                 */
                starting_positions.put(starting_positions.size(), cursor_index);

                prev_item_id = item_id;
            }
            cursor_index++;
        } while (cursor.moveToNext());

        cursor.moveToFirst();

        return starting_positions;
    }

    private SItem(Parcel parcel) {
        company_id = parcel.readLong();
        item_id = parcel.readLong();
        name = parcel.readString();
        item_code = parcel.readString();
        category = parcel.readLong();
        unit_of_measurement = parcel.readInt();
        has_derived_unit = SheketContract.toBool(parcel.readInt());
        derived_name = parcel.readString();
        derived_factor = parcel.readDouble();
        reorder_level = parcel.readDouble();
        model_year = parcel.readString();
        part_number = parcel.readString();
        bar_code = parcel.readString();
        has_bar_code = SheketContract.toBool(parcel.readInt());
        change_status = parcel.readInt();
        client_uuid = parcel.readString();
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_COMPANY_ID, company_id);
        values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
        values.put(ItemEntry.COLUMN_NAME, name);
        values.put(ItemEntry.COLUMN_ITEM_CODE, item_code);
        values.put(ItemEntry.COLUMN_CATEGORY_ID, category);

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
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        values.put(COLUMN_UUID, client_uuid);
        return values;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(JSON_ITEM_ID, item_id);
        result.put(JSON_ITEM_NAME, name);
        result.put(JSON_ITEM_CODE, item_code);
        result.put(JSON_ITEM_CATEGORY, category);
        result.put(JSON_UNIT_OF_MEASUREMENT, unit_of_measurement);
        result.put(JSON_HAS_DERIVED_UNIT, has_derived_unit);
        result.put(JSON_DERIVED_NAME, derived_name);
        result.put(JSON_DERIVED_FACTOR, derived_factor);
        result.put(JSON_REORDER_LEVEL, reorder_level);
        result.put(JSON_MODEL_YEAR, model_year);
        result.put(JSON_PART_NUMBER, part_number);
        result.put(JSON_BAR_CODE, bar_code);
        result.put(JSON_HAS_BAR_CODE, has_bar_code);
        result.put(JSON_ITEM_UUID, client_uuid);
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(item_id);
        dest.writeString(name);
        dest.writeString(item_code);
        dest.writeLong(category);
        dest.writeInt(unit_of_measurement);
        dest.writeInt(SheketContract.toInt(has_derived_unit));
        dest.writeString(derived_name);
        dest.writeDouble(derived_factor);
        dest.writeDouble(reorder_level);
        dest.writeString(model_year);
        dest.writeString(part_number);
        dest.writeString(bar_code);
        dest.writeInt(SheketContract.toInt(has_bar_code));
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

