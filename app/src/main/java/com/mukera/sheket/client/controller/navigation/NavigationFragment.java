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
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gamma on 3/27/16.
 */
public class NavigationFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private BranchSelectionCallback mCallback;

    private ListView mBranchListView;
    private ListView mAdminListView;
    private ListView mUserListView;

    private NavigationBranchAdapter mNavigationBranchAdapter;
    private StaticNavigationAdapter mAdminAdapter, mUserAdapter;
    private TextView mSeparator1TextView;
    private TextView mSeparator2TextView;
    private TextView mSeparator3TextView;

    private TextView mCompanyName;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_navigation, container, false);

        mCompanyName = (TextView) rootView.findViewById(R.id.navigation_text_view_company_name);
        mCompanyName.setText(PrefUtil.getCurrentCompanyName(getActivity()));

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

        mAdminListView = (ListView) rootView.findViewById(R.id.navigation_list_view_admin);
        mAdminAdapter = new StaticNavigationAdapter(getContext());
        mAdminListView.setAdapter(mAdminAdapter);
        if (user_permission == SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            List<Integer> adminCategories = new ArrayList<>();
            if (user_permission == SPermission.PERMISSION_TYPE_ALL_ACCESS) {
                adminCategories.add(StaticNavigationAdapter.ENTITY_ALL_ITEMS);
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

        mUserListView = (ListView) rootView.findViewById(R.id.navigation_list_view_user);
        mUserAdapter = new StaticNavigationAdapter(getContext());
        mUserListView.setAdapter(mUserAdapter);

        List<Integer> userCategories = new ArrayList<>();
        userCategories.add(StaticNavigationAdapter.ENTITY_SYNC);
        userCategories.add(StaticNavigationAdapter.ENTITY_USER_PROFILE);
        userCategories.add(StaticNavigationAdapter.ENTITY_COMPANIES);
        userCategories.add(StaticNavigationAdapter.ENTITY_SETTINGS);
        userCategories.add(StaticNavigationAdapter.ENTITY_DEBUG);
        userCategories.add(StaticNavigationAdapter.ENTITY_LOG_OUT);
        for (Integer _i : userCategories) {
            mUserAdapter.add(_i);
        }
        mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer elem = mUserAdapter.getItem(position);
                mCallback.onElementSelected(elem);
            }
        });


        View separator1 = rootView.findViewById(R.id.separator_1);
        mSeparator1TextView = (TextView) separator1.findViewById(R.id.text_view_separator);
        mSeparator1TextView.setText("Branches");

        View separator2 = rootView.findViewById(R.id.separator_2);
        mSeparator2TextView = (TextView) separator2.findViewById(R.id.text_view_separator);
        mSeparator2TextView.setText("Management");
        if (user_permission != SPermission.PERMISSION_TYPE_ALL_ACCESS) {
            separator2.setVisibility(View.GONE);
        } else {
            separator2.setVisibility(View.VISIBLE);
        }

        View separator3 = rootView.findViewById(R.id.separator_3);
        mSeparator3TextView = (TextView) separator3.findViewById(R.id.text_view_separator);
        mSeparator3TextView.setText("Preferences");

        ListUtils.setDynamicHeight(mBranchListView);
        ListUtils.setDynamicHeight(mAdminListView);
        ListUtils.setDynamicHeight(mUserListView);

        return rootView;
    }


    public static class ListUtils {
        public static void setDynamicHeight(ListView mListView) {
            ListAdapter mListAdapter = mListView.getAdapter();
            if (mListAdapter == null) {
                // when adapter is null
                return;
            }
            int height = 0;
            int desiredWidth = View.MeasureSpec.makeMeasureSpec(mListView.getWidth(), View.MeasureSpec.UNSPECIFIED);
            for (int i = 0; i < mListAdapter.getCount(); i++) {
                View listItem = mListAdapter.getView(i, null, mListView);
                listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                height += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = mListView.getLayoutParams();
            params.height = height + (mListView.getDividerHeight() * (mListAdapter.getCount() - 1));
            mListView.setLayoutParams(params);
            mListView.requestLayout();
        }
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
            holder.elementName.setText(branch.branch_name);
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

        public static final HashMap<Integer,
                Pair<String, Integer>> sEntityAndIcon;

        static {
            sEntityAndIcon = new HashMap<>();
            sEntityAndIcon.put(ENTITY_ALL_ITEMS,
                    new Pair<>("All Items", R.mipmap.ic_action_home));
            sEntityAndIcon.put(ENTITY_SYNC,
                    new Pair<>("Sync Now", R.mipmap.ic_action_refresh));
            sEntityAndIcon.put(ENTITY_BRANCHES,
                    new Pair<>("Branches", R.drawable.ic_action_place));
            sEntityAndIcon.put(ENTITY_COMPANIES,
                    new Pair<>("Companies", R.mipmap.ic_company));
            sEntityAndIcon.put(ENTITY_MEMBERS,
                    new Pair<>("Members", R.drawable.ic_action_group));
            sEntityAndIcon.put(ENTITY_HISTORY,
                    new Pair<>("History", R.drawable.ic_action_history));
            sEntityAndIcon.put(ENTITY_USER_PROFILE,
                    new Pair<>("User Profile", R.drawable.ic_action_person));
            sEntityAndIcon.put(ENTITY_SETTINGS,
                    new Pair<>("Settings", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(ENTITY_DEBUG,
                    new Pair<>("Debug", R.mipmap.ic_action_settings));
            sEntityAndIcon.put(ENTITY_LOG_OUT,
                    new Pair<>("Logout", R.drawable.ic_action_warning));
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

            public StaticNavViewHolder(View view) {
                elemImage = (ImageView) view.findViewById(R.id.list_item_static_nav_icon);
                elemName = (TextView) view.findViewById(R.id.list_item_static_nav_name);
            }
        }


    }
}
