package com.mukera.sheket.client.controller.navigation;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
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
import com.mukera.sheket.client.LanguageSelectionDialog;
import com.mukera.sheket.client.MainActivity;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.SheketGRPCCall;
import com.mukera.sheket.client.SheketTracker;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.network.Company;
import com.mukera.sheket.client.network.EditUserNameRequest;
import com.mukera.sheket.client.network.NewCompanyRequest;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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

                boolean is_add_company_selected = CompanyAdapter.isAddCompanyRow(cursor, position);
                // checking for the "negation" is much cleaner as the "negation" is only 3 lines
                // it saves us a lot of indentation.
                if (!is_add_company_selected) {
                    SCompany company = new SCompany(cursor);
                    getCallBack().onCompanySelected(company);
                    return;
                }

                /**
                 * We require READ_PHONE_STATE to get device_id. We send that device id to the
                 * server so the company is tied to the phone. This is to prevent users from
                 * creating "fake-facebook-accounts" to freely use the app.
                 */
                // there is a bug in android M, declaring the permission in the manifest isn't enough
                // see: http://stackoverflow.com/a/38782876/5753416
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int permissionCheck = ContextCompat.checkSelfPermission(getNavActivity(), Manifest.permission.READ_PHONE_STATE);

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        displayAddCompanyDialog();
                    } else {
                        ActivityCompat.requestPermissions(getNavActivity(), new String[]{Manifest.permission.READ_PHONE_STATE},
                                MainActivity.REQUEST_READ_PHONE_STATE);
                    }
                } else {
                    displayAddCompanyDialog();
                }
            }
        });

        mPreferenceList = (ListView) getRootView().findViewById(R.id.nav_left_list_view_preference);
        mPrefAdapter = new StaticNavAdapter(getNavActivity());
        mPreferenceList.setAdapter(mPrefAdapter);

        mPrefAdapter.add(BaseNavigation.StaticNavigationOptions.OPTION_LANGUAGES);
        mPrefAdapter.add(StaticNavigationOptions.OPTION_DEBUG);
        ListUtils.setDynamicHeight(mPreferenceList);

        mPreferenceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer i = mPrefAdapter.getItem(position);
                switch (i) {
                    case BaseNavigation.StaticNavigationOptions.OPTION_LANGUAGES:
                        LanguageSelectionDialog.
                                displayLanguageConfigurationDialog(getNavActivity(), true);
                        break;
                    case StaticNavigationOptions.OPTION_DEBUG:
                        getCallBack().onNavigationOptionSelected(StaticNavigationOptions.OPTION_DEBUG);
                        break;
                }
            }
        });

        // check if user has managerial role
        mMangerLayout = getRootView().findViewById(R.id.nav_left_layout_management);

        if (getUserPermission().getPermissionType() != SPermission.PERMISSION_TYPE_OWNER) {
            mMangerLayout.setVisibility(View.GONE);
        } else {
            mMangerLayout.setVisibility(View.VISIBLE);
            mManagerList = (ListView) getRootView().findViewById(R.id.nav_left_list_view_management);

            mManagementAdapter = new StaticNavAdapter(getNavActivity());

            mManagerList.setAdapter(mManagementAdapter);
            mManagementAdapter.add(StaticNavigationOptions.OPTION_BRANCHES);
            mManagementAdapter.add(StaticNavigationOptions.OPTION_EMPLOYEES);
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

    void displayAddCompanyDialog() {
        final EditText editText = new EditText(getNavActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getNavActivity()).
                setTitle(R.string.dialog_new_company_title).
                setMessage(R.string.dialog_new_company_body).
                setView(editText).
                setPositiveButton(R.string.dialog_new_company_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String company_name = editText.getText().toString().trim();

                        SheketTracker.setScreenName(getNavActivity(), SheketTracker.SCREEN_NAME_MAIN);
                        SheketTracker.sendTrackingData(getNavActivity(),
                                new HitBuilders.EventBuilder().
                                        setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                        setAction("create company selected").
                                        build());

                        final ProgressDialog progress = ProgressDialog.show(
                                getNavActivity(),
                                getString(R.string.dialog_new_company_progress_title),
                                getString(R.string.dialog_new_company_progress_body),
                                true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Pair<Boolean, String> result = createNewCompany(getNavActivity(), company_name);
                                getNavActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.dismiss();
                                        dialog.dismiss();

                                        Map<String, String> trackingData;
                                        if (result.first == Boolean.TRUE) {
                                            trackingData = new HitBuilders.EventBuilder().
                                                    setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                                    setAction("company creation successful").
                                                    build();
                                            new AlertDialog.Builder(getNavActivity()).
                                                    setIcon(android.R.drawable.ic_dialog_info).
                                                    setMessage(R.string.dialog_new_company_success).
                                                    setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                        @Override
                                                        public void onDismiss(DialogInterface dialog) {
                                                            LocalBroadcastManager.getInstance(getNavActivity()).
                                                                    sendBroadcast(new Intent(SheketBroadcast.ACTION_COMPANY_SWITCH));
                                                        }
                                                    }).
                                                    setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                        @Override
                                                        public void onCancel(DialogInterface dialog) {
                                                            LocalBroadcastManager.getInstance(getNavActivity()).
                                                                    sendBroadcast(new Intent(SheketBroadcast.ACTION_COMPANY_SWITCH));
                                                        }
                                                    }).
                                                    show();
                                        } else {
                                            trackingData = new HitBuilders.EventBuilder().
                                                    setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                                    setAction("create company error").
                                                    setLabel(result.second).
                                                    build();

                                            new AlertDialog.Builder(getNavActivity()).
                                                    setIcon(android.R.drawable.ic_dialog_alert).
                                                    setTitle(R.string.dialog_new_company_error).
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
                setNeutralButton(R.string.dialog_new_company_btn_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();

        editText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(
                        !name.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // b/c there is no name initially, hide "ok" btn
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        });

        dialog.show();
    }

    /**
     * Tries to create a company by sending request to the server. If all goes well,
     * it returns <True, Null>. Otherwise it returns <False, "error message">
     */
    Pair<Boolean, String> createNewCompany(Activity activity, final String company_name) {
        try {
            int company_id;
            String user_permission;
            String license;
            String payment_id;

            if (PrefUtil.isUserLocallyCreated(getNavActivity())) {
                // TODO: generate local company id and name
                //user_permission = created_company.getPermission();
                //license = created_company.getSignedLicense();
                company_id = -2;
                user_permission = "";
                license = "";
                payment_id = "";
            } else {
                Company created_company = new SheketGRPCCall<Company>().runBlockingCall(
                        new SheketGRPCCall.GRPCCallable<Company>() {
                            @Override
                            public Company runGRPCCall() throws Exception {
                                ManagedChannel managedChannel = ManagedChannelBuilder.
                                        forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                                        usePlaintext(true).
                                        build();

                                SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                                        SheketServiceGrpc.newBlockingStub(managedChannel);
                                return blockingStub.createCompany(
                                        NewCompanyRequest.
                                                newBuilder().
                                                setAuth(SheketAuth.newBuilder().setLoginCookie(
                                                        PrefUtil.getLoginCookie(getNavActivity()))).
                                                setCompanyName(company_name).
                                                setDeviceId(DeviceId.getUniqueDeviceId(getNavActivity())).
                                                setLocalUserTime(
                                                        String.valueOf(System.currentTimeMillis())
                                                ).build()
                                );
                            }
                        }
                );

                company_id = created_company.getCompanyId();
                user_permission = created_company.getPermission();
                license = created_company.getSignedLicense();
                payment_id = created_company.getPaymentId();
            }

            ContentValues values = new ContentValues();
            values.put(CompanyEntry.COLUMN_COMPANY_ID, company_id);
            values.put(CompanyEntry.COLUMN_USER_ID, PrefUtil.getUserId(activity));
            values.put(CompanyEntry.COLUMN_NAME, company_name);
            values.put(CompanyEntry.COLUMN_PERMISSION, user_permission);
            values.put(CompanyEntry.COLUMN_PAYMENT_LICENSE, license);
            values.put(CompanyEntry.COLUMN_PAYMENT_ID, payment_id);

            Uri uri = activity.getContentResolver().insert(
                    CompanyEntry.CONTENT_URI, values
            );
            if (ContentUris.parseId(uri) < 0) {
                return new Pair<>(Boolean.FALSE, "error adding company into db");
            }

            // TODO: do a TOTAL company switch. We shouldn't be using other company
            // state for this company.
            PrefUtil.setCurrentCompanyId(activity, company_id);
            PrefUtil.setUserPermission(activity, user_permission);

        } catch (SheketGRPCCall.SheketException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
        return new Pair<>(Boolean.TRUE, null);
    }

    void displayProfileDetails() {
        View view = getNavActivity().getLayoutInflater().inflate(R.layout.dialog_show_profile, null);

        TextView username = (TextView) view.findViewById(R.id.dialog_show_profile_username);
        TextView id = (TextView) view.findViewById(R.id.dialog_show_profile_id);

        int user_id = PrefUtil.getUserId(getNavActivity());

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
        try {
            ManagedChannel managedChannel = ManagedChannelBuilder.
                    forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                    usePlaintext(true).
                    build();

            SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                    SheketServiceGrpc.newBlockingStub(managedChannel);

            String cookie = PrefUtil.getLoginCookie(getNavActivity());

            // TODO: check if we need a better check
            // we don't really have a response, we just need to check if we can
            // "pass" the call without throwing an exception. If that happened it means
            // a "non-error" result.
            blockingStub.editUserName(EditUserNameRequest.newBuilder().
                    setNewName(new_name).
                    setAuth(SheketAuth.newBuilder().setLoginCookie(cookie)).
                    build());

            PrefUtil.setUserName(getNavActivity(), new_name);
            return new Pair<>(Boolean.TRUE, null);
        } catch (StatusRuntimeException e) {
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
        if (data.getCount() == 0) {     // show the "Add Company" if there are no companies
            /**
             * FIXME: Because {@code SCompany.COMPANY_COLUMNS} are fully qualified(they have the format table_name.column_name)
             * that creates a problem when trying to find the "_id" column, which just tries to
             * search for a column with "_id".
             *
             * FIXME: (Workaround) So use the "un-qualified" column name for company id. This isn't a
             * hack because the {@code MatrixCursor} will probably won't change.
             *
             * This happens here and not other-places because {@code MatrixCursor} just stores the
             * column names "raw" plainly. so when they ask the cursor to find the "_id" column it
             * gets screw-up. Other cursors from actual ContentProvider queries don't have this problem.
             *
             * See {@code AbstractCursor.getColumnIndex} for more
             * Also this Bug issue Tracker https://code.google.com/p/android/issues/detail?id=7201.
             */
            String[] company_columns = SCompany.COMPANY_COLUMNS;
            company_columns[0] = CompanyEntry.COLUMN_COMPANY_ID;
            MatrixCursor addCompanyRowCursor = new MatrixCursor(company_columns);

            // adding the columns adds it in-order from left to right, so make sure company_id column in the first.
            addCompanyRowCursor.newRow().add(CompanyAdapter.ADD_COMPANY_ROW_COMPANY_ID);
            /**
             * TODO: we are adding the "add company" cursor to the top and not at the bottom b/c
             * doing that creates an exception when selecting the first company.
             *
             * android.database.CursorIndexOutOfBoundsException: Index -1 requested, with a size
             */
            mCompanyAdapter.swapCursor(new MergeCursor(
                    new Cursor[]{
                            addCompanyRowCursor,
                            data,
                    }
            ));
        } else {
            mCompanyAdapter.swapCursor(data);
        }

        ListUtils.setDynamicHeight(mCompanyList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCompanyAdapter.swapCursor(null);
    }

    static class CompanyAdapter extends CursorAdapter {
        private int mCurrentCompanyId;

        public CompanyAdapter(Context context) {
            super(context, null);
            mCurrentCompanyId = PrefUtil.getCurrentCompanyId(context);
        }

        private final int VIEW_TYPE_COMPANY = 0;
        private final int VIEW_TYPE_ADD_COMPANY = 1;

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public static final int ADD_COMPANY_ROW_COMPANY_ID = -1;

        /**
         * Checks if the row is pointing to the "add company" cell.
         *
         * @param position the row of the cursor
         */
        public static boolean isAddCompanyRow(Cursor cursor, int position) {
            if (!cursor.moveToPosition(position)) return false;

            return new SCompany(cursor).company_id == ADD_COMPANY_ROW_COMPANY_ID;
        }

        @Override
        public int getItemViewType(int position) {
            return isAddCompanyRow(getCursor(), position) ? VIEW_TYPE_ADD_COMPANY : VIEW_TYPE_COMPANY;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view;

            if (isAddCompanyRow(cursor, cursor.getPosition())) {
                view = LayoutInflater.from(context).inflate(
                        R.layout.list_item_nav_left_add_company, parent, false);
            } else {
                view = LayoutInflater.from(context).inflate(
                        R.layout.list_item_nav_left_companies, parent, false);
                CompanyListViewHolder holder = new CompanyListViewHolder(view);
                view.setTag(holder);
            }

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // the "add company" view is "static" things, we don't change anything here
            if (isAddCompanyRow(cursor, cursor.getPosition()))
                return;

            SCompany company = new SCompany(cursor);

            CompanyListViewHolder holder = (CompanyListViewHolder) view.getTag();
            holder.name.setText(company.name);
            int icon_res;
            if (company.company_id == mCurrentCompanyId) {
                icon_res = R.drawable.abc_btn_check_to_on_mtrl_015;
            } else {
                icon_res = R.drawable.abc_btn_check_to_on_mtrl_000;
            }
            holder.icon.setImageResource(icon_res);
        }

        static class CompanyListViewHolder {
            TextView name;
            ImageView icon;

            public CompanyListViewHolder(View view) {
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
