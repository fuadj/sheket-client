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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mukera.sheket.client.OperationSupport;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.SheketGRPCCall;
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
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by gamma on 4/3/16.
 */
public class EmployeesFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final OkHttpClient client = new OkHttpClient();

    private ListView mMemberList;
    private MemberAdapter mAdapter;

    private List<SBranch> mBranches;
    private HashMap<Integer, SPermission.BranchAccess> mBranchAuthorities;

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
                    AddEditEmployeeDialog dialog = new AddEditEmployeeDialog();
                    dialog.setBranches(getBranches(), mBranchAuthorities);
                    dialog.mDialogType = AddEditEmployeeDialog.MEMBER_DIALOG_EDIT;
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
                                AddEditEmployeeDialog dialog = new AddEditEmployeeDialog();
                                dialog.setBranches(getBranches(), mBranchAuthorities);
                                dialog.mDialogType = AddEditEmployeeDialog.MEMBER_DIALOG_ADD;
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
        if (mBranches != null)
            return mBranches;

        mBranches = new ArrayList<>();
        mBranchAuthorities = new HashMap<>();

        int company_id = PrefUtil.getCurrentCompanyId(getActivity());

        String selection = String.format(Locale.US, "%s != %d",
                BranchEntry._full(BranchEntry.COLUMN_STATUS_FLAG),
                BranchEntry.STATUS_INVISIBLE);

        String sortOrder = BranchEntry._full(BranchEntry.COLUMN_NAME) + " ASC";
        Cursor cursor = getActivity().getContentResolver().
                query(BranchEntry.buildBaseUri(company_id),
                        SBranch.BRANCH_COLUMNS,
                        selection, null,
                        sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SBranch branch = new SBranch(cursor);
                mBranches.add(branch);
            } while (cursor.moveToNext());
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
            holder.textMemberPermission.setText(member.member_permission.toString());
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

    public static class AddEditEmployeeDialog extends DialogFragment {
        public static final int MEMBER_DIALOG_ADD = 1;
        public static final int MEMBER_DIALOG_EDIT = 2;

        public EmployeesFragment fragment;
        public int mDialogType;
        public SMember mMember;

        private EditText mEditEmployeeId;

        private TextView mEmployeeName;

        private Spinner mSpinnerPermissionType;
        private ListView mListBranches;
        private MultiSpinner mSpinnerBranches;

        private Button mBtnAddEditEmployee;

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
                    new PermType(SPermission.PERMISSION_TYPE_NONE, "--- Choose Permission ---"));
            sPermTypesHashMap.put(SPermission.PERMISSION_TYPE_OWNER,
                    new PermType(SPermission.PERMISSION_TYPE_OWNER, "Manager"));
            sPermTypesHashMap.put(SPermission.PERMISSION_TYPE_EMPLOYEE,
                    new PermType(SPermission.PERMISSION_TYPE_EMPLOYEE, "Employee"));
        }

        private HashMap<Integer, SPermission.BranchAccess> mBranchAuthorities;

        public void setBranches(List<SBranch> branches, HashMap<Integer, SPermission.BranchAccess> branchAuthorityHashMap) {
            mBranches = branches;
            mBranchAuthorities = branchAuthorityHashMap;
        }

        void setBtnState(boolean enabled) {
            mBtnAddEditEmployee.setEnabled(enabled);
            mBtnAddEditEmployee.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        }

        void setOkButtonStatus() {
            String delimited_id = mEditEmployeeId.getText().toString().trim().
                    // remove any space
                            replaceAll("\\s+", "").
                    // also remove any non-alphanumeric characters
                            replaceAll("\\W+", "");

            String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited_id);
            if (!IdEncoderUtil.isValidEncodedUserId(delimiter_removed)) {
                setBtnState(false);
                return;
            } else {
                long decoded_user_id = IdEncoderUtil.decodeEncodedId(delimiter_removed, IdEncoderUtil.ID_TYPE_USER);
                // we should prohibit a user adding themselves as an employee, who
                // knows what that can do
                if (PrefUtil.getUserId(getActivity()) == decoded_user_id) {
                    setBtnState(false);
                    return;
                }
            }

            PermType perm_type = (PermType) mSpinnerPermissionType.getSelectedItem();
            if (perm_type.type == SPermission.PERMISSION_TYPE_NONE) {
                setBtnState(false);
                return;
            }

            if (mDialogType == MEMBER_DIALOG_ADD &&
                    mEditEmployeeId.getText().toString().trim().isEmpty()) {
                setBtnState(false);
                return;
            }

            if (perm_type.type != SPermission.PERMISSION_TYPE_EMPLOYEE) {
                setBtnState(true);
                return;
            }

            if (mBranchAuthorities.isEmpty())
                setBtnState(false);
            else
                setBtnState(true);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_add_edit_member, null);

            final Drawable successIcon = getResources().getDrawable(R.drawable.ic_action_success);
            successIcon.setBounds(new Rect(0, 0, successIcon.getIntrinsicWidth(), successIcon.getIntrinsicHeight()));

            final View layout_name = view.findViewById(R.id.dialog_layout_member_name);
            mEditEmployeeId = (EditText) view.findViewById(R.id.dialog_edit_text_member_id);

            final boolean is_edit = mDialogType == MEMBER_DIALOG_EDIT;
            String title;
            if (is_edit) {
                title = getString(R.string.placeholder_employee_action_edit_member);
                layout_name.setVisibility(View.VISIBLE);
                mEmployeeName = (TextView) view.findViewById(R.id.dialog_text_view_member_name);
                mEmployeeName.setText(mMember.member_name);
                mEditEmployeeId.setText(IdEncoderUtil.delimitEncodedId(IdEncoderUtil.encodeId(mMember.member_id, IdEncoderUtil.ID_TYPE_USER), 4));
                mEditEmployeeId.setEnabled(false);
            } else {
                title = getString(R.string.placeholder_employee_action_add_member);
                layout_name.setVisibility(View.GONE);
                mEditEmployeeId.setEnabled(true);
                mEditEmployeeId.addTextChangedListener(new TextWatcherAdapter() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        setOkButtonStatus();

                        String delimited_id = mEditEmployeeId.getText().toString().trim().
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
                                mEditEmployeeId.setError("Correct ID", successIcon);
                            }
                        } else {
                            mEditEmployeeId.setError(null);
                        }
                    }
                });
            }

            mLayoutBranchSelector = (LinearLayout) view.findViewById(R.id.dialog_member_layout_permission_branch);

            List<PermType> types = new ArrayList<>();
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_NONE));
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_OWNER));
            types.add(sPermTypesHashMap.get(SPermission.PERMISSION_TYPE_EMPLOYEE));

            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, types.toArray());
            mSpinnerPermissionType = (Spinner) view.findViewById(R.id.dialog_member_spinner_permission_type);
            mSpinnerPermissionType.setAdapter(adapter);
            mSpinnerPermissionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    PermType type = (PermType) parent.getAdapter().getItem(position);
                    if (type.type == SPermission.PERMISSION_TYPE_EMPLOYEE) {
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

            mListBranches = (ListView) view.findViewById(R.id.dialog_member_list_branches);

            BranchAuthoritiesAdapter branchesAdapter = new BranchAuthoritiesAdapter(getContext());
            branchesAdapter.setBranchAuthorities(mBranchAuthorities);
            branchesAdapter.setDialogReference(this);
            SBranch headerRow = new SBranch();
            headerRow.branch_id = -1;
            branchesAdapter.add(headerRow);
            for (SBranch branch : mBranches) {
                branchesAdapter.add(branch);
            }
            mListBranches.setAdapter(branchesAdapter);

            mBtnAddEditEmployee = (Button) view.findViewById(R.id.dialog_btn_member_add_edit);
            mBtnAddEditEmployee.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Activity activity = getActivity();

                    final SMember member;
                    if (mDialogType == MEMBER_DIALOG_ADD) {
                        member = new SMember();
                        String id = mEditEmployeeId.getText().toString().trim();
                        member.member_id = (int) IdEncoderUtil.decodeEncodedId(IdEncoderUtil.removeDelimiterOnEncodedId(id), IdEncoderUtil.ID_TYPE_USER);
                        mProgressDialog = ProgressDialog.show(getActivity(),
                                "Adding Member", "Please wait...", true);
                    } else {
                        member = new SMember(mMember);
                    }

                    List<SPermission.BranchAccess> allowedBranches = new ArrayList<>();
                    for (SPermission.BranchAccess access : mBranchAuthorities.values()) {
                        access.encodeAccess();
                        allowedBranches.add(access);
                    }

                    SPermission permission = new SPermission();
                    PermType permType = (PermType) mSpinnerPermissionType.getSelectedItem();
                    permission.setPermissionType(permType.type);
                    permission.setAllowedBranches(allowedBranches);

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
                                                    setTitle("Employee Adding Error").
                                                    setMessage(mErrorMsg).show();
                                        } else {
                                            new AlertDialog.Builder(getActivity()).
                                                    setTitle("Success").
                                                    setMessage("Employee Added").show();
                                        }
                                    }
                                }
                            });
                        }
                    };
                    t.start();
                }
            });
            Button btnCancel;
            btnCancel = (Button) view.findViewById(R.id.dialog_btn_member_cancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
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
                int company_id = PrefUtil.getCurrentCompanyId(getContext());
                String cookie = PrefUtil.getLoginCookie(getContext());

                final AddEmployeeRequest request = AddEmployeeRequest.newBuilder().
                        setCompanyAuth(CompanyAuth.newBuilder().
                                setCompanyId(CompanyID.newBuilder().
                                        setCompanyId(company_id)).
                                setSheketAuth(SheketAuth.newBuilder().
                                        setLoginCookie(cookie))).
                        setEmployeeId(member.member_id).
                        setPermission(member.member_permission.Encode()).
                        build();

                AddEmployeeResponse response = new SheketGRPCCall<AddEmployeeResponse>().runBlockingCall(
                        new SheketGRPCCall.GRPCCallable<AddEmployeeResponse>() {
                            @Override
                            public AddEmployeeResponse runGRPCCall() throws Exception {
                                ManagedChannel managedChannel = ManagedChannelBuilder.
                                        forAddress(ConfigData.getServerIP(getActivity()), ConfigData.getServerPort()).
                                        usePlaintext(true).
                                        build();

                                SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                                        SheketServiceGrpc.newBlockingStub(managedChannel);
                                return blockingStub.addEmployee(request);
                            }
                        }
                );

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
            } catch (SheketGRPCCall.SheketException | MemberException e) {
                mErrorOccurred = true;
                mErrorMsg = e.getMessage();
            }
        }
    }

    static class BranchAuthoritiesAdapter extends ArrayAdapter<SBranch> {
        private HashMap<Integer, SPermission.BranchAccess> mBranchAuthorities;
        private AddEditEmployeeDialog mDialog;

        public BranchAuthoritiesAdapter(Context context) {
            super(context, 0);
        }

        public void setBranchAuthorities(HashMap<Integer, SPermission.BranchAccess> authorities) {
            mBranchAuthorities = authorities;
        }

        public void setDialogReference(AddEditEmployeeDialog dialog) {
            mDialog = dialog;
        }

        public static boolean isHeaderRow(int position) {
            return position == 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final EmployeeViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_add_employee, parent, false);
                holder = new EmployeeViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (EmployeeViewHolder) convertView.getTag();
            }

            int hide_if_header = isHeaderRow(position) ? View.GONE : View.VISIBLE;
            int show_if_header = isHeaderRow(position) ? View.VISIBLE : View.GONE;

            /**
             * Reset listeners!!! It will create un-expected behaviour otherwise.
             */
            holder.checkBranch.setOnCheckedChangeListener(null);
            holder.checkSeeQty.setOnCheckedChangeListener(null);
            holder.checkBuyItem.setOnCheckedChangeListener(null);

            holder.layoutSeeQty.setOnClickListener(null);
            holder.layoutBuyItem.setOnClickListener(null);

            holder.checkBranch.setVisibility(hide_if_header);
            holder.checkSeeQty.setVisibility(hide_if_header);
            holder.checkBuyItem.setVisibility(hide_if_header);

            holder.imgSeeQty.setVisibility(show_if_header);
            holder.imgBuyItem.setVisibility(show_if_header);

            if (isHeaderRow(position)) {
                holder.branchName.setText("Choose Branches");
                return convertView;
            }

            final SBranch branch = getItem(position);

            holder.branchName.setText(branch.branch_name);

            holder.checkBranch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!isChecked) {
                        mBranchAuthorities.remove(branch.branch_id);
                        // Toggle-off branch related options
                        holder.checkSeeQty.setChecked(false);
                        holder.checkBuyItem.setChecked(false);
                    } else {
                        mBranchAuthorities.put(branch.branch_id, new SPermission.BranchAccess(branch.branch_id));
                    }
                    mDialog.setOkButtonStatus();
                }
            });
            // When the row is selected, it should behave as-if the branch is selected
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.checkBranch.setChecked(!holder.checkBranch.isChecked());
                }
            });

            holder.checkSeeQty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mBranchAuthorities.containsKey(branch.branch_id)) {
                        mBranchAuthorities.get(branch.branch_id).show_qty = isChecked;
                    } else if (isChecked) {
                        // only bother to add the branch authority if it is checked.

                        if (!mBranchAuthorities.containsKey(branch.branch_id)) {
                            // this will call checkBranch's listener.
                            // Which will cause a branchAuthority to be added for the key
                            holder.checkBranch.setChecked(true);
                        }

                        mBranchAuthorities.get(branch.branch_id).show_qty = isChecked;
                    }
                    mDialog.setOkButtonStatus();
                }
            });
            holder.layoutSeeQty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.checkSeeQty.setChecked(!holder.checkSeeQty.isChecked());
                }
            });

            holder.checkBuyItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mBranchAuthorities.containsKey(branch.branch_id)) {
                        mBranchAuthorities.get(branch.branch_id).buy_items = isChecked;
                    } else if (isChecked) {
                        // only bother to add the branch authority if it is checked.

                        if (!mBranchAuthorities.containsKey(branch.branch_id)) {
                            // this will call checkBranch's listener.
                            // Which will cause a branchAuthority to be added for the key
                            holder.checkBranch.setChecked(true);
                        }

                        mBranchAuthorities.get(branch.branch_id).buy_items = isChecked;
                    }
                    mDialog.setOkButtonStatus();
                }
            });
            holder.layoutBuyItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.checkBuyItem.setChecked(!holder.checkBuyItem.isChecked());
                }
            });

            return convertView;
        }

        static class EmployeeViewHolder {
            CheckBox checkBranch;

            TextView branchName;

            View layoutSeeQty;
            View layoutBuyItem;

            ImageView imgSeeQty;
            ImageView imgBuyItem;

            CheckBox checkSeeQty;
            CheckBox checkBuyItem;

            public EmployeeViewHolder(View view) {
                checkBranch = (CheckBox) view.findViewById(R.id.list_item_add_employee_chk_choose_branch);
                branchName = (TextView) view.findViewById(R.id.list_item_add_employee_text_branch_name);

                layoutSeeQty = view.findViewById(R.id.list_item_add_employee_layout_see_qty);
                layoutBuyItem = view.findViewById(R.id.list_item_add_employee_layout_buy_item);

                imgSeeQty = (ImageView) view.findViewById(R.id.list_item_add_employee_img_see_qty);
                imgBuyItem = (ImageView) view.findViewById(R.id.list_item_add_employee_img_buy_item);

                checkSeeQty = (CheckBox) view.findViewById(R.id.list_item_add_employee_chk_see_qty);
                checkBuyItem = (CheckBox) view.findViewById(R.id.list_item_add_employee_chk_buy_item);
            }
        }
    }

    static class MemberException extends Exception {
        public MemberException(String detailMessage) {
            super(detailMessage);
        }
    }
}
