package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.CompanyUtil;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;

/**
 * Created by fuad on 7/29/16.
 */
public class LeftNavigation extends BaseNavigation implements LoaderManager.LoaderCallbacks<Cursor> {
    private View mLayoutProfile;
    private TextView mProfileUserName;

    private ListView mCompanyList;
    private CompanyAdapter mCompanyAdapter;

    private ListView mPreferenceList;
    private StaticNavAdapter mPrefAdapter;

    private View mMangerLayout;
    private ListView mManagerList;
    private StaticNavAdapter mManagementAdapter;

    @Override
    protected void onSetup() {
        mLayoutProfile = getRootView().findViewById(R.id.left_nav_layout_user_profile);
        mLayoutProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayProfileDetails();
            }
        });
        mProfileUserName = (TextView) getRootView().findViewById(R.id.left_nav_text_user_name);
        mProfileUserName.setText(PrefUtil.getUsername(getNavActivity()));

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
                SCompany company = new SCompany(cursor);
                getCallBack().onCompanySelected(company);
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

        if (getUserPermission().getPermissionType() != SPermission.PERMISSION_TYPE_ALL_ACCESS) {
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

    void displayProfileDetails() {
        View view = getNavActivity().getLayoutInflater().inflate(R.layout.dialog_show_profile, null);

        TextView username = (TextView) view.findViewById(R.id.dialog_show_profile_username);
        TextView id = (TextView) view.findViewById(R.id.dialog_show_profile_id);

        long user_id = PrefUtil.getUserId(getNavActivity());

        username.setText(PrefUtil.getUsername(getNavActivity()));
        id.setText(IdEncoderUtil.encodeAndDelimitId(user_id, IdEncoderUtil.ID_TYPE_USER));

        ImageButton editNameBtn = (ImageButton) view.findViewById(R.id.dialog_show_profile_edit_name);
        editNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayEditUsernameDialog();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(getNavActivity());
        builder.setView(view).show();
    }

    void displayEditUsernameDialog() {
        final EditText editText = new EditText(getNavActivity());
        final String current_user_name = PrefUtil.getUsername(getNavActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getNavActivity()).
                setTitle(R.string.dialog_edit_profile_title).
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).
                setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // start things off with the current name, the "OK" button should be invisible
        editText.setText(current_user_name);

        final AlertDialog dialog = builder.create();

        editText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_name = s.toString().trim();

                // only enable editing if there is a "non-empty name" and
                // it is different from the current one
                boolean show_ok_btn = !new_name.isEmpty() &&
                        !new_name.equals(current_user_name);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).
                        setVisibility(show_ok_btn ? View.VISIBLE : View.GONE);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // initially don't show the "Ok" button b/c the name hasn't changed
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        });

        dialog.show();
    }

    @Override
    public void onUserPermissionChanged() {
        // this will "re-calibrate" the UI
        onSetup();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CompanyEntry._full(CompanyEntry.COLUMN_COMPANY_ID) + " ASC";

        return new CursorLoader(getNavActivity(),
                CompanyEntry.CONTENT_URI,
                SCompany.COMPANY_COLUMNS,
                CompanyEntry.COLUMN_USER_ID + " = ?",
                new String[]{
                        String.valueOf(PrefUtil.getUserId(getNavActivity()))
                },
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
            SCompany company = new SCompany(cursor);

            CompanyViewHolder holder = (CompanyViewHolder) view.getTag();
            holder.name.setText(company.name);
            int icon_res;
            if (company.company_id == mCurrentCompanyId) {
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
