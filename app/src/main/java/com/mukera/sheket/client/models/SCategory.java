package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Parcel;
import android.os.Parcelable;

import com.mukera.sheket.client.network.Category;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.data.SheketContract.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fuad on 5/21/16.
 */
public class SCategory extends UUIDSyncable implements Parcelable {

    public static final String JSON_CATEGORY_ID = "category_id";
    public static final String JSON_CATEGORY_UUID = "client_uuid";
    public static final String JSON_NAME = "name";
    public static final String JSON_PARENT_ID = "parent_id";

    static String _fCurrent(String s) {
        return CategoryEntry._fullCurrent(s);
    }

    static String _fChild(String s) {
        return CategoryEntry._fullChild(s);
    }

    /**
     * You can use this projection with results from joined queries with Category tables,
     * provided that you ALIASED the category table with {@code CategoryEntry.PART_CURRENT} qualifier.
     * NOTE: if you want to fetch the children categories also, you need to create a joined
     * query to fetch children and alias that bit with {@code CategoryEntry.PART_CHILD}. Then
     * you can use {@code SCategory.CATEGORY_WITH_CHILDREN_COLUMNS} with it.
     */
    public static final String[] CATEGORY_COLUMNS = {
            // Current category columns
            _fCurrent(CategoryEntry.COLUMN_COMPANY_ID),
            _fCurrent(CategoryEntry.COLUMN_CATEGORY_ID),
            _fCurrent(CategoryEntry.COLUMN_NAME),
            _fCurrent(CategoryEntry.COLUMN_PARENT_ID),
            _fCurrent(COLUMN_CHANGE_INDICATOR),
            _fCurrent(COLUMN_UUID),
    };

    private static final String[] CHILDREN_COLUMNS = {
            // Child category columns
            _fChild(CategoryEntry.COLUMN_COMPANY_ID),
            _fChild(CategoryEntry.COLUMN_CATEGORY_ID),
            _fChild(CategoryEntry.COLUMN_NAME),
            _fChild(CategoryEntry.COLUMN_PARENT_ID),
            _fChild(COLUMN_CHANGE_INDICATOR),
            _fChild(COLUMN_UUID),
    };

    /**
     * Use this if you also want to fetch the children.
     */
    public static final String[] CATEGORY_WITH_CHILDREN_COLUMNS;

    static {
        CATEGORY_WITH_CHILDREN_COLUMNS = new String[CATEGORY_COLUMNS.length + CHILDREN_COLUMNS.length];

        System.arraycopy(CATEGORY_COLUMNS, 0, CATEGORY_WITH_CHILDREN_COLUMNS, 0, CATEGORY_COLUMNS.length);
        System.arraycopy(CHILDREN_COLUMNS, 0, CATEGORY_WITH_CHILDREN_COLUMNS,
                CATEGORY_COLUMNS.length, CHILDREN_COLUMNS.length);
    }

    public static final int COL_CURRENT_COMPANY_ID = 0;
    public static final int COL_CURRENT_CATEGORY_ID = 1;
    public static final int COL_CURRENT_NAME = 2;
    public static final int COL_CURRENT_PARENT_ID = 3;
    public static final int COL_CURRENT_CHANGE_INDICATOR = 4;
    public static final int COL_CURRENT_CLIENT_UUID = 5;

    public static final int COL_CHILD_COMPANY_ID = 6;
    public static final int COL_CHILD_CATEGORY_ID = 7;
    public static final int COL_CHILD_NAME = 8;
    public static final int COL_CHILD_PARENT_ID = 9;
    public static final int COL_CHILD_CHANGE_INDICATOR = 10;
    public static final int COL_CHILD_CLIENT_UUID = 11;

    public static final int COL_LAST = 12;

    public long company_id;
    public long category_id;
    public String name;
    public long parent_id;

    public List<SCategory> childrenCategories;

    public SCategory() {
    }

    // constructor from gRPC object
    public SCategory(Category gRPC_Category) {
        category_id = gRPC_Category.getCategoryId();
        name = gRPC_Category.getName();
        parent_id = gRPC_Category.getParentId();
        client_uuid = gRPC_Category.getUUID();
    }

    public SCategory(Cursor cursor) {
        this(cursor, 0, true, false);
    }

    public SCategory(Cursor cursor, int offset) {
        this(cursor, offset, true, false);
    }

    public SCategory(Cursor cursor, boolean fetch_children) {
        this(cursor, 0, true, fetch_children);
    }

    private static final int NO_CATEGORY_FOUND = 0;

    public SCategory(Cursor cursor, int offset, boolean is_parent, boolean fetch_children) {
        if (is_parent) {
            if (cursor.isNull(COL_CURRENT_COMPANY_ID + offset)) {
                category_id = NO_CATEGORY_FOUND;
                return;
            }
            company_id = cursor.getLong(COL_CURRENT_COMPANY_ID + offset);
            category_id = cursor.getLong(COL_CURRENT_CATEGORY_ID + offset);
            name = Utils.toTitleCase(cursor.getString(COL_CURRENT_NAME + offset));
            parent_id = cursor.getLong(COL_CURRENT_PARENT_ID + offset);

            change_status = cursor.getInt(COL_CURRENT_CHANGE_INDICATOR + offset);
            client_uuid = cursor.getString(COL_CURRENT_CLIENT_UUID + offset);

            childrenCategories = new ArrayList<>();
            if (fetch_children) {
                while (true) {
                    SCategory child = new SCategory(cursor, offset, false, false);
                    if (child.category_id == NO_CATEGORY_FOUND)
                        break;

                    childrenCategories.add(child);
                    if (!cursor.moveToNext())       // we've reached the end
                        break;
                    long next_category = cursor.getLong(COL_CURRENT_CATEGORY_ID + offset);
                    if (next_category != category_id) {     // we've moved to the territory of the next category, get back
                        cursor.moveToPrevious();
                        break;
                    }
                }
            }
        } else {
            if (cursor.isNull(COL_CHILD_CATEGORY_ID + offset)) {
                category_id = NO_CATEGORY_FOUND;
                return;
            }

            company_id = cursor.getLong(COL_CHILD_COMPANY_ID + offset);
            category_id = cursor.getLong(COL_CHILD_CATEGORY_ID + offset);
            name = Utils.toTitleCase(cursor.getString(COL_CHILD_NAME + offset));
            parent_id = cursor.getLong(COL_CHILD_PARENT_ID + offset);

            change_status = cursor.getInt(COL_CHILD_CHANGE_INDICATOR + offset);
            client_uuid = cursor.getString(COL_CHILD_CLIENT_UUID + offset);

            childrenCategories = null;
        }
    }

    /**
     * See docs for {@code SItem.ItemWithAvailableBranchesCursor}
     */
    public static class CategoryWithChildrenCursor extends CursorWrapper {
        private List<Integer> mCategoryStartPositions;

        public CategoryWithChildrenCursor(Cursor cursor) {
            super(cursor);
            mCategoryStartPositions = getCategoryStartPositionsInCursor(cursor);
        }

        @Override
        public int getCount() {
            return mCategoryStartPositions.size();
        }

        @Override
        public boolean moveToPosition(int position) {
            return super.moveToPosition(mCategoryStartPositions.get(position));
        }
    }

    private static List<Integer> getCategoryStartPositionsInCursor(Cursor cursor) {
        List<Integer> starting_positions = new ArrayList<>();

        if (!cursor.moveToFirst()) return starting_positions;

        long prev_category_id = -1;
        for (int i = 0; ; i++) {
            if (cursor.isNull(COL_CURRENT_CATEGORY_ID)) break;

            long category_id = cursor.getLong(COL_CURRENT_CATEGORY_ID);
            if (category_id != prev_category_id) {
                starting_positions.add(i);
                prev_category_id = category_id;
            }
            if (!cursor.moveToNext())
                break;
        }

        cursor.moveToFirst();
        return starting_positions;
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

    public Category.Builder toGRPCBuilder() {
        return Category.newBuilder().
                setCategoryId((int) category_id).
                setName(name).
                setParentId((int) parent_id).
                setUUID(client_uuid);
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
