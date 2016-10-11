package com.mukera.sheket.client.controller.admin;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
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

import com.mukera.sheket.client.OperationSupport;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.data.SheketContract.BranchEntry;
import com.mukera.sheket.client.data.SheketContract.ChangeTraceable;
import com.mukera.sheket.client.data.SheketContract.MemberEntry;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SMember;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.network.AddEmployeeRequest;
import com.mukera.sheket.client.network.AddEmployeeResponse;
import com.mukera.sheket.client.network.CompanyAuth;
import com.mukera.sheket.client.network.CompanyID;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.DbUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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
                OperationSupport.checkPaymentSupportsOperation(getActivity(),
                        OperationSupport.OPERATION_ADD_EMPLOYEE,
                        new OperationSupport.OperationSupportListener() {
                            @Override
                            public void operationSupported() {
                                FragmentManager fm = getActivity().getSupportFragmentManager();
                                AddEditMemberDialog dialog = new AddEditMemberDialog();
                                dialog.setBranches(getBranches());
                                dialog.mDialogType = AddEditMemberDialog.MEMBER_DIALOG_ADD;
                                dialog.fragment = EmployeesFragment.this;
                                dialog.mMember = null;
                                dialog.show(fm, null);
                            }

                            @Override
                            public void operationNotSupported() {

                            }
                        });
            }
        });

        getLoaderManager().initLoader(LoaderId.MainActivity.MEMBER_LIST_LOADER, null, this);
        return rootView;
    }

    List<SBranch> getBranches() {
        if (mBranches == null) {
            mBranches = new ArrayList<>();
            int company_id = PrefUtil.getCurrentCompanyId(getActivity());

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

        String selection = String.format(Locale.US, "(%s != ? AND %s != ?)",

                // don't want those who've got their "deleted-flag" set
                MemberEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),

                // and we don't want users to "see" themselves, it might lead to miss-use of power(e.g: privilege-escalation)
                MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID));
        String[] selectionArgs = {
                String.valueOf(ChangeTraceable.CHANGE_STATUS_DELETED),
                String.valueOf(PrefUtil.getUserId(getContext())),
        };

        return new CursorLoader(getActivity(),
                MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SMember.MEMBER_COLUMNS,
                selection,
                selectionArgs,
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
        public void bindView(View view, final Context context, Cursor cursor) {
            MemberViewHolder holder = (MemberViewHolder) view.getTag();

            final SMember member = new SMember(cursor);

            holder.textMemberName.setText(member.member_name);
            holder.textMemberId.setText(
                    IdEncoderUtil.encodeAndDelimitId(member.member_id, IdEncoderUtil.ID_TYPE_USER)
            );
            holder.textMemberPermission.setText(SPermission.shortName(member.member_permission));
            holder.btnDeleteMember.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmMemberDeletion(context, member);
                }
            });
        }
    }

    static void confirmMemberDeletion(final Context context, final SMember member) {
        final int company_id = PrefUtil.getCurrentCompanyId(context);

        // substitute the member name in the message.
        String confirm_body = context.getString(R.string.employee_delete_confirm_body, member.member_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.employee_delete_confirm_title).
                setMessage(confirm_body).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues values = member.toContentValues();

                        // set the deleted flag to "ON", so it can be deleted on sync
                        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                                ChangeTraceable.CHANGE_STATUS_DELETED);
                        values.remove(MemberEntry.COLUMN_MEMBER_ID);

                        context.getContentResolver().
                                update(MemberEntry.buildBaseUri(company_id),
                                        values,
                                        MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID) + " = ?",
                                        new String[]{String.valueOf(member.member_id)});
                        dialog.dismiss();
                    }
                }).
                setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
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

            String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited_id);
            if (!IdEncoderUtil.isValidEncodedUserId(delimiter_removed)) {
                mBtnAddEditMember.setEnabled(false);
                return;
            } else {
                long decoded_user_id = IdEncoderUtil.decodeEncodedId(delimiter_removed, IdEncoderUtil.ID_TYPE_USER);
                // we should prohibit a user adding themselves as an employee, who
                // knows what that can do
                if (PrefUtil.getUserId(getActivity()) == decoded_user_id) {
                    mBtnAddEditMember.setEnabled(false);
                    return;
                }
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

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_edit_member, null);

            final Drawable successIcon = getResources().getDrawable(R.drawable.ic_action_success);
            successIcon.setBounds(new Rect(0, 0, successIcon.getIntrinsicWidth(), successIcon.getIntrinsicHeight()));

            final View layout_name = view.findViewById(R.id.dialog_layout_member_name);
            mEditMemberId = (EditText) view.findViewById(R.id.dialog_edit_text_member_id);

            final boolean is_edit = mDialogType == MEMBER_DIALOG_EDIT;
            String title;
            if (is_edit) {
                title = getString(R.string.placeholder_employee_action_edit_member);
                layout_name.setVisibility(View.VISIBLE);
                mMemberName = (TextView) view.findViewById(R.id.dialog_text_view_member_name);
                mMemberName.setText(mMember.member_name);
                mEditMemberId.setText(IdEncoderUtil.delimitEncodedId(IdEncoderUtil.encodeId(mMember.member_id, IdEncoderUtil.ID_TYPE_USER), 4));
                mEditMemberId.setEnabled(false);
            } else {
                title = getString(R.string.placeholder_employee_action_add_member);
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

                        long current_user_id = PrefUtil.getUserId(getActivity());
                        String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited_id);
                        if (IdEncoderUtil.isValidEncodedUserId(delimiter_removed)) {

                            long decoded_user_id = IdEncoderUtil.decodeEncodedId(delimited_id, IdEncoderUtil.ID_TYPE_USER);
                            // we should prohibit a user adding themselves as an employee, who
                            // knows what that can do
                            if (current_user_id != decoded_user_id) {
                                // I know it is weird to call {@code setError} for telling success
                                // but we don't have an API for the success.
                                mEditMemberId.setError("Correct ID", successIcon);
                            }
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
                        member.member_id = (int)IdEncoderUtil.decodeEncodedId(IdEncoderUtil.removeDelimiterOnEncodedId(id), IdEncoderUtil.ID_TYPE_USER);
                        mProgressDialog = ProgressDialog.show(getActivity(),
                                "Adding Member", "Please wait...", true);
                    } else {
                        member = new SMember(mMember);
                    }

                    List<Integer> branch_ids = new ArrayList<>();
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
            return new AlertDialog.Builder(getContext()).
                    setTitle(title).
                    setView(view).
                    create();
        }

        void updateMember(Activity activity, SMember member) {
            int company_id = PrefUtil.getCurrentCompanyId(activity);
            member.company_id = company_id;
            ContentValues values = member.toContentValues();
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_UPDATED);
            DbUtil.setUpdateOnConflict(values);

            getActivity().getContentResolver().insert(MemberEntry.buildBaseUri(company_id), values);
        }

        void addMember(Activity activity, SMember member) {
            try {
                ManagedChannel managedChannel = ManagedChannelBuilder.
                        forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                        usePlaintext(true).
                        build();

                SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                        SheketServiceGrpc.newBlockingStub(managedChannel);

                int company_id = PrefUtil.getCurrentCompanyId(getContext());
                String cookie = PrefUtil.getLoginCookie(getContext());

                AddEmployeeRequest request = AddEmployeeRequest.newBuilder().
                        setCompanyAuth(CompanyAuth.newBuilder().
                                setCompanyId(CompanyID.newBuilder().
                                        setCompanyId(company_id)).
                                setSheketAuth(SheketAuth.newBuilder().
                                        setLoginCookie(cookie))).
                        setEmployeeId(member.member_id).
                        setPermission(member.member_permission.Encode()).
                        build();
                AddEmployeeResponse response = blockingStub.addEmployee(request);

                ContentValues values = new ContentValues();
                values.put(MemberEntry.COLUMN_COMPANY_ID,
                        PrefUtil.getCurrentCompanyId(activity));
                values.put(MemberEntry.COLUMN_MEMBER_ID, member.member_id);
                values.put(MemberEntry.COLUMN_MEMBER_NAME, response.getEmployeeName());
                values.put(MemberEntry.COLUMN_MEMBER_PERMISSION, member.member_permission.Encode());

                // b/c we directly added the member, we've synced it with the server
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_SYNCED);

                Uri uri = activity.getContentResolver().
                        insert(MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())), values);
                if (MemberEntry.getMemberId(uri) < 0) {
                    throw new MemberException("error adding member into company");
                }
            } catch (StatusRuntimeException | MemberException e) {
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
}
