package com.mukera.sheket.client.controller.admin;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.support.v4.app.Fragment;

import com.mukera.sheket.client.controller.navigation.BaseNavigation;
import com.mukera.sheket.client.controller.user.UserUtil;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.SyncUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SMember;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.PrefUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gamma on 4/3/16.
 */
public class EmployeesFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final OkHttpClient client = new OkHttpClient();

    private ListView mMemberList;
    private MemberAdapter mAdapter;

    private List<SBranch> mBranches;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_members, container, false);

        mAdapter = new MemberAdapter(getContext());
        mMemberList = (ListView) rootView.findViewById(R.id.member_list_view);
        mMemberList.setAdapter(mAdapter);
        mMemberList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SMember member = new SMember(cursor);

                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    AddEditMemberDialog dialog = new AddEditMemberDialog();
                    dialog.setBranches(getBranches());
                    dialog.mDialogType = AddEditMemberDialog.MEMBER_DIALOG_EDIT;
                    dialog.mMember = member;
                    dialog.fragment = EmployeesFragment.this;
                    dialog.show(fm, "Edit Member");
                }
            }
        });

        Button addMemberBtn = (Button) rootView.findViewById(R.id.member_btn_add);
        addMemberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                AddEditMemberDialog dialog = new AddEditMemberDialog();
                dialog.setBranches(getBranches());
                dialog.mDialogType = AddEditMemberDialog.MEMBER_DIALOG_ADD;
                dialog.fragment = EmployeesFragment.this;
                dialog.mMember = null;
                dialog.show(fm, "Add Member");
            }
        });

        getLoaderManager().initLoader(LoaderId.MainActivity.MEMBER_LIST_LOADER, null, this);
        return rootView;
    }

    List<SBranch> getBranches() {
        if (mBranches == null) {
            mBranches = new ArrayList<>();
            long company_id = PrefUtil.getCurrentCompanyId(getActivity());

            String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";
            Cursor cursor = getActivity().getContentResolver().
                    query(BranchEntry.buildBaseUri(company_id),
                            SBranch.BRANCH_COLUMNS, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SBranch branch = new SBranch(cursor);
                    mBranches.add(branch);
                } while (cursor.moveToNext());
            }
        }
        return mBranches;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID) + " ASC";

        return new CursorLoader(getActivity(),
                MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SMember.MEMBER_COLUMNS,
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

    static class MemberAdapter extends CursorAdapter {
        static class MemberViewHolder {
            ImageButton btnDeleteMember;
            TextView textMemberName;
            TextView textMemberId;
            TextView textMemberPermission;

            public MemberViewHolder(View view) {
                btnDeleteMember = (ImageButton) view.findViewById(R.id.member_img_btn_delete);
                textMemberName = (TextView) view.findViewById(R.id.member_text_view_name);
                textMemberId = (TextView) view.findViewById(R.id.member_text_view_id);
                textMemberPermission = (TextView) view.findViewById(R.id.member_text_view_permission);
            }
        }

        public MemberAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_member, parent, false);
            MemberViewHolder holder = new MemberViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            MemberViewHolder holder = (MemberViewHolder) view.getTag();

            final SMember member = new SMember(cursor);

            holder.textMemberName.setText(member.member_name);
            holder.textMemberId.setText("" + member.member_id);
            holder.textMemberPermission.setText(SPermission.shortName(member.member_permission));
            holder.btnDeleteMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: request permission to delete member
                }
            });
        }
    }

    public static class AddEditMemberDialog extends DialogFragment {
        public static final int MEMBER_DIALOG_ADD = 1;
        public static final int MEMBER_DIALOG_EDIT = 2;

        public EmployeesFragment fragment;
        public int mDialogType;
        public SMember mMember;

        private EditText mEditMemberId;

        private TextView mMemberName;

        private Spinner mSpinnerPermissionType;
        private MultiSpinner mSpinnerBranches;

        private Button mBtnAddEditMember;

        private LinearLayout mLayoutBranchSelector;

        private List<SBranch> mBranches;

        private ProgressDialog mProgressDialog;

        private boolean mErrorOccurred;
        private String mErrorMsg;

        static class PermType {
            public int type;
            public String name;

            public PermType(int t, String n) {
                type = t;
                name = n;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        private static final HashMap<Integer, PermType> sPermTypesHashMap;

        static {
            sPermTypesHashMap = new HashMap<>();
            sPermTypesHashMap.put(SPermission.PERMISSION_TYPE_NONE,
                    new PermType(SPermission.PERMISSION_TYPE_NONE, "--Select Access--"));
            sPermTypesHashMap.put(SPermission.PERMISSION_TYPE_ALL_ACCESS,
                    new PermType(SPermission.PERMISSION_TYPE_ALL_ACCESS, "Manager"));
            sPermTypesHashMap.put(SPermission.PERMISSION_TYPE_LISTED_BRANCHES,
                    new PermType(SPermission.PERMISSION_TYPE_LISTED_BRANCHES, "Branch Employee"));
        }

        public void setBranches(List<SBranch> branches) {
            mBranches = branches;
        }

        void setOkButtonStatus() {
            String delimited_id = mEditMemberId.getText().toString().trim().
                    // remove any space
                            replaceAll("\\s+", "").
                    // also remove any non-alphanumeric characters
                            replaceAll("\\W+", "");

            String delimiter_removed = UserUtil.removeDelimiterOnEncodedId(delimited_id);
            if (!UserUtil.isValidEncodedId(delimiter_removed)) {
                mBtnAddEditMember.setEnabled(false);
                return;
            }

            PermType perm_type = (PermType) mSpinnerPermissionType.getSelectedItem();
            if (perm_type.type == SPermission.PERMISSION_TYPE_NONE) {
                mBtnAddEditMember.setEnabled(false);
                return;
            }

            if (mDialogType == MEMBER_DIALOG_ADD &&
                    mEditMemberId.getText().toString().trim().isEmpty()) {
                mBtnAddEditMember.setEnabled(false);
                return;
            }

            if (perm_type.type != SPermission.PERMISSION_TYPE_LISTED_BRANCHES) {
                mBtnAddEditMember.setEnabled(true);
                return;
            }

            boolean none_selected = true;
            for (boolean selected : mSpinnerBranches.getSelected()) {
                if (selected) {
                    none_selected = false;
                    break;
                }
            }
            if (none_selected) {
                mBtnAddEditMember.setEnabled(false);
                return;
            }
            mBtnAddEditMember.setEnabled(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_add_edit_member, container);

            final Drawable successIcon = getResources().getDrawable(R.drawable.ic_action_success);
            successIcon.setBounds(new Rect(0, 0, successIcon.getIntrinsicWidth(), successIcon.getIntrinsicHeight()));

            final View layout_name = view.findViewById(R.id.dialog_layout_member_name);
            mEditMemberId = (EditText) view.findViewById(R.id.dialog_edit_text_member_id);

            final boolean is_edit = mDialogType == MEMBER_DIALOG_EDIT;
            TextView title = (TextView) view.findViewById(R.id.dialog_text_view_member_action);
            if (is_edit) {
                title.setText(getString(R.string.placeholder_employee_action_edit_member));
                layout_name.setVisibility(View.VISIBLE);
                mMemberName = (TextView) view.findViewById(R.id.dialog_text_view_member_name);
                mMemberName.setText(mMember.member_name);
                mEditMemberId.setText(UserUtil.delimitEncodedUserId(UserUtil.encodeUserId(mMember.member_id), 4));
                mEditMemberId.setEnabled(false);
            } else {
                title.setText(getString(R.string.placeholder_employee_action_add_member));
                layout_name.setVisibility(View.GONE);
                mEditMemberId.setEnabled(true);
                mEditMemberId.addTextChangedListener(new TextWatcherAdapter() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        setOkButtonStatus();

                        String delimited_id = mEditMemberId.getText().toString().trim().
                                // remove any space
                                        replaceAll("\\s+", "").
                                // also remove any non-alphanumeric characters
                                        replaceAll("\\W+", "");

                        String delimiter_removed = UserUtil.removeDelimiterOnEncodedId(delimited_id);
                        if (UserUtil.isValidEncodedId(delimiter_removed)) {
                            // I know it is weird to call {@code setError} for telling success
                            // but we don't have an API for the success.
                            mEditMemberId.setError("Correct ID", successIcon);
                        } else {
                            mEditMemberId.setError(null);
                        }
                    }
                });
            }

            mLayoutBranchSelector = (LinearLayout) view.findViewById(R.id.dialog_member_layout_permission_branch);

            Button btnCancel;
            btnCancel = (Button) view.findViewById(R.id.dialog_btn_member_cancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
                }
            });

            List<PermType> types = new ArrayList<>();
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_NONE));
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_ALL_ACCESS));
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_LISTED_BRANCHES));

            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, types.toArray());
            mSpinnerPermissionType = (Spinner) view.findViewById(R.id.dialog_member_spinner_permission_type);
            mSpinnerPermissionType.setAdapter(adapter);
            mSpinnerPermissionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PermType type = (PermType) parent.getAdapter().getItem(position);
                    if (type.type == SPermission.PERMISSION_TYPE_LISTED_BRANCHES) {
                        mLayoutBranchSelector.setVisibility(View.VISIBLE);
                    } else {
                        mLayoutBranchSelector.setVisibility(View.GONE);
                    }
                    setOkButtonStatus();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            mSpinnerBranches = (MultiSpinner) view.findViewById(R.id.dialog_member_spinner_branch);
            List<String> branches = new ArrayList<>();
            for (SBranch branch : mBranches) {
                branches.add(branch.branch_name);
            }
            mSpinnerBranches.setItems(branches, "--Select Branch--",
                    new MultiSpinner.MultiSpinnerListener() {
                        @Override
                        public void onItemsSelected(boolean[] selected) {
                            setOkButtonStatus();
                        }
                    });

            mBtnAddEditMember = (Button) view.findViewById(R.id.dialog_btn_member_add_edit);
            mBtnAddEditMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Activity activity = getActivity();

                    final SMember member;
                    if (mDialogType == MEMBER_DIALOG_ADD) {
                        member = new SMember();
                        String id = mEditMemberId.getText().toString().trim();
                        member.member_id = UserUtil.decodeUserId(UserUtil.removeDelimiterOnEncodedId(id));
                        mProgressDialog = ProgressDialog.show(getActivity(),
                                "Adding Member", "Please wait...", true);
                    } else {
                        member = new SMember(mMember);
                    }

                    List<Long> branch_ids = new ArrayList<>();
                    boolean[] selected = mSpinnerBranches.getSelected();
                    for (int i = 0; i < selected.length; i++) {
                        if (selected[i] == true) {
                            branch_ids.add(mBranches.get(i).branch_id);
                        }
                    }

                    SPermission permission = new SPermission();
                    PermType permType = (PermType) mSpinnerPermissionType.getSelectedItem();
                    permission.setPermissionType(permType.type);
                    permission.setAllowedBranches(branch_ids);

                    member.member_permission = permission;

                    final boolean is_editing = mDialogType == MEMBER_DIALOG_EDIT;

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            mErrorOccurred = false;
                            if (is_editing) {
                                updateMember(activity, member);
                            } else {
                                addMember(activity, member);
                            }
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mProgressDialog != null)
                                        mProgressDialog.dismiss();
                                    getDialog().dismiss();

                                    if (!is_editing) {
                                        if (mErrorOccurred) {
                                            new AlertDialog.Builder(getActivity()).
                                                    setTitle("Member Error").
                                                    setMessage(mErrorMsg).show();
                                        } else {
                                            new AlertDialog.Builder(getActivity()).
                                                    setTitle("Member Success").
                                                    setMessage("Member Added").show();
                                        }
                                    }
                                }
                            });
                        }
                    };
                    t.start();
                }
            });
            setOkButtonStatus();
            return view;
        }

        void updateMember(Activity activity, SMember member) {
            long company_id = PrefUtil.getCurrentCompanyId(activity);
            member.company_id = company_id;
            ContentValues values = member.toContentValues();
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_UPDATED);
            DbUtil.setUpdateOnConflict(values);

            getActivity().getContentResolver().insert(MemberEntry.buildBaseUri(company_id), values);
        }

        void addMember(Activity activity, SMember member) {
            final String JSON_MEMBER_ID = "user_id";
            final String JSON_MEMBER_PERMISSION = "user_permission";

            // This is part of the response
            final String JSON_MEMBER_NAME = "username";

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_MEMBER_ID, member.member_id);
                jsonObject.put(JSON_MEMBER_PERMISSION, member.member_permission.Encode());

                Request.Builder builder = new Request.Builder();
                builder.url(ConfigData.getAddress(getActivity()) + "v1/member/add");
                builder.addHeader(getContext().getString(R.string.pref_header_key_company_id),
                        Long.toString(PrefUtil.getCurrentCompanyId(getContext())));
                builder.addHeader(activity.getString(R.string.pref_request_key_cookie),
                        PrefUtil.getLoginCookie(activity));
                builder.post(RequestBody.create(MediaType.parse("application/json"),
                        jsonObject.toString()));

                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    throw new MemberException(SyncUtil.getErrorMessage(response));
                }

                JSONObject json = new JSONObject(response.body().string());

                String member_name = json.getString(JSON_MEMBER_NAME);

                ContentValues values = new ContentValues();
                values.put(MemberEntry.COLUMN_COMPANY_ID,
                        PrefUtil.getCurrentCompanyId(activity));
                values.put(MemberEntry.COLUMN_MEMBER_ID, member.member_id);
                values.put(MemberEntry.COLUMN_MEMBER_NAME, member_name);
                values.put(MemberEntry.COLUMN_MEMBER_PERMISSION, member.member_permission.Encode());

                // b/c we directly added the member, we've synced it with the server
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_SYNCED);

                Uri uri = activity.getContentResolver().
                        insert(MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())), values);
                if (MemberEntry.getMemberId(uri) < 0) {
                    throw new MemberException("error adding member into company");
                }
            } catch (JSONException | IOException | MemberException e) {
                mErrorOccurred = true;
                mErrorMsg = e.getMessage();
            }
        }
    }

    static class MemberException extends Exception {
        public MemberException() {
            super();
        }

        public MemberException(String detailMessage) {
            super(detailMessage);
        }

        public MemberException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public MemberException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * http://stackoverflow.com/questions/5015686/android-spinner-with-multiple-choice
     */
    public static class MultiSpinner extends Spinner implements
            DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnCancelListener {

        private List<String> items;
        private boolean[] selected;
        private String defaultText;
        private MultiSpinnerListener listener;

        public MultiSpinner(Context context) {
            super(context);
        }

        public MultiSpinner(Context arg0, AttributeSet arg1) {
            super(arg0, arg1);
        }

        public MultiSpinner(Context arg0, AttributeSet arg1, int arg2) {
            super(arg0, arg1, arg2);
        }

        public boolean[] getSelected() {
            return selected;
        }

        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            if (isChecked)
                selected[which] = true;
            else
                selected[which] = false;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // refresh text on spinner
            StringBuffer spinnerBuffer = new StringBuffer();
            boolean someSelected = false;
            for (int i = 0; i < items.size(); i++) {
                if (selected[i] == true) {
                    if (someSelected)
                        spinnerBuffer.append(", ");
                    spinnerBuffer.append(items.get(i));
                    someSelected = true;
                }
            }
            String spinnerText;
            if (someSelected) {
                spinnerText = spinnerBuffer.toString();
            } else {
                spinnerText = defaultText;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{spinnerText});
            setAdapter(adapter);
            listener.onItemsSelected(selected);
        }

        @Override
        public boolean performClick() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMultiChoiceItems(
                    items.toArray(new CharSequence[items.size()]), selected, this);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.setOnCancelListener(this);
            builder.show();
            return true;
        }

        public void setItems(List<String> items, String allText,
                             MultiSpinnerListener listener) {
            this.items = items;
            this.defaultText = allText;
            this.listener = listener;

            // all un-selected by default
            selected = new boolean[items.size()];
            for (int i = 0; i < selected.length; i++)
                selected[i] = false;

            // all text on the spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_dropdown_item, new String[]{allText});
            setAdapter(adapter);
        }

        public interface MultiSpinnerListener {
            void onItemsSelected(boolean[] selected);
        }
    }
}
