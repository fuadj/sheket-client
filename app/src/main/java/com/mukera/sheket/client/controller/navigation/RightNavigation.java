package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.Utils;

import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import java.util.List;

/**
 * Created by fuad on 7/29/16.
 */
public class RightNavigation extends BaseNavigation implements LoaderCallbacks<Cursor> {
    private ListView mBranchList;
    private BranchAdapter mBranchAdapter;

    private View mViewSync, mViewTransaction, mViewItems;

    void configureStaticOptions() {
        ViewHolder holder = new ViewHolder(mViewSync);
        holder.name.setText(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_SYNC).first);
        holder.icon.setImageResource(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_SYNC).second);

        holder = new ViewHolder(mViewTransaction);
        holder.name.setText(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_TRANSACTIONS).first);
        holder.icon.setImageResource(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_TRANSACTIONS).second);

        holder = new ViewHolder(mViewItems);
        holder.name.setText(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_ITEM_LIST).first);
        holder.icon.setImageResource(StaticNavigationOptions.sEntityAndIcon.get(
                StaticNavigationOptions.OPTION_ITEM_LIST).second);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean option_matched = false;
                int selected_option = -1;
                switch (v.getId()) {
                    case R.id.nav_right_sync:
                        selected_option = StaticNavigationOptions.OPTION_SYNC;
                        option_matched = true; break;
                    case R.id.nav_right_transactions:
                        selected_option = StaticNavigationOptions.OPTION_TRANSACTIONS;
                        option_matched = true; break;
                    case R.id.nav_right_items:
                        selected_option = StaticNavigationOptions.OPTION_ITEM_LIST;
                        option_matched = true; break;
                }
                if (option_matched) {
                    getCallBack().onNavigationOptionSelected(selected_option);
                }
            }
        };
        mViewSync.setOnClickListener(listener);
        mViewTransaction.setOnClickListener(listener);
        if (getUserPermission().getPermissionType() != SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            mViewItems.setVisibility(View.GONE);
        } else {
            mViewItems.setOnClickListener(listener);
        }
    }

    @Override
    protected void onSetup() {
        mViewSync = getRootView().findViewById(R.id.nav_right_sync);
        mViewTransaction = getRootView().findViewById(R.id.nav_right_transactions);
        mViewItems = getRootView().findViewById(R.id.nav_right_items);

        configureStaticOptions();

        mBranchList = (ListView) getRootView().findViewById(R.id.nav_right_list_view_branches);
        mBranchAdapter = new BranchAdapter(getNavActivity());
        mBranchList.setAdapter(mBranchAdapter);
        mBranchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mBranchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SBranch branch = new SBranch(cursor);
                    getCallBack().onBranchSelected(branch);
                }
            }
        });

        getNavActivity().getSupportLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_LIST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = SheketContract.BranchEntry._full(SheketContract.BranchEntry.COLUMN_BRANCH_ID) + " ASC";

        String selection = null;
        String[] selectionArgs = null;

        if (getUserPermission().getPermissionType() == SPermission.PERMISSION_TYPE_LISTED_BRANCHES) {
            List<Long> branches = getUserPermission().getAllowedBranches();
            selection = "";
            selectionArgs = new String[branches.size()];
            for (int i = 0; i < branches.size(); i++) {
                if (i != 0) {
                    selection += " OR ";
                }
                selection += SheketContract.BranchEntry._full(SheketContract.BranchEntry.COLUMN_BRANCH_ID) + " = ? ";
                selectionArgs[i] = String.valueOf(branches.get(i));
            }
        }

        return new CursorLoader(getNavActivity(),
                SheketContract.BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getNavActivity())),
                SBranch.BRANCH_COLUMNS,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBranchAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBranchAdapter.swapCursor(null);
    }

    static class BranchAdapter extends CursorAdapter {

        public BranchAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_nav_right, parent, false);
            ViewHolder holder = new ViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            // we don't show an icon for branches
            holder.icon.setVisibility(View.GONE);
            holder.namePadding.setVisibility(View.VISIBLE);

            SBranch branch = new SBranch(cursor);
            holder.name.setText(Utils.toTitleCase(branch.branch_name));
        }
    }

    static class ViewHolder {
        TextView name;
        ImageView icon;
        View namePadding;

        public ViewHolder(View view) {
            name = (TextView) view.findViewById(R.id.list_item_nav_right_text_name);
            icon = (ImageView) view.findViewById(R.id.list_item_nav_right_icon);
            namePadding = view.findViewById(R.id.list_item_nav_right_name_left_padding);

            view.setTag(this);
        }
    }
}
