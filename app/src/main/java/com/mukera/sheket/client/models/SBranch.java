package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.network.Branch;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gamma on 3/27/16.
 */
public class SBranch extends UUIDSyncable implements Parcelable {

    static String _f(String s) {
        return BranchEntry._full(s);
    }

    public static final String[] BRANCH_COLUMNS = {
            _f(BranchEntry.COLUMN_COMPANY_ID),
            _f(BranchEntry.COLUMN_BRANCH_ID),
            _f(BranchEntry.COLUMN_NAME),
            _f(BranchEntry.COLUMN_LOCATION),
            _f(COLUMN_CHANGE_INDICATOR),
            _f(COLUMN_UUID),
            _f(BranchEntry.COLUMN_STATUS_FLAG)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_BRANCH_ID = 1;
    public static final int COL_NAME = 2;
    public static final int COL_LOCATION = 3;
    public static final int COL_CHANGE = 4;
    public static final int COL_CLIENT_UUID = 5;
    public static final int COL_STATUS_FLAG = 6;

    public static final int COL_LAST = 7;

    public int company_id;
    public int branch_id;
    public String branch_name;
    public String branch_location;
    public int status_flag;

    // See docs for {@link SItem.NO_ITEM_FOUND}
    public static final int NO_BRANCH_FOUND = 0;

    public SBranch() {
    }

    public SBranch(Branch gRPC_Branch) {
        branch_id = gRPC_Branch.getBranchId();
        branch_name = gRPC_Branch.getName();
        client_uuid = gRPC_Branch.getUUID();
        status_flag = gRPC_Branch.getStatusFlag();
    }

    public SBranch(Cursor cursor) {
        this(cursor, 0);
    }

    public SBranch(Cursor cursor, int offset) {
        if (cursor.isNull(COL_BRANCH_ID + offset)) {
            branch_id = NO_BRANCH_FOUND;
            return;
        }
        branch_id = cursor.getInt(COL_BRANCH_ID + offset);
        company_id = cursor.getInt(COL_COMPANY_ID + offset);
        branch_name = cursor.getString(COL_NAME + offset);
        branch_location = cursor.getString(COL_LOCATION + offset);
        change_status = cursor.getInt(COL_CHANGE + offset);
        client_uuid = cursor.getString(COL_CLIENT_UUID + offset);
        status_flag = cursor.getInt(COL_STATUS_FLAG + offset);
    }

    private SBranch(Parcel parcel) {
        company_id = parcel.readInt();
        branch_id = parcel.readInt();
        branch_name = parcel.readString();
        branch_location = parcel.readString();

        change_status = parcel.readInt();
        client_uuid = parcel.readString();

        status_flag = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(company_id);
        dest.writeLong(branch_id);
        dest.writeString(branch_name);
        dest.writeString(branch_location);

        dest.writeInt(change_status);
        dest.writeString(client_uuid);

        dest.writeInt(status_flag);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(BranchEntry.COLUMN_COMPANY_ID, company_id);
        values.put(BranchEntry.COLUMN_BRANCH_ID, branch_id);
        values.put(BranchEntry.COLUMN_NAME, branch_name);
        values.put(BranchEntry.COLUMN_LOCATION, branch_location);
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        values.put(BranchEntry.COLUMN_STATUS_FLAG, status_flag);
        values.put(COLUMN_UUID, client_uuid);
        return values;
    }

    public Branch.Builder toGRPCBuilder() {
        return Branch.newBuilder().
                setBranchId((int) branch_id).
                setName(branch_name).
                setUUID(client_uuid).
                setStatusFlag(status_flag);
    }

    public static final Parcelable.Creator<SBranch> CREATOR = new Parcelable.Creator<SBranch>() {
        @Override
        public SBranch createFromParcel(Parcel source) {
            return new SBranch(source);
        }

        @Override
        public SBranch[] newArray(int size) {
            return new SBranch[size];
        }
    };
}
