package com.mukera.sheket.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.user.UserUtil;

import java.util.Vector;

/**
 * Created by gamma on 3/28/16.
 */
public class PrefUtil {
    /**
     * Sync On Login: Check this if you are just loggin in and should sync
     */
    private static final String pref_should_sync_on_login = "pref_should_sync_on_login";
    public static void setShouldSyncOnLogin(Context context, boolean should_sync) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(pref_should_sync_on_login, should_sync);
        editor.commit();
    }

    public static boolean getShouldSyncOnLogin(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(pref_should_sync_on_login, false);
    }

    private static final String pref_is_first_time = "pref_is_first_time";
    public static void setIsFirstTime(Context context, boolean is_first_time) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(pref_is_first_time, is_first_time);
        editor.commit();
    }

    public static boolean getIsFirstTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(pref_is_first_time, true);
    }

    public static final int LANGUAGE_ENGLISH = 1;
    public static final int LANGUAGE_AMHARIC = 2;
    private static final String pref_user_language = "pref_user_language";
    public static void setUserLanguage(Context context, int language_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(pref_user_language, language_id);
        editor.commit();
    }

    public static int getUserLanguageId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(pref_user_language, LANGUAGE_ENGLISH);
    }

    public static String getUserLanguageLocale(Context context) {
        int lang_id = getUserLanguageId(context);
        String language_code = "en";
        switch (lang_id) {
            case LANGUAGE_ENGLISH: language_code = "en"; break;
            case LANGUAGE_AMHARIC: language_code = "am"; break;
        }
        return language_code;
    }

    private static final String pref_is_sync_running = "pref_is_sync_running";
    public static boolean isSyncRunning(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(pref_is_sync_running, false);
    }

    public static void setIsSyncRunning(Context context, boolean is_running) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(pref_is_sync_running, is_running);
        editor.commit();
    }
    /**
     */

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

    public static int getBranchCategoryRevision(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        return prefs.getInt(context.getString(R.string.pref_branch_category_revision), default_rev);
    }

    public static void setBranchCategoryRevision(Context context, int revision) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(context.getString(R.string.pref_branch_category_revision), revision);
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

    public static void resetAllRevisionNumbers(Context context) {
        int default_rev = context.getResources().getInteger(R.integer.pref_default_revision);
        setBranchRevision(context, default_rev);
        setItemRevision(context, default_rev);
        setBranchItemRevision(context, default_rev);
        setTransactionRevision(context, default_rev);
        setCategoryRevision(context, default_rev);
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
        setEncodedDelimitedUserId(context, user_id);
    }

    public static long getUserId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long invalid_id = context.getResources().getInteger(R.integer.invalid_user_id);
        return prefs.getLong(context.getString(R.string.pref_user_id), invalid_id);
    }

    private static final String ENCODED_DELIMITED_USER_ID = "encoded_delimited_user_id";
    private static final int USER_ID_GROUPING = 4;
    public static void setEncodedDelimitedUserId(Context context, long user_id) {
        String encoded = UserUtil.encodeUserId(user_id);
        String delimited = UserUtil.delimitEncodedUserId(encoded, USER_ID_GROUPING);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(ENCODED_DELIMITED_USER_ID, delimited);
        editor.commit();
    }

    public static String getEncodedDelimitedUserId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String encoded = prefs.getString(ENCODED_DELIMITED_USER_ID, "");
        if (TextUtils.isEmpty(encoded)) {
            // in-case we didn't save it
            setEncodedDelimitedUserId(context, getUserId(context));
        }

        return prefs.getString(ENCODED_DELIMITED_USER_ID, "");
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
     * locally generated ids are negative.
     */
    public static long getCurrentBranchId(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        long default_id = c.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(c.getString(R.string.local_last_branch_id), default_id);
    }

    /**
     * We add -1 to get a new id for each entity.
     */
    public static long getNewBranchId(Context context) {
        return getCurrentBranchId(context) - 1;
    }

    public static void setNewBranchId(Context context, long branch_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_branch_id), branch_id);
        editor.commit();
    }

    public static long getCurrentItemId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_item_id), default_id);
    }

    public static long getNewItemId(Context context) {
        return getCurrentItemId(context) - 1;
    }

    public static void setNewItemId(Context context, long item_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_item_id), item_id);
        editor.commit();
    }

    public static long getCurrentTransId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_trans_id), default_id);
    }

    public static long getNewTransId(Context context) {
        return getCurrentTransId(context) - 1;
    }

    public static void setNewTransId(Context context, long trans_id) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putLong(context.getString(R.string.local_last_trans_id), trans_id);
        editor.commit();
    }

    public static long getCurrentCategoryId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long default_id = context.getResources().getInteger(R.integer.default_local_entity_id);
        return prefs.getLong(context.getString(R.string.local_last_category_id), default_id);
    }

    public static long getNewCategoryId(Context context) {
        return getCurrentCategoryId(context) - 1;
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

    private static final String KEY_SHOW_CATEGORY_CARD = "key_show_category_card";
    private static final String KEY_SHOW_CARD_BACKUP = "key_show_card_backup";
    private static final String KEY_SHOW_CATEGORY_TREE = "key_show_category_tree";

    public static boolean showCategoryCards(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_SHOW_CATEGORY_CARD, false);
    }

    public static void setCategoryCardShow(Context context, boolean show_cards) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_SHOW_CATEGORY_CARD, show_cards);
        editor.commit();
        if (show_cards) {
            setShowCategoryTreeState(context, true);
        }
    }

    public static boolean getShowCategoryTreeState(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_SHOW_CATEGORY_TREE, true);
    }

    public static void setShowCategoryTreeState(Context context, boolean show_tree) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_SHOW_CATEGORY_TREE, show_tree);
        editor.commit();
        if (!show_tree) {
            setCategoryCardShow(context, false);
        }
    }

    static String _to_str(int i) {
        return Integer.toString(i);
    }

    static String _to_str(long l) {
        return Long.toString(l);
    }

    /**
     * Encode the current state so it may be restored later.
     * This can then be persisted, e.g: stored in database.
     */
    public static String getEncodedStateBackup(Context c) {
        int branch_rev = getBranchRevision(c);
        int item_rev = getItemRevision(c);
        int branch_item_rev = getBranchItemRevision(c);
        int trans_rev = getTransactionRevision(c);
        int category_rev = getCategoryRevision(c);

        long c_branch_id = getCurrentBranchId(c);
        long c_item_id = getCurrentItemId(c);
        long c_trans_id = getCurrentTransId(c);
        long c_category_id = getCurrentCategoryId(c);

        Vector<String> vec = new Vector<>();
        vec.add(_to_str(branch_rev));
        vec.add(_to_str(item_rev));
        vec.add(_to_str(branch_item_rev));
        vec.add(_to_str(trans_rev));
        vec.add(_to_str(category_rev));

        vec.add(_to_str(c_branch_id));
        vec.add(_to_str(c_item_id));
        vec.add(_to_str(c_trans_id));
        vec.add(_to_str(c_category_id));

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < vec.size(); i++) {
            if (i > 0)
                builder.append(":");
            builder.append(vec.get(i));
        }
        return builder.toString();
    }

    static int _to_i(String s) { return Integer.parseInt(s); }
    static long _to_l(String s) { return Long.parseLong(s); }
    public static void restoreStateFromBackup(Context context, String bkup) {
        if (bkup == null) return;
        bkup = bkup.trim();

        if (TextUtils.isEmpty(bkup))
            return;

        String[] values = bkup.split(":");

        if (values.length != 9) {
            Log.e("PrefUtil", "Invalid restoration");
            return;
        }

        int branch_rev = _to_i(values[0]);
        int item_rev = _to_i(values[1]);
        int branch_item_rev = _to_i(values[2]);
        int trans_rev = _to_i(values[3]);
        int category_rev = _to_i(values[4]);

        long c_branch_id = _to_l(values[5]);
        long c_item_id = _to_l(values[6]);
        long c_trans_id = _to_l(values[7]);
        long c_category_id = _to_l(values[8]);

        setBranchRevision(context, branch_rev);
        setItemRevision(context, item_rev);
        setBranchItemRevision(context, branch_item_rev);
        setTransactionRevision(context, trans_rev);
        setCategoryRevision(context, category_rev);

        setNewBranchId(context, c_branch_id);
        setNewItemId(context, c_item_id);
        setNewTransId(context, c_trans_id);
        setNewCategoryId(context, c_category_id);
    }
}
