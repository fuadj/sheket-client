package com.mukera.sheket.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 3/28/16.
 */
public class PrefUtil {
    public static void setLoginCookie(Context context, String cookie) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_login_cookie), cookie);
        editor.commit();
    }

    public static String getLoginCookie(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_login_cookie), "");
    }

    /**
     * Revision Tracking Block
     */
    public static int getCategoryRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_category_revision), default_rev);
    }

    public static void setCategoryRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_category_revision), revision);
        editor.commit();
    }

    public static int getItemRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_item_revision), default_rev);
    }

    public static void setItemRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_item_revision), revision);
        editor.commit();
    }

    public static int getBranchRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_branch_revision), default_rev);
    }

    public static void setBranchRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_branch_revision), revision);
        editor.commit();
    }

    public static int getBranchItemRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_branch_item_revision), default_rev);
    }

    public static void setBranchItemRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_branch_item_revision), revision);
        editor.commit();
    }

    public static int getTransactionRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_transaction_revision), default_rev);
    }

    public static void setTransactionRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_transaction_revision), revision);
        editor.commit();
    }

    public static int getMemberRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_member_revision), default_rev);
    }

    public static void setMemberRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_member_revision), revision);
        editor.commit();
    }

    public static void setUserRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_user_rev), revision);
        editor.commit();
    }

    public static long getUserRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_user_rev), default_rev);
    }
    /**
     * End-Revision Tracking Block
     */

    /**
     * User setting Tracking Block
     */
    public static void setCurrentCompanyId(Context context, long companyId) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.pref_company_id), companyId);
        editor.commit();
    }

    public static long getCurrentCompanyId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long invalid_id = context.getResources().getInteger(R.integer.invalid_company_id);
        return prefs.getLong(context.getString(R.string.pref_company_id), invalid_id);
    }

    public static boolean isCompanySet(Context context) {
        return getCurrentCompanyId(context) !=
                context.getResources().getInteger(R.integer.invalid_company_id);
    }

    public static void setCurrentCompanyName(Context context, String name) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_company_name), name);
        editor.commit();
    }

    public static String getCurrentCompanyName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_company_name),
                context.getString(R.string.pref_company_name_default));
    }

    public static void setUserName(Context context, String name) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_user_name), name);
        editor.commit();
    }

    public static String getUsername(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        return prefs.getString(context.getString(R.string.pref_user_name), "");
    }

    public static void setUserId(Context context, long user_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.pref_user_id), user_id);
        editor.commit();
    }

    public static long getUserId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long invalid_id = context.getResources().getInteger(R.integer.invalid_user_id);
        return prefs.getLong(context.getString(R.string.pref_user_id), invalid_id);
    }

    public static boolean isUserSet(Context context) {
        return getUserId(context) !=
                context.getResources().getInteger(R.integer.invalid_user_id);
    }

    public static void logoutUser(Context context) {
        setUserId(context, context.getResources().getInteger(R.integer.invalid_user_id));
        setCurrentCompanyId(context, context.getResources().getInteger(R.integer.invalid_company_id));

        setUserName(context, "");
        setCurrentCompanyName(context, "");

        setUserPermission(context, "");
        setLoginCookie(context, "");
    }

    public static void setUserPermission(Context context, String permission) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_user_permission), permission);
        editor.commit();
    }

    public static String getUserPermission(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_user_permission), "");
    }
    /**
     * End-User setting Tracking Block
     */

    /**
     * Storing locally generate temporary ids.
     * All these ids are -ve, this is b/c we will be replacing
     * them with a globally unique id after syncing with the server.
     * So, to avoid any collisions with the local and server ids, all
     * locally generated ids are negative. We add -1
     * to get a new id for each entity.
     */
    public static long getNewBranchId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_branch_id), default_id) - 1;
    }

    public static void setNewBranchId(Context context, long branch_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_branch_id), branch_id);
        editor.commit();
    }

    public static long getNewItemId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_item_id), default_id) - 1;
    }

    public static void setNewItemId(Context context, long item_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_item_id), item_id);
        editor.commit();
    }

    public static void setNewTransId(Context context, long trans_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_trans_id), trans_id);
        editor.commit();
    }

    public static long getNewTransId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_trans_id), default_id) - 1;
    }

    public static long getNewCategoryId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_category_id), default_id) - 1;
    }

    public static void setNewCategoryId(Context context, long category_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_category_id), category_id);
        editor.commit();
    }

    public static void setIpAddress(Context context, String address) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_ip_key), address);
        editor.commit();
    }
}
