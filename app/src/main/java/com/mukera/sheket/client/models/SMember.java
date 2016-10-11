package com.mukera.sheket.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.network.Employee;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gamma on 4/7/16.
 */
public class SMember extends ChangeTraceable {
    static String _f(String s) { return MemberEntry._full(s); }

    public static final String[] MEMBER_COLUMNS = {
            _f(MemberEntry.COLUMN_COMPANY_ID),
            _f(MemberEntry.COLUMN_MEMBER_ID),
            _f(MemberEntry.COLUMN_MEMBER_NAME),
            _f(MemberEntry.COLUMN_MEMBER_PERMISSION),
            _f(COLUMN_CHANGE_INDICATOR)
    };

    public static final int COL_COMPANY_ID = 0;
    public static final int COL_MEMBER_ID = 1;
    public static final int COL_MEMBER_NAME = 2;
    public static final int COL_MEMBER_PERMISSION = 3;
    public static final int COL_CHANGE = 4;

    public static final int COL_LAST = 5;

    public int company_id;
    public int member_id;
    public String member_name;
    public SPermission member_permission;

    public SMember() {
    }

    public SMember(Employee gRPC_Employee) {
        member_id = gRPC_Employee.getEmployeeId();
        member_name = gRPC_Employee.getName();
        member_permission = SPermission.Decode(
                gRPC_Employee.getPermission());
    }

    public SMember(SMember other) {
        company_id = other.company_id;
        member_id = other.member_id;
        member_name = other.member_name;
        member_permission = other.member_permission;
        change_status = other.change_status;
    }

    public SMember(Cursor cursor) {
        this(cursor, 0);
    }

    public SMember(Cursor cursor, int offset) {
        company_id = cursor.getInt(COL_COMPANY_ID + offset);
        member_id = cursor.getInt(COL_MEMBER_ID + offset);
        member_name = cursor.getString(COL_MEMBER_NAME + offset);
        member_permission = SPermission.Decode(cursor.
                getString(COL_MEMBER_PERMISSION + offset));
        change_status = cursor.getInt(COL_CHANGE + offset);
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(MemberEntry.COLUMN_COMPANY_ID, company_id);
        values.put(MemberEntry.COLUMN_MEMBER_ID, member_id);
        values.put(MemberEntry.COLUMN_MEMBER_NAME, member_name);
        values.put(MemberEntry.COLUMN_MEMBER_PERMISSION,
                member_permission.Encode());
        values.put(COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    public Employee.Builder toGRPCBuilder() {
        return Employee.newBuilder().
                setEmployeeId(member_id).
                setName(member_name).
                setPermission(member_permission.Encode());
    }
}
