package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.Utils;

import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

/**
 * Created by fuad on 7/29/16.
 */
public class RightNavigation extends BaseNavigation implements LoaderCallbacks<Cursor> {
    private ListView mBranchList;
    private BranchAdapter mBranchAdapter;

    private ListView mSyncList;
    private SyncAdapter mSyncAdapter;

    @Override
    protected void onSetup() {
        mSyncList = (ListView) getRootView().findViewById(R.id.nav_right_list_view_sync);
        mSyncAdapter = new SyncAdapter(getNavActivity());

        mSyncList.setAdapter(mSyncAdapter);
        mSyncAdapter.add(StaticNavigationEntities.ENTITY_TRANSACTIONS);
        mSyncAdapter.add(StaticNavigationEntities.ENTITY_SYNC);

        mSyncList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: notify listener
            }
        });
        ListUtils.setDynamicHeight(mSyncList);

        mBranchList = (ListView) getRootView().findViewById(R.id.nav_right_list_view_branches);
        mBranchAdapter = new BranchAdapter(getNavActivity());
        mBranchList.setAdapter(mBranchAdapter);
        mBranchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mBranchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SBranch branch = new SBranch(cursor);
                    // TODO: notify listener
                }
            }
        });
        getNavActivity().getSupportLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_LIST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = SheketContract.BranchEntry._full(SheketContract.BranchEntry.COLUMN_BRANCH_ID) + " ASC";

        return new CursorLoader(getNavActivity(),
                SheketContract.BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getNavActivity())),
                SBranch.BRANCH_COLUMNS,
                null, null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBranchAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mBranchList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBranchAdapter.swapCursor(null);
        ListUtils.setDynamicHeight(mBranchList);
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

            SBranch branch = new SBranch(cursor);
            holder.name.setText(Utils.toTitleCase(branch.branch_name));
        }
    }

    static class SyncAdapter extends ArrayAdapter<Integer> {
        public SyncAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer item = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_nav_right, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // we don't need padding when we show the icon
            holder.namePadding.setVisibility(View.GONE);

            holder.name.setText(StaticNavigationEntities.sEntityAndIcon.get(item).first);
            holder.icon.setImageResource(StaticNavigationEntities.sEntityAndIcon.get(item).second);

            return convertView;
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
