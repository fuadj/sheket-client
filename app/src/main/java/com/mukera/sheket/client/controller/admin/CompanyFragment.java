package com.mukera.sheket.client.controller.admin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.PrefUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by gamma on 4/3/16.
 */
public class CompanyFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final OkHttpClient client = new OkHttpClient();

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

    private ListView mCompanies;
    private CompanyAdapter mAdapter;
    private SPermission.PermissionChangeListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_companies, container, false);

        mAdapter = new CompanyAdapter(getContext());
        mCompanies = (ListView) rootView.findViewById(R.id.companies_list_view);
        mCompanies.setAdapter(mAdapter);
        mCompanies.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mAdapter.getCursor();
                if (cursor == null ||
                        !cursor.moveToPosition(position)) {
                    return;
                }

                final long company_id = cursor.getLong(COL_COMPANY_ID);
                if (PrefUtil.getCurrentCategoryId(getActivity()) == company_id) {
                    // there is nothing to do, we are already viewing that company
                    return;
                }

                final String company_name = cursor.getString(COL_NAME);
                final String permission = cursor.getString(COL_PERMISSION);
                final String state_bkup = cursor.getString(COL_STATE_BKUP);

                final Context context = getActivity();

                // This isn't just run in the thread below for EFFICIENCY, because
                // most users only have 1 company, starting a thread is unnecessary
                // b/c we won't be switching between companies and there is no state backup necessary.
                if (!PrefUtil.isCompanySet(context)) {
                    PrefUtil.setCurrentCompanyId(getActivity(), company_id);
                    PrefUtil.setCurrentCompanyName(getActivity(), company_name);
                    PrefUtil.setUserPermission(getActivity(), permission);
                    PrefUtil.restoreStateFromBackup(context, state_bkup);

                    SPermission.setSingletonPermission(permission);

                    if (mListener != null) {
                        mListener.userPermissionChanged();
                    }
                } else {
                    // save the current company's state, we are switching!!!
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            long current_company = PrefUtil.getCurrentCompanyId(context);
                            String current_state = PrefUtil.getEncodedStateBackup(context);

                            ContentValues values = new ContentValues();
                            // Yes, It is valid to only include the values you want to update
                            values.put(CompanyEntry.COLUMN_STATE_BACKUP, current_state);

                            context.getContentResolver().
                                    update(
                                            CompanyEntry.CONTENT_URI,
                                            values,
                                            CompanyEntry._full(CompanyEntry.COLUMN_ID) + " = ?",
                                            new String[]{
                                                    String.valueOf(current_company)
                                            }
                                    );

                            PrefUtil.setCurrentCompanyId(getActivity(), company_id);
                            PrefUtil.setCurrentCompanyName(getActivity(), company_name);
                            PrefUtil.setUserPermission(getActivity(), permission);
                            PrefUtil.restoreStateFromBackup(context, state_bkup);

                            SPermission.setSingletonPermission(permission);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mListener != null) {
                                        mListener.userPermissionChanged();
                                    }
                                }
                            });
                        }
                    }).start();
                }
            }
        });

        Button createCompanyBtn = (Button) rootView.findViewById(R.id.companies_btn_create);
        createCompanyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                CompanyCreateDialog dialog = new CompanyCreateDialog();
                dialog.fragment = CompanyFragment.this;
                dialog.show(fm, "Create Company");
            }
        });

        getLoaderManager().initLoader(LoaderId.MainActivity.COMPANY_LIST_LOADER, null, this);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (SPermission.PermissionChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PermissionChangeListener");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CompanyEntry._full(CompanyEntry.COLUMN_ID) + " ASC";

        return new CursorLoader(getActivity(),
                CompanyEntry.CONTENT_URI,
                COMPANY_COLUMNS,
                null, null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public static class CompanyAdapter extends CursorAdapter {
        static class CompanyViewHolder {
            TextView companyName;

            public CompanyViewHolder(View view) {
                companyName = (TextView) view.findViewById(R.id.company_list_item_name);
            }
        }

        public CompanyAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_company, parent, false);
            CompanyViewHolder holder = new CompanyViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CompanyViewHolder holder = (CompanyViewHolder) view.getTag();
            holder.companyName.setText(cursor.getString(COL_NAME));
        }

    }

    public static class CompanyCreateDialog extends DialogFragment {
        private EditText mCompanyName;
        public CompanyFragment fragment;
        private Button mBtnCreate;
        private ProgressDialog mProgressDialog;

        void setButtonStatus() {
            mBtnCreate.setEnabled(!mCompanyName.getText().toString().trim().isEmpty());
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_new_company, container);

            mCompanyName = (EditText) view.findViewById(R.id.dialog_edit_text_company_name);
            mCompanyName.addTextChangedListener(new TextWatcherAdapter(){
                @Override
                public void afterTextChanged(Editable s) {
                    setButtonStatus();
                }
            });
            Button btnCancel;

            btnCancel = (Button) view.findViewById(R.id.dialog_btn_company_cancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
                }
            });
            mBtnCreate = (Button) view.findViewById(R.id.dialog_btn_company_create);
            mBtnCreate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Activity activity = getActivity();
                    final String company_name = mCompanyName.getText().toString();
                    mProgressDialog = ProgressDialog.show(
                            getActivity(), "Creating Company", "Please Wait...", true);
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            // TODO: give some feedback if it is success/failiure(maybe no internet)
                            createCompany(activity, company_name);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.dismiss();
                                    getDialog().dismiss();
                                    if (fragment.mListener != null) {
                                        fragment.mListener.userPermissionChanged();
                                    }
                                }
                            });
                        }
                    };
                    t.start();
                }
            });

            setButtonStatus();
            getDialog().setCanceledOnTouchOutside(false);
            return view;
        }

        void createCompany(Activity activity, String company_name) {
            final String JSON_COMPANY_NAME = "company_name";
            final String JSON_COMPANY_ID = activity.getString(R.string.pref_header_key_company_id);
            final String JSON_USER_PERMISSION = "user_permission";
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_COMPANY_NAME, company_name);

                Request.Builder builder = new Request.Builder();
                builder.url(ConfigData.getAddress(getActivity()) + "v1/company/create");
                builder.addHeader(activity.getString(R.string.pref_request_key_cookie),
                        PrefUtil.getLoginCookie(activity));
                builder.post(RequestBody.create(MediaType.parse("application/json"),
                        jsonObject.toString()));

                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    // TODO: signify error
                    throw new CompanyCreateException("error response");
                }

                JSONObject result = new JSONObject(response.body().string());
                long company_id = result.getLong(JSON_COMPANY_ID);
                String user_permission = result.getString(JSON_USER_PERMISSION);

                ContentValues values = new ContentValues();
                values.put(CompanyEntry.COLUMN_ID, company_id);
                values.put(CompanyEntry.COLUMN_NAME, company_name);
                values.put(CompanyEntry.COLUMN_PERMISSION, user_permission);

                Uri uri = activity.getContentResolver().insert(
                        CompanyEntry.CONTENT_URI, values
                );
                if (ContentUris.parseId(uri) < 0) {
                    throw new CompanyCreateException("error adding company into db");
                }
                PrefUtil.setCurrentCompanyId(activity, company_id);
                PrefUtil.setCurrentCompanyName(activity, company_name);
                PrefUtil.setUserPermission(activity, user_permission);

                SPermission.setSingletonPermission(user_permission);
            } catch (JSONException | IOException | CompanyCreateException e) {
                Log.e("CompanyFragment", e.getMessage());
            }
        }

        class CompanyCreateException extends Exception {
            public CompanyCreateException() {
                super();
            }

            public CompanyCreateException(String detailMessage) {
                super(detailMessage);
            }

            public CompanyCreateException(String detailMessage, Throwable throwable) {
                super(detailMessage, throwable);
            }

            public CompanyCreateException(Throwable throwable) {
                super(throwable);
            }
        }
    }
}
