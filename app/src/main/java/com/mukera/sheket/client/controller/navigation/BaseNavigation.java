package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mukera.sheket.client.R;

import java.util.HashMap;

/**
 * Created by fuad on 7/29/16.
 */
public abstract class BaseNavigation {
    private AppCompatActivity mActivity;
    private View mRootView;

    public void setUpNavigation(AppCompatActivity activity, View view) {
        mActivity = activity;
        mRootView = view;
        onSetup();
    }

    protected abstract void onSetup();

    public View getRootView() {
        return mRootView;
    }

    public AppCompatActivity getNavActivity() {
        return mActivity;
    }

    /**
     * Holes the different Static Navigation ids and their Icons.
     */
    public static class StaticNavigationEntities {
        public static final int ENTITY_ALL_ITEMS = 0;
        public static final int ENTITY_BRANCHES = 1;
        public static final int ENTITY_COMPANIES = 2;
        public static final int ENTITY_EMPLOYEES = 3;
        public static final int ENTITY_USER_PROFILE = 4;
        public static final int ENTITY_SETTINGS = 5;
        public static final int ENTITY_SYNC = 6;
        public static final int ENTITY_DEBUG = 7;
        public static final int ENTITY_LOG_OUT = 8;
        public static final int ENTITY_HISTORY = 9;
        public static final int ENTITY_IMPORT = 10;
        public static final int ENTITY_DELETE = 11;
        public static final int ENTITY_TRANSACTIONS = 12;

        public static final HashMap<Integer,
                        Pair<String, Integer>> sEntityAndIcon;

        static {
            sEntityAndIcon = new HashMap<>();
            sEntityAndIcon.put(ENTITY_ALL_ITEMS,
                    new Pair<>("All Items", R.mipmap.ic_action_all_items));
            sEntityAndIcon.put(ENTITY_IMPORT,
                    new Pair<>("Import", R.mipmap.ic_action_import));
            sEntityAndIcon.put(ENTITY_SYNC,
                    new Pair<>("Sync Now", R.mipmap.ic_action_sync));
            sEntityAndIcon.put(ENTITY_TRANSACTIONS,
                    new Pair<>("Transactions", R.mipmap.ic_action_transaction));
            sEntityAndIcon.put(ENTITY_BRANCHES,
                    new Pair<>("Branches", R.mipmap.ic_action_branches));
            sEntityAndIcon.put(ENTITY_COMPANIES,
                    new Pair<>("Companies", R.mipmap.ic_company));
            sEntityAndIcon.put(ENTITY_EMPLOYEES,
                    new Pair<>("Employees", R.mipmap.ic_action_members));
            sEntityAndIcon.put(ENTITY_HISTORY,
                    new Pair<>("History", R.mipmap.ic_action_history));
            sEntityAndIcon.put(ENTITY_USER_PROFILE,
                    new Pair<>("User Profile", R.mipmap.ic_action_profile));
            sEntityAndIcon.put(ENTITY_SETTINGS,
                    new Pair<>("Settings", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(ENTITY_DEBUG,
                    new Pair<>("Debug", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(ENTITY_DELETE,
                    new Pair<>("Delete", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(ENTITY_LOG_OUT,
                    new Pair<>("Logout", R.mipmap.ic_action_logout));
        }
    }


}
