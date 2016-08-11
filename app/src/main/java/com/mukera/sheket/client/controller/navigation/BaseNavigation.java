package com.mukera.sheket.client.controller.navigation;

import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SBranch;
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
        void onCompanySwitched();
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

        SPermission.setSingletonPermission(PrefUtil.getUserPermission(mActivity));
        mUserPermission = SPermission.getSingletonPermission();

        onSetup();
    }

    protected abstract void onSetup();

    public View getRootView() {
        return mRootView;
    }

    public AppCompatActivity getNavActivity() {
        return mActivity;
    }

    public NavigationCallback getCallBack() { return mCallback; }

    public SPermission getUserPermission() {
        return mUserPermission;
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
        public static final int OPTION_HISTORY = 9;
        public static final int OPTION_IMPORT = 10;
        public static final int OPTION_DELETE = 11;
        public static final int OPTION_TRANSACTIONS = 12;
        public static final int OPTION_LANGUAGES = 13;

        public static final HashMap<Integer,
                        Pair<String, Integer>> sEntityAndIcon;

        static {
            sEntityAndIcon = new HashMap<>();
            sEntityAndIcon.put(OPTION_ITEM_LIST,
                    new Pair<>("Items", R.drawable.ic_action_new_all_items));
            sEntityAndIcon.put(OPTION_IMPORT,
                    new Pair<>("Import", R.drawable.ic_action_new_import));
            sEntityAndIcon.put(OPTION_SYNC,
                    new Pair<>("Sync", R.drawable.ic_action_new_sync));
            sEntityAndIcon.put(OPTION_TRANSACTIONS,
                    new Pair<>("Transactions", R.drawable.ic_action_new_transactions));
            sEntityAndIcon.put(OPTION_LANGUAGES,
                    new Pair<>("Languages", R.mipmap.ic_action_transaction));
            sEntityAndIcon.put(OPTION_BRANCHES,
                    new Pair<>("Branches", R.drawable.ic_action_new_branches));
            sEntityAndIcon.put(OPTION_COMPANIES,
                    new Pair<>("Companies", R.mipmap.ic_company));
            sEntityAndIcon.put(OPTION_EMPLOYEES,
                    new Pair<>("Employees", R.drawable.ic_action_new_employees));
            sEntityAndIcon.put(OPTION_HISTORY,
                    new Pair<>("History", R.mipmap.ic_action_history));
            sEntityAndIcon.put(OPTION_USER_PROFILE,
                    new Pair<>("User Profile", R.drawable.ic_action_new_profile));
            sEntityAndIcon.put(OPTION_SETTINGS,
                    new Pair<>("Settings", R.drawable.ic_action_new_settings));
            sEntityAndIcon.put(OPTION_DEBUG,
                    new Pair<>("Debug", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(OPTION_DELETE,
                    new Pair<>("Delete", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(OPTION_LOG_OUT,
                    new Pair<>("Logout", R.mipmap.ic_action_logout));
        }
    }


}
