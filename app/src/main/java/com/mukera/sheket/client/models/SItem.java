package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Pair;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.network.Item;

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
            _f(COLUMN_UUID),
            _f(ItemEntry.COLUMN_STATUS_FLAG)
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

    public static final int COL_STATUS_FLAG = 16;

    public static final int COL_LAST = 17;

    public int company_id;
    public int item_id;
    public String name;
    public String item_code;
    public int category;

    public int unit_of_measurement;
    public boolean has_derived_unit;
    public String derived_name;
    public double derived_factor;
    public double reorder_level;

    public String model_year;
    public String part_number;
    public String bar_code;
    public boolean has_bar_code;
    public int status_flag;

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
    /**
     * The list contains pair of {branch_item and branch}. The branch_item can be null if the
     * item doesn't exist inside the branch.
     */
    public List<Pair<SBranchItem, SBranch>> available_branches;

    public SItem() {
    }

    public SItem(Item gRPC_Item) {
        item_id = gRPC_Item.getItemId();
        name = gRPC_Item.getName();
        item_code = gRPC_Item.getCode();
        category = gRPC_Item.getCategoryId();

        unit_of_measurement = gRPC_Item.getUnitOfMeasurement();
        has_derived_unit = gRPC_Item.getHasDerivedUnit();
        derived_name = gRPC_Item.getDerivedName();
        derived_factor = gRPC_Item.getDerivedFactor();
        client_uuid = gRPC_Item.getUUID();
        status_flag = gRPC_Item.getStatusFlag();
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

        item_id = cursor.getInt(COL_ITEM_ID + offset);
        company_id = cursor.getInt(COL_COMPANY_ID + offset);
        name = cursor.getString(COL_NAME + offset);
        item_code = cursor.getString(COL_MANUAL_CODE + offset);
        category = cursor.getInt(COL_CATEGORY + offset);

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
        status_flag = cursor.getInt(COL_STATUS_FLAG + offset);

        total_quantity = 0.d;
        available_branches = new ArrayList<>();

        if (fetch_branch_items) {
            while (true) {
                SBranchItem branchItem = new SBranchItem(cursor, COL_LAST + offset, false);
                if (branchItem.branch_id == SBranchItem.NO_BRANCH_ITEM_FOUND) {
                    /**
                     * we are assigning null and not breaking because we want to fetch
                     * all the branches for an item, even if it doesn't exist in some of them.
                     * NOTE: we check if the branch is also not available, in which case we
                     * ignore this row. Otherwise, we fetch the branch even though the item
                     * doesn't exist in it. This is useful in transactions when a user tries
                     * to receive an item from a branch which doesn't contain the item. We can
                     * inform the user that it is a non-existent item we are trying to move.
                     */
                    branchItem = null;
                }
                SBranch branch = new SBranch(cursor, COL_LAST + SBranchItem.COL_LAST);
                if (branch.branch_id == SBranch.NO_BRANCH_FOUND) {
                    break;
                }
                available_branches.add(new Pair<>(branchItem, branch));
                total_quantity += (branchItem != null) ? branchItem.quantity : 0.d;

                if (!cursor.moveToNext()) // we've hit the end
                    break;
                int next_item_id = cursor.getInt(COL_ITEM_ID + offset);
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
         * This contains the start positions of items in the cursor. It is then used to determine
         * how many "unique" items there are, AND to move to the start index for each unique item.
         */
        private List<Integer> mItemStartPositions;

        public ItemWithAvailableBranchesCursor(Cursor cursor) {
            super(cursor);
            mItemStartPositions = getItemStartPositionsInCursor(cursor);
            cursor.moveToFirst();
        }

        @Override
        public int getCount() {
            return mItemStartPositions.size();
        }

        @Override
        public boolean moveToPosition(int position) {
            return super.moveToPosition(mItemStartPositions.get(position));
        }
    }

    /**
     * When the cursor is the result of JOINED query, there can be multiple rows with the same item.
     *      (e.g: when fetching items with branch_items, there will be a row with
     *      the same item id for each branch the item exists in).
     * This function returns the starting positions for each item. The start position is the
     * boundary where the last item finishes and a new item starts.
     * @return      a mapping from the n-th item to the starting position in the cursor.
     *              each element of the list contains the starting position of the item at that
     *              index.
     *              (e.g:    if it returns [0, 3, 5] then the items start at indices (0, 3, 5))
     */
    private static List<Integer> getItemStartPositionsInCursor(Cursor cursor) {
        List<Integer> starting_positions = new ArrayList<>();

        if (!cursor.moveToFirst())
            return starting_positions;

        int prev_item_id = -1;

        for (int i = 0; ; i++) {
            if (cursor.isNull(COL_ITEM_ID)) break;

            int item_id = cursor.getInt(COL_ITEM_ID);
            if (item_id != prev_item_id) {
                starting_positions.add(i);

                prev_item_id = item_id;
            }
            if (!cursor.moveToNext())
                break;
        }

        cursor.moveToFirst();

        return starting_positions;
    }

    private SItem(Parcel parcel) {
        company_id = parcel.readInt();
        item_id = parcel.readInt();
        name = parcel.readString();
        item_code = parcel.readString();
        category = parcel.readInt();
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
        status_flag = parcel.readInt();
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
        values.put(ItemEntry.COLUMN_STATUS_FLAG, status_flag);
        return values;
    }

    public Item.Builder toGRPCBuilder() {
        return Item.newBuilder().
                setItemId((int)item_id).
                setName(name).
                setCode(item_code).
                setCategoryId((int)category).
                setUnitOfMeasurement(unit_of_measurement).
                setHasDerivedUnit(has_derived_unit).
                setDerivedName(derived_name).
                setDerivedFactor(derived_factor).
                setUUID(client_uuid).setStatusFlag(status_flag);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(company_id);
        dest.writeInt(item_id);
        dest.writeString(name);
        dest.writeString(item_code);
        dest.writeInt(category);
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
        dest.writeInt(status_flag);
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

