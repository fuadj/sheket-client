package com.mukera.sheket.client.models;

import android.content.Context;

import com.mukera.sheket.client.utils.PrefUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 4/7/16.
 */
public class SPermission {
    public static final int PERMISSION_TYPE_NONE = 0;
    public static final int PERMISSION_TYPE_OWNER = 1;
    public static final int PERMISSION_TYPE_GENERAL_MANAGER = 2;
    public static final int PERMISSION_TYPE_BRANCH_MANAGER = 3;
    public static final int PERMISSION_TYPE_EMPLOYEE = 4;

    public static final int BRANCH_ACCESS_SEE_QTY = 1;
    public static final int BRANCH_ACCESS_BUY_ITEM = 2;
    public static final int BRANCH_ACCESS_SEE_QTY_AND_BUY_ITEM = 3;

    public static class BranchAccess {
        public int branch_id;
        public int access;

        public boolean show_qty;
        public boolean buy_items;

        public BranchAccess(int branch_id) {
            this.branch_id = branch_id;
        }

        public BranchAccess(int branch_id, int access) {
            this.branch_id = branch_id;
            this.access = access;
        }

        public void encodeAccess() {
            if (show_qty && buy_items) {
                access = BRANCH_ACCESS_SEE_QTY_AND_BUY_ITEM;
            } else if (show_qty) {
                access = BRANCH_ACCESS_SEE_QTY;
            } else if (buy_items) {
                access = BRANCH_ACCESS_BUY_ITEM;
            }
        }
    }

    public int mPermissionType;
    public List<BranchAccess> mAllowedBranches;

    static final String P_JSON_TYPE = "permission_type";
    static final String P_JSON_BRANCHES = "branches";

    static final String P_JSON_BRANCH_ID = "branch_id";
    static final String P_JSON_ACCESS = "access";

    public SPermission() {
        mPermissionType = PERMISSION_TYPE_NONE;
        mAllowedBranches = new ArrayList<>();
    }

    @Override
    public String toString() {
        switch (mPermissionType) {
            case PERMISSION_TYPE_GENERAL_MANAGER: return "Manager";
            case PERMISSION_TYPE_EMPLOYEE: return "Employee";
            default: return "Unknown";
        }
    }

    /**
     * Decodes the user permission and converts this to an {@code SPermission} object.
     * It is safe to use even if permission hasn't been set(e.g: if there is no company at all).
     * It will return an {@code SPermission} with a PERMISSION_TYPE_NONE as its type.
     */
    public static SPermission getUserPermission(Context context) {
        return Decode(PrefUtil.getEncodedUserPermission(context));
    }

    public static SPermission Decode(String text) {
        SPermission permission = new SPermission();
        try {
            JSONObject json = new JSONObject(text);

            permission.mPermissionType = json.getInt(P_JSON_TYPE);
            if (json.has(P_JSON_BRANCHES)) {
                JSONArray arr = json.getJSONArray(P_JSON_BRANCHES);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    int branch_id = obj.getInt(P_JSON_BRANCH_ID);
                    int access = obj.getInt(P_JSON_ACCESS);

                    permission.mAllowedBranches.add(
                            new BranchAccess(branch_id, access)
                    );
                }
            }
        } catch (JSONException e) {
            permission.mPermissionType = PERMISSION_TYPE_NONE;
            return permission;
        }
        return permission;
    }

    public String Encode() {
        String result;
        try {
            JSONObject json = new JSONObject();
            json.put(P_JSON_TYPE, mPermissionType);
            if (!mAllowedBranches.isEmpty()) {

                JSONArray branchAuthorities = new JSONArray();
                for (BranchAccess access : mAllowedBranches) {
                    access.encodeAccess();

                    branchAuthorities.put(
                            new JSONObject().
                                    put(P_JSON_BRANCH_ID, access.branch_id).
                                    put(P_JSON_ACCESS, access.access)
                    );
                }
                json.put(P_JSON_BRANCHES, branchAuthorities);
            }
            result = json.toString();
        } catch (JSONException e) {
            result = null;
        }
        return result;
    }

    public boolean hasManagerAccess() {
        switch (getPermissionType()) {
            case PERMISSION_TYPE_OWNER:
            case PERMISSION_TYPE_GENERAL_MANAGER:
                return true;
            default:
                return false;
        }
    }

    public int getPermissionType() {
        return mPermissionType;
    }

    public SPermission setPermissionType(int type) {
        mPermissionType = type;
        return this;
    }

    public SPermission setAllowedBranches(List<BranchAccess> branches) {
        mAllowedBranches = new ArrayList<>(branches);
        return this;
    }

    public List<BranchAccess> getAllowedBranches() {
        return mAllowedBranches;
    }
}
