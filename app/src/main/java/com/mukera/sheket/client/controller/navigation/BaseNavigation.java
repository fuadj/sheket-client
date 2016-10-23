package com.mukera.sheket.client.controller.navigation;

import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.HashMap;

/**
 * Created by fuad on 7/29/16.
 */
public abstract class BaseNavigation {
    public interface NavigationCallback {
        void onBranchSelected(SBranch branch);
        void onNavigationOptionSelected(int item);
        void onCompanySelected(SCompany company);
    }

    private AppCompatActivity mActivity;
    private View mRootView;
    private NavigationCallback mCallback;
    private SPermission mUserPermission;

    /**
     * Setup the navigation UI.
     * NOTE: the activity needs to implement {@code NavigationCallback}.
     * @param activity
     * @param view
     */
    public void setUpNavigation(AppCompatActivity activity, View view) {
        mActivity = activity;
        mRootView = view;
        mCallback = (NavigationCallback) activity;

        onSetup();
    }

    /**
     * Call this if there is a change in user permissions and UI needs to update.
     */
    public void userPermissionChanged() {
        onUserPermissionChanged();
    }

    protected void onUserPermissionChanged() { }

    protected abstract void onSetup();

    public View getRootView() {
        return mRootView;
    }

    public AppCompatActivity getNavActivity() {
        return mActivity;
    }

    public NavigationCallback getCallBack() { return mCallback; }

    public SPermission getUserPermission() {
        return SPermission.getUserPermission(getNavActivity());
    }

    /**
     * Holes the different Static Navigation ids and their Icons.
     */
    public static class StaticNavigationOptions {
        public static final int OPTION_ITEM_LIST = 0;
        public static final int OPTION_BRANCHES = 1;
        public static final int OPTION_COMPANIES = 2;
        public static final int OPTION_EMPLOYEES = 3;
        public static final int OPTION_USER_PROFILE = 4;
        public static final int OPTION_SETTINGS = 5;
        public static final int OPTION_SYNC = 6;
        public static final int OPTION_DEBUG = 7;
        public static final int OPTION_LOG_OUT = 8;
        public static final int OPTION_IMPORT = 10;
        public static final int OPTION_DELETE = 11;
        public static final int OPTION_TRANSACTIONS = 12;
        public static final int OPTION_LANGUAGES = 13;

        public static final HashMap<Integer,
                        Pair<Integer, Integer>> sEntityAndIcon;

        static {
            sEntityAndIcon = new HashMap<>();
            sEntityAndIcon.put(OPTION_ITEM_LIST,
                    new Pair<>(R.string.nav_items, R.drawable.ic_action_new_all_items));
            sEntityAndIcon.put(OPTION_IMPORT,
                    new Pair<>(R.string.nav_import, R.drawable.ic_action_new_import));
            sEntityAndIcon.put(OPTION_SYNC,
                    new Pair<>(R.string.nav_sync, R.drawable.ic_action_new_sync));
            sEntityAndIcon.put(OPTION_TRANSACTIONS,
                    new Pair<>(R.string.nav_transactions, R.drawable.ic_action_new_transactions));
            sEntityAndIcon.put(OPTION_LANGUAGES,
                    new Pair<>(R.string.nav_languages, R.drawable.ic_action_globe));
            sEntityAndIcon.put(OPTION_BRANCHES,
                    new Pair<>(R.string.nav_branches, R.drawable.ic_action_new_branches));
            sEntityAndIcon.put(OPTION_COMPANIES,
                    new Pair<>(R.string.nav_companies, R.drawable.ic_company));
            sEntityAndIcon.put(OPTION_EMPLOYEES,
                    new Pair<>(R.string.nav_employees, R.drawable.ic_action_new_employees));
            sEntityAndIcon.put(OPTION_SETTINGS,
                    new Pair<>(R.string.nav_settings, R.drawable.ic_action_new_settings));
            sEntityAndIcon.put(OPTION_USER_PROFILE,
                    new Pair<>(R.string.nav_user_profile, R.drawable.ic_action_user_profile));
            sEntityAndIcon.put(OPTION_DEBUG,
                    new Pair<>(R.string.nav_debug, R.mipmap.ic_action_settings));
            /*
            sEntityAndIcon.put(OPTION_DELETE,
                    new Pair<>("Delete", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(OPTION_LOG_OUT,
                    new Pair<>("Logout", R.mipmap.ic_action_logout));
                    */
        }

        public static int getOptionString(int id) {
            return sEntityAndIcon.get(id).first;
        }
    }
}
