package com.mukera.sheket.client.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 3/28/16.
 */
public class SyncUtil {
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

    public static long getCurrentCompanyId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long invalid_id = context.getResources().getInteger(R.integer.invalid_company_id);
        return prefs.getLong(context.getString(R.string.pref_company_id), invalid_id);
    }

    public static void setCurrentCompanyId(Context context, long companyId) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.pref_company_id), companyId);
        editor.commit();
    }

}
