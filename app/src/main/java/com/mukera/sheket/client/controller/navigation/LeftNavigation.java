package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.CompanyUtil;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 7/29/16.
 */
public class LeftNavigation extends BaseNavigation implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String[] COMPANY_COLUMNS = {
            CompanyEntry._full(CompanyEntry.COLUMN_ID),
            CompanyEntry._full(CompanyEntry.COLUMN_NAME),
            CompanyEntry._full(CompanyEntry.COLUMN_PERMISSION),
            CompanyEntry._full(CompanyEntry.COLUMN_STATE_BACKUP)
    };

    private static final int COL_COMPANY_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PERMISSION = 2;
    private static final int COL_STATE_BKUP = 3;

    private ListView mCompanyList;
    private CompanyAdapter mCompanyAdapter;

    private ListView mPreferenceList;
    private StaticNavAdapter mPrefAdapter;

    private View mMangerLayout;
    private ListView mManagerList;
    private StaticNavAdapter mManagementAdapter;

    @Override
    protected void onSetup() {
        mCompanyList = (ListView) getRootView().findViewById(R.id.nav_left_list_view_companies);
        mCompanyAdapter = new CompanyAdapter(getNavActivity());
        mCompanyList.setAdapter(mCompanyAdapter);
        mCompanyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mCompanyAdapter.getCursor();
                if (cursor == null ||
                        !cursor.moveToPosition(position)) {
                    return;
                }

                final long company_id = cursor.getLong(COL_COMPANY_ID);
                if (PrefUtil.getCurrentCategoryId(getNavActivity()) == company_id) {
                    // there is nothing to do, we are already viewing that company
                    return;
                }

                final String company_name = cursor.getString(COL_NAME);
                final String permission = cursor.getString(COL_PERMISSION);
                final String state_bkup = cursor.getString(COL_STATE_BKUP);

                CompanyUtil.switchCurrentCompanyInWorkerThread(getNavActivity(),
                        company_id, company_name, permission, state_bkup,
                        new CompanyUtil.StateSwitchedListener() {
                            @Override
                            public void runAfterSwitchCompleted() {
                                getCallBack().onCompanySwitched();
                            }
                        });

            }
        });

        mPreferenceList = (ListView) getRootView().findViewById(R.id.nav_left_list_view_preference);
        mPrefAdapter = new StaticNavAdapter(getNavActivity());
        mPreferenceList.setAdapter(mPrefAdapter);

        mPrefAdapter.add(StaticNavigationOptions.OPTION_SETTINGS);
        ListUtils.setDynamicHeight(mPreferenceList);

        mPreferenceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer i = mPrefAdapter.getItem(position);
                getCallBack().onNavigationOptionSelected(i);
            }
        });

        // check if user has managerial role
        mMangerLayout = getRootView().findViewById(R.id.nav_left_layout_management);

        if (getUserPermission() != SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            mMangerLayout.setVisibility(View.GONE);
        } else {
            mMangerLayout.setVisibility(View.VISIBLE);
            mManagerList = (ListView) getRootView().findViewById(R.id.nav_left_list_view_management);

            mManagementAdapter = new StaticNavAdapter(getNavActivity());

            mManagerList.setAdapter(mManagementAdapter);
            mManagementAdapter.add(StaticNavigationOptions.OPTION_BRANCHES);
            mManagementAdapter.add(StaticNavigationOptions.OPTION_EMPLOYEES);
            mManagementAdapter.add(StaticNavigationOptions.OPTION_IMPORT);
            ListUtils.setDynamicHeight(mManagerList);

            mManagerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Integer i = mManagementAdapter.getItem(position);
                    getCallBack().onNavigationOptionSelected(i);
                }
            });
        }
        getNavActivity().getSupportLoaderManager().initLoader(LoaderId.MainActivity.COMPANY_LIST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CompanyEntry._full(CompanyEntry.COLUMN_ID) + " ASC";

        return new CursorLoader(getNavActivity(),
                CompanyEntry.CONTENT_URI,
                COMPANY_COLUMNS,
                null, null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCompanyAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mCompanyList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCompanyAdapter.swapCursor(null);
    }

    static class CompanyAdapter extends CursorAdapter {
        private long mCurrentCompanyId;
        public CompanyAdapter(Context context) {
            super(context, null);
            mCurrentCompanyId = PrefUtil.getCurrentCompanyId(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_nav_left_companies, parent, false);
            CompanyViewHolder holder = new CompanyViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CompanyViewHolder holder = (CompanyViewHolder) view.getTag();
            holder.name.setText(cursor.getString(COL_NAME));
            long company_id = cursor.getLong(COL_COMPANY_ID);
            int icon_res;
            if (company_id == mCurrentCompanyId) {
                icon_res = R.drawable.abc_btn_check_to_on_mtrl_015;
            } else {
                icon_res = R.drawable.abc_btn_check_to_on_mtrl_000;
            }
            holder.icon.setImageResource(icon_res);
        }

        static class CompanyViewHolder {
            TextView name;
            ImageView icon;

            public CompanyViewHolder(View view) {
                name = (TextView) view.findViewById(R.id.list_item_nav_left_company_name);
                icon = (ImageView) view.findViewById(R.id.list_item_nav_left_company_icon);
            }
        }
    }

    static class StaticNavAdapter extends ArrayAdapter<Integer> {
        public StaticNavAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer item = getItem(position);

            StaticNavViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_nav_left, parent, false);
                holder = new StaticNavViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StaticNavViewHolder) convertView.getTag();
            }

            holder.name.setText(StaticNavigationOptions.sEntityAndIcon.get(item).first);
            holder.icon.setImageResource(StaticNavigationOptions.sEntityAndIcon.get(item).second);

            return convertView;
        }

        private static class StaticNavViewHolder {
            TextView name;
            ImageView icon;

            public StaticNavViewHolder(View view) {
                name = (TextView) view.findViewById(R.id.list_item_nav_left_name);
                icon = (ImageView) view.findViewById(R.id.list_item_nav_left_icon);
                view.setTag(this);
            }
        }
    }
}
