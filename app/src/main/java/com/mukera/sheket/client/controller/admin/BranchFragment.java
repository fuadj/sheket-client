package com.mukera.sheket.client.controller.admin;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.UUID;

/**
 * Created by gamma on 4/3/16.
 */
public class BranchFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private ListView mBranches;
    private BranchAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_branches, container, false);

        mBranches = (ListView) rootView.findViewById(R.id.branches_list_view);
        mAdapter = new BranchAdapter(getContext());
        mBranches.setAdapter(mAdapter);
        Button createBranchBtn = (Button) rootView.findViewById(R.id.branches_btn_create);
        createBranchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                final BranchCreateDialog dialog = new BranchCreateDialog();
                dialog.fragment = BranchFragment.this;
                dialog.show(fm, "Create Branch");
            }
        });
        getLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_LIST_LOADER, null, this);
        return rootView;
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
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    public static class BranchAdapter extends CursorAdapter {

        public static class BranchViewHolder {
            TextView branchName, loc;

            public BranchViewHolder(View view) {
                branchName = (TextView) view.findViewById(R.id.branch_list_item_branch_name);
                loc = (TextView) view.findViewById(R.id.branch_list_item_branch_loc);
                view.setTag(this);
            }
        }


        public BranchAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.list_item_branch, parent, false);
            BranchViewHolder holder = new BranchViewHolder(view);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            BranchViewHolder holder = (BranchViewHolder) view.getTag();
            SBranch branch = new SBranch(cursor);
            holder.branchName.setText(branch.branch_name);
            holder.loc.setText(branch.branch_location);
        }
    }

    public static class BranchCreateDialog extends DialogFragment {
        private EditText mBranchName, mBranchLocation;
        private Button mOkBtn, mCancelBtn;
        public BranchFragment fragment;

        void setButtonStatus() {
            mOkBtn.setEnabled(
                    !mBranchName.getText().toString().trim().isEmpty()
            );
        }
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_new_branch, container);

            mBranchName = (EditText) view.findViewById(R.id.dialog_edit_text_branch_name);
            mBranchName.addTextChangedListener(new TextWatcherAdapter(){
                @Override
                public void afterTextChanged(Editable s) {
                    setButtonStatus();
                }
            });
            mBranchLocation = (EditText) view.findViewById(R.id.dialog_edit_text_branch_location);

            mOkBtn = (Button) view.findViewById(R.id.dialog_btn_branch_ok);
            mCancelBtn = (Button) view.findViewById(R.id.dialog_btn_branch_cancel);

            mOkBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Activity activity = getActivity();
                    final String branch_name = mBranchName.getText().toString();
                    final String location = mBranchLocation.getText().toString();
                    final long company_id = PrefUtil.getCurrentCompanyId(activity);
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(BranchEntry.COLUMN_NAME, branch_name);
                            values.put(BranchEntry.COLUMN_LOCATION, location);
                            values.put(BranchEntry.COLUMN_COMPANY_ID, company_id);
                            values.put(SheketContract.UUIDSyncable.COLUMN_UUID,
                                    UUID.randomUUID().toString());
                            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                                    ChangeTraceable.CHANGE_STATUS_CREATED);

                            long branch_id = PrefUtil.getNewBranchId(activity);
                            PrefUtil.setNewBranchId(activity, branch_id);

                            values.put(BranchEntry.COLUMN_BRANCH_ID, branch_id);
                            activity.getContentResolver().insert(
                                    BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())), values);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getDialog().dismiss();
                                }
                            });
                        }
                    };
                    t.start();

                }
            });

            mCancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
                }
            });

            setButtonStatus();
            getDialog().setCanceledOnTouchOutside(false);
            return view;
        }
    }
}
