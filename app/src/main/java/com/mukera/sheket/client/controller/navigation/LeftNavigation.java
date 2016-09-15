package com.mukera.sheket.client.controller.navigation;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
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

import com.google.android.gms.analytics.HitBuilders;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.SheketTracker;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.SheketNetworkUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

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
                setTitle(R.string.dialog_edit_user_profile_title).
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String new_name = editText.getText().toString().trim();

                        SheketTracker.setScreenName(getNavActivity(), SheketTracker.SCREEN_NAME_MAIN);
                        SheketTracker.sendTrackingData(getNavActivity(),
                                new HitBuilders.EventBuilder().
                                        setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                        setAction("change username selected").
                                        build());

                        final ProgressDialog progress = ProgressDialog.show(
                                getNavActivity(),
                                getString(R.string.dialog_edit_user_profile_progress_title),
                                getString(R.string.dialog_edit_user_profile_progress_body),
                                true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Pair<Boolean, String> result = updateCurrentUserName(new_name);
                                getNavActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.dismiss();
                                        dialog.dismiss();

                                        Map<String, String> trackingData;
                                        if (result.first == Boolean.TRUE) {
                                            trackingData = new HitBuilders.EventBuilder().
                                                        setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                                        setAction("username change successful").
                                                        build();
                                            new AlertDialog.Builder(getNavActivity()).
                                                    setIcon(android.R.drawable.ic_dialog_info).
                                                    setMessage(R.string.dialog_edit_user_profile_result_success).
                                                    setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                        @Override
                                                        public void onDismiss(DialogInterface dialog) {
                                                            LocalBroadcastManager.getInstance(getNavActivity()).
                                                                    sendBroadcast(new Intent(SheketBroadcast.ACTION_USER_CONFIG_CHANGE));
                                                        }
                                                    }).
                                                    setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                        @Override
                                                        public void onCancel(DialogInterface dialog) {
                                                            LocalBroadcastManager.getInstance(getNavActivity()).
                                                                    sendBroadcast(new Intent(SheketBroadcast.ACTION_USER_CONFIG_CHANGE));
                                                        }
                                                    }).
                                                    show();
                                        } else {
                                            trackingData = new HitBuilders.EventBuilder().
                                                    setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                                    setAction("change username error").
                                                    setLabel(result.second).
                                                    build();

                                            new AlertDialog.Builder(getNavActivity()).
                                                    setIcon(android.R.drawable.ic_dialog_alert).
                                                    setTitle(R.string.dialog_edit_user_profile_result_error).
                                                    setMessage(result.second).
                                                    show();
                                        }

                                        SheketTracker.setScreenName(getNavActivity(), SheketTracker.SCREEN_NAME_MAIN);
                                        SheketTracker.sendTrackingData(getNavActivity(), trackingData);
                                    }
                                });
                            }
                        }).start();
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

    String getString(int res_id) {
        return getNavActivity().getString(res_id);
    }

    static final OkHttpClient client = new OkHttpClient();

    Pair<Boolean, String> updateCurrentUserName(String new_name) {
        final String REQUEST_NEW_USER_NAME = "new_user_name";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(REQUEST_NEW_USER_NAME, new_name);

            Request.Builder builder = new Request.Builder();
            builder.url(ConfigData.getAddress(getNavActivity()) + "v1/user/edit/name");
            builder.addHeader(getString(R.string.pref_request_key_cookie),
                    PrefUtil.getLoginCookie(getNavActivity()));
            builder.post(RequestBody.create(MediaType.parse("application/json"),
                    jsonObject.toString()));

            Response response = client.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                return new Pair<>(Boolean.FALSE, SheketNetworkUtil.getErrorMessage(response));
            }

            PrefUtil.setUserName(getNavActivity(), new_name);

            return new Pair<>(Boolean.TRUE, null);
        } catch (JSONException | IOException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
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
