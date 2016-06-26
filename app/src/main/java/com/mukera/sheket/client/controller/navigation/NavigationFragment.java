package com.mukera.sheket.client.controller.navigation;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gamma on 3/27/16.
 */
public class NavigationFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private BranchSelectionCallback mCallback;

    private ListView mBranchListView;
    private ListView mSyncingListView;
    private ListView mAdminListView;
    private ExpandableListView mSettingsListView;

    private NavigationBranchAdapter mNavigationBranchAdapter;
    private StaticNavigationAdapter mAdminAdapter, mSyncAdapter;
    private StaticExpandableListAdapter mSettingsAdapter;

    private TextView mSeparator1TextView;
    private TextView mSeparator2TextView;
    private TextView mSeparator3TextView;
    private TextView lSeparator4TextView;

    private TextView mCompanyName;

    private ScrollView mContainerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_navigation, container, false);

        mContainerView = (ScrollView) rootView.findViewById(R.id.navigation_scroll_view_container);

        mCompanyName = (TextView) rootView.findViewById(R.id.navigation_text_view_company_name);
        mCompanyName.setText(Utils.toTitleCase(PrefUtil.getCurrentCompanyName(getActivity())));

        SPermission.setSingletonPermission(PrefUtil.getUserPermission(getContext()));
        int user_permission = SPermission.getSingletonPermission().getPermissionType();

        mBranchListView = (ListView) rootView.findViewById(R.id.navigation_list_view_branches);
        mNavigationBranchAdapter = new NavigationBranchAdapter(getContext());
        mBranchListView.setAdapter(mNavigationBranchAdapter);
        mBranchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mNavigationBranchAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SBranch branch = new SBranch(cursor);
                    mCallback.onBranchSelected(branch);
                }
            }
        });

        mSyncingListView = (ListView) rootView.findViewById(R.id.navigation_list_view_syncing);
        mSyncAdapter = new StaticNavigationAdapter(getContext());
        mSyncingListView.setAdapter(mSyncAdapter);

        List<Integer> syncCategories = new ArrayList<>();
        syncCategories.add(StaticNavigationAdapter.ENTITY_TRANSACTIONS);
        syncCategories.add(StaticNavigationAdapter.ENTITY_SYNC);
        for (Integer i : syncCategories) {
            mSyncAdapter.add(i);
        }
        mSyncingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer i = mSyncAdapter.getItem(position);
                mCallback.onElementSelected(i);
            }
        });

        mAdminListView = (ListView) rootView.findViewById(R.id.navigation_list_view_admin);
        mAdminAdapter = new StaticNavigationAdapter(getContext());
        mAdminListView.setAdapter(mAdminAdapter);
        if (user_permission == SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            List<Integer> adminCategories = new ArrayList<>();
            if (user_permission == SPermission.PERMISSION_TYPE_ALL_ACCESS) {
                adminCategories.add(StaticNavigationAdapter.ENTITY_ALL_ITEMS);
                adminCategories.add(StaticNavigationAdapter.ENTITY_IMPORT);
                adminCategories.add(StaticNavigationAdapter.ENTITY_HISTORY);
                adminCategories.add(StaticNavigationAdapter.ENTITY_BRANCHES);
                adminCategories.add(StaticNavigationAdapter.ENTITY_MEMBERS);
            }
            for (Integer _i : adminCategories) {
                mAdminAdapter.add(_i);
            }
            mAdminListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Integer elem = mAdminAdapter.getItem(position);
                    mCallback.onElementSelected(elem);
                }
            });
        }

        mSettingsListView = (ExpandableListView) rootView.findViewById(R.id.navigation_list_view_settings);
        mSettingsAdapter = new StaticExpandableListAdapter(getContext());
        mSettingsListView.setAdapter(mSettingsAdapter);

        List<Integer> settingsChildren = new ArrayList<>();
        settingsChildren.add(StaticNavigationAdapter.ENTITY_USER_PROFILE);
        settingsChildren.add(StaticNavigationAdapter.ENTITY_COMPANIES);
        settingsChildren.add(StaticNavigationAdapter.ENTITY_DEBUG);
        settingsChildren.add(StaticNavigationAdapter.ENTITY_DELETE);
        settingsChildren.add(StaticNavigationAdapter.ENTITY_LOG_OUT);

        Pair<Integer, List<Integer>> settingsCategory =
                new Pair<>(StaticNavigationAdapter.ENTITY_SETTINGS, settingsChildren);

        List<Pair<Integer, List<Integer>>> listData = new ArrayList<>();
        listData.add(settingsCategory);

        mSettingsAdapter.setData(listData);

        mSettingsListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Integer elem = (Integer)mSettingsAdapter.getChild(groupPosition, childPosition);
                mCallback.onElementSelected(elem);
                return true;
            }
        });

        mSettingsListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                ListUtils.setDynamicHeight(mSettingsListView);
            }
        });

        mSettingsListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(final int groupPosition) {
                ListUtils.setDynamicHeight(mSettingsListView);
                mContainerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mContainerView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        View separator1 = rootView.findViewById(R.id.separator_1);
        mSeparator1TextView = (TextView) separator1.findViewById(R.id.text_view_separator);
        mSeparator1TextView.setText("Branches");

        View separator2 = rootView.findViewById(R.id.separator_2);
        mSeparator2TextView = (TextView) separator2.findViewById(R.id.text_view_separator);
        mSeparator2TextView.setText("Syncing");

        View separator3 = rootView.findViewById(R.id.separator_3);
        mSeparator3TextView = (TextView) separator3.findViewById(R.id.text_view_separator);
        mSeparator3TextView.setText("Management");
        if (user_permission != SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            separator3.setVisibility(View.GONE);
        } else {
            separator3.setVisibility(View.VISIBLE);
        }

        View separator4 = rootView.findViewById(R.id.separator_4);
        lSeparator4TextView = (TextView) separator4.findViewById(R.id.text_view_separator);
        lSeparator4TextView.setText("Preferences");

        ListUtils.setDynamicHeight(mBranchListView);
        ListUtils.setDynamicHeight(mSyncingListView);
        ListUtils.setDynamicHeight(mAdminListView);
        ListUtils.setDynamicHeight(mSettingsListView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_LIST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LoaderId.MainActivity.BRANCH_LIST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";

        return new CursorLoader(getActivity(),
                BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SBranch.BRANCH_COLUMNS,
                null, null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mNavigationBranchAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mBranchListView);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNavigationBranchAdapter.swapCursor(null);
        ListUtils.setDynamicHeight(mBranchListView);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (BranchSelectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement BranchSelectionCallback.");
        }
    }

    public interface BranchSelectionCallback {
        void onBranchSelected(SBranch branch);

        void onElementSelected(int item);
    }

    public static class NavigationBranchAdapter extends CursorAdapter {

        public static class NavigationViewHolder {
            TextView elementName;

            public NavigationViewHolder(View view) {
                elementName = (TextView) view.findViewById(R.id.text_view_list_item_entity_name);
                view.setTag(this);
            }
        }


        public NavigationBranchAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_navigation, parent, false);
            NavigationViewHolder holder = new NavigationViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            NavigationViewHolder holder = (NavigationViewHolder) view.getTag();
            SBranch branch = new SBranch(cursor);
            holder.elementName.setText(Utils.toTitleCase(branch.branch_name));
        }
    }

    public static class StaticNavigationAdapter extends ArrayAdapter<Integer> {
        public static final int ENTITY_ALL_ITEMS = 0;
        public static final int ENTITY_BRANCHES = 1;
        public static final int ENTITY_COMPANIES = 2;
        public static final int ENTITY_MEMBERS = 3;
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
            sEntityAndIcon.put(ENTITY_MEMBERS,
                    new Pair<>("Members", R.mipmap.ic_action_members));
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

        private Context mContext;

        public StaticNavigationAdapter(Context context) {
            super(context, 0);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer item = getItem(position);

            StaticNavViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.list_item_static_navigation, parent, false);
                holder = new StaticNavViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StaticNavViewHolder) convertView.getTag();
            }
            holder.elemName.setText(sEntityAndIcon.get(item).first);
            holder.elemImage.setImageResource(sEntityAndIcon.get(item).second);

            return convertView;
        }

        public static class StaticNavViewHolder {
            ImageView elemImage;
            TextView elemName;
            View indentationView;
            ImageView expandImgView, collapseImgView;

            public StaticNavViewHolder(View view) {
                elemImage = (ImageView) view.findViewById(R.id.list_item_static_nav_icon);
                elemName = (TextView) view.findViewById(R.id.list_item_static_nav_name);

                indentationView = view.findViewById(R.id.list_item_static_indentation_view);
                expandImgView = (ImageView) view.findViewById(R.id.list_item_static_img_expand);
                collapseImgView = (ImageView) view.findViewById(R.id.list_item_static_img_collapse);
            }
        }
    }

    public static class StaticExpandableListAdapter extends BaseExpandableListAdapter {
        private Context mContext;
        private List<Pair<Integer, List<Integer>>> mData;
        public StaticExpandableListAdapter(Context context) {
            super();
            mContext = context;
            mData = new ArrayList<>();
        }

        public void setData(List<Pair<Integer, List<Integer>>> data) {
            mData = data;
            if (mData == null)
                mData = new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public int getGroupCount() {
            return mData.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mData.get(groupPosition).second.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mData.get(groupPosition).first;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mData.get(groupPosition).second.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            Integer item = (Integer)getGroup(groupPosition);

            StaticNavigationAdapter.StaticNavViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.list_item_static_navigation, parent, false);
                holder = new StaticNavigationAdapter.StaticNavViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StaticNavigationAdapter.StaticNavViewHolder) convertView.getTag();
            }
            holder.elemName.setText(StaticNavigationAdapter.sEntityAndIcon.get(item).first);
            holder.elemImage.setImageResource(StaticNavigationAdapter.sEntityAndIcon.get(item).second);

            holder.collapseImgView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.expandImgView.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            Integer item = (Integer)getChild(groupPosition, childPosition);

            StaticNavigationAdapter.StaticNavViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.list_item_static_navigation, parent, false);
                holder = new StaticNavigationAdapter.StaticNavViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (StaticNavigationAdapter.StaticNavViewHolder) convertView.getTag();
            }

            holder.elemName.setText(StaticNavigationAdapter.sEntityAndIcon.get(item).first);
            holder.elemImage.setImageResource(StaticNavigationAdapter.sEntityAndIcon.get(item).second);
            holder.indentationView.setVisibility(View.VISIBLE);

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
