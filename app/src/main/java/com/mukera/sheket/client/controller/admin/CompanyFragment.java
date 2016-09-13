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
import android.support.v4.util.Pair;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
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

import com.mukera.sheket.client.controller.CompanyUtil;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.SheketNetworkUtil;
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
                SCompany company = new SCompany(cursor);

                if (PrefUtil.getCurrentCategoryId(getActivity()) == company.company_id) {
                    // there is nothing to do, we are already viewing that company
                    return;
                }

                CompanyUtil.switchCurrentCompanyInWorkerThread(getActivity(),
                        company,
                        new CompanyUtil.StateSwitchedListener() {
                            @Override
                            public void runAfterSwitchCompleted() {
                                if (mListener != null) {
                                    mListener.userPermissionChanged();
                                }
                            }
                        });

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
        String sortOrder = CompanyEntry._full(CompanyEntry.COLUMN_COMPANY_ID) + " ASC";

        return new CursorLoader(getActivity(),
                CompanyEntry.CONTENT_URI,
                SCompany.COMPANY_COLUMNS,
                CompanyEntry.COLUMN_USER_ID + " = ?",
                new String[]{
                        String.valueOf(PrefUtil.getUserId(getActivity()))
                },
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
            SCompany company = new SCompany(cursor);
            CompanyViewHolder holder = (CompanyViewHolder) view.getTag();
            holder.companyName.setText(company.name);
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
            mCompanyName.addTextChangedListener(new TextWatcherAdapter() {
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
                            final Pair<Boolean, String> result = createCompany(activity, company_name);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.dismiss();
                                    // the company creation was a success
                                    if (result.first == true) {
                                        getDialog().dismiss();
                                        if (fragment.mListener != null) {
                                            fragment.mListener.userPermissionChanged();
                                        }
                                    } else {
                                        new AlertDialog.Builder(getContext()).
                                                setTitle("error").
                                                setMessage(result.second).
                                                show();
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

        /**
         * Tries to create a company by sending request to the server. If all goes well,
         * it returns <True, Null>. Otherwise it returns <False, "error message">
         */
        Pair<Boolean, String> createCompany(Activity activity, String company_name) {
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
                    return new Pair<>(Boolean.FALSE, SheketNetworkUtil.getErrorMessage(response));
                }

                JSONObject result = new JSONObject(response.body().string());
                long company_id = result.getLong(JSON_COMPANY_ID);
                String user_permission = result.getString(JSON_USER_PERMISSION);

                ContentValues values = new ContentValues();
                values.put(CompanyEntry.COLUMN_COMPANY_ID, company_id);
                values.put(CompanyEntry.COLUMN_USER_ID, PrefUtil.getUserId(getActivity()));
                values.put(CompanyEntry.COLUMN_NAME, company_name);
                values.put(CompanyEntry.COLUMN_PERMISSION, user_permission);

                Uri uri = activity.getContentResolver().insert(
                        CompanyEntry.CONTENT_URI, values
                );
                if (ContentUris.parseId(uri) < 0) {
                    return new Pair<>(Boolean.FALSE, "error adding company into db");
                }
                PrefUtil.setCurrentCompanyId(activity, company_id);
                PrefUtil.setCurrentCompanyName(activity, company_name);
                PrefUtil.setUserPermission(activity, user_permission);

                SPermission.setSingletonPermission(user_permission);
            } catch (JSONException | IOException e) {
                return new Pair<>(Boolean.FALSE, e.getMessage());
            }
            return new Pair<>(Boolean.TRUE, null);
        }
    }
}
