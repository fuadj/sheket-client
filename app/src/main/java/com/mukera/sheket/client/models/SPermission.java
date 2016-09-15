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
    public interface PermissionChangeListener {
        void userPermissionChanged();
    }

    private static SPermission sPermission;

    public static final int PERMISSION_TYPE_NONE = 0;
    public static final int PERMISSION_TYPE_ALL_ACCESS = 1;
    public static final int PERMISSION_TYPE_ALL_BRANCHES = 2;
    public static final int PERMISSION_TYPE_LISTED_BRANCHES = 3;

    private int mPermissionType;
    private List<Long> mAllowedBranches;
    private List<Long> mAllowedStoreBranches;

    static final String P_JSON_TYPE = "permission_type";
    static final String P_JSON_BRANCHES = "branches";
    static final String P_JSON_STORE_BRANCHES = "store_branches";

    public SPermission() {
        mPermissionType = PERMISSION_TYPE_NONE;
        mAllowedBranches = new ArrayList<>();
        mAllowedStoreBranches = new ArrayList<>();
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
                    permission.mAllowedBranches.add(arr.getLong(i));
                }
            }
            if (json.has(P_JSON_STORE_BRANCHES)) {
                JSONArray arr = json.getJSONArray(P_JSON_STORE_BRANCHES);
                for (int i = 0; i < arr.length(); i++) {
                    permission.mAllowedStoreBranches.add(arr.getLong(i));
                }
            }
        } catch (JSONException e) {
            permission.mPermissionType = PERMISSION_TYPE_NONE;
            return permission;
        }
        return permission;
    }

    public static String shortName(String permission_text) {
        return shortName(Decode(permission_text));
    }

    public static String shortName(SPermission permission) {
        if (permission == null) {
            return "Undefined";
        }
        switch (permission.mPermissionType) {
            default:
            case PERMISSION_TYPE_NONE:
                return "NONE";
            case PERMISSION_TYPE_ALL_ACCESS:
                return "All Access";
            case PERMISSION_TYPE_ALL_BRANCHES:
                return "All Branches";
            case PERMISSION_TYPE_LISTED_BRANCHES:
                return "Limited";
        }
    }

    public String Encode() {
        String result = null;

        try {
            JSONObject json = new JSONObject();
            json.put(P_JSON_TYPE, mPermissionType);
            if (!mAllowedBranches.isEmpty()) {
                json.put(P_JSON_BRANCHES,
                        new JSONArray(mAllowedBranches));
            }
            if (!mAllowedStoreBranches.isEmpty()) {
                json.put(P_JSON_STORE_BRANCHES,
                        new JSONArray(mAllowedStoreBranches));
            }

            result = json.toString();
        } catch (JSONException e) {
            result = null;
        }
        return result;
    }

    public boolean hasManagerAccess() {
        return getPermissionType() == PERMISSION_TYPE_ALL_ACCESS;
    }

    public boolean hasEmploeeAccess() {
        return getPermissionType() != PERMISSION_TYPE_NONE;
    }

    public int getPermissionType() {
        return mPermissionType;
    }

    public void setPermissionType(int type) {
        mPermissionType = type;
    }

    public void setAllowedBranches(List<Long> branches) {
        mAllowedBranches = new ArrayList<>(branches);
    }

    public void setAllowedStoreBranches(List<Long> stores) {
        mAllowedStoreBranches = new ArrayList<>(stores);
    }

    public List<Long> getAllowedBranches() {
        return mAllowedBranches;
    }

    public List<Long> getAllowedStoreBranches() {
        return mAllowedStoreBranches;
    }
}
