package com.mukera.sheket.client.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 3/28/16.
 */
public class SyncUtil {
    public static void setLoginCookie(Context context, String cookie) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.pref_login_cookie), cookie);
        editor.commit();
    }

    public static String getLoginCookie(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_login_cookie), "");
    }

    public static int getItemRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.default_revision);
        return prefs.getInt(context.getString(R.string.pref_item_revision), default_rev);
    }

    public static int getBranchRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.default_revision);
        return prefs.getInt(context.getString(R.string.pref_branch_revision), default_rev);
    }

    public static int getBranchItemRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.default_revision);
        return prefs.getInt(context.getString(R.string.pref_branch_item_revision), default_rev);
    }

    public static int getTransactionRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.default_revision);
        return prefs.getInt(context.getString(R.string.pref_transaction_revision), default_rev);
    }

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
}
