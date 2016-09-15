package com.mukera.sheket.client.controller.admin;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.widget.*;

import com.mukera.sheket.client.controller.items.transactions.TransactionUtil;
import com.mukera.sheket.client.models.SMember;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionHistoryFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private ListView mTransList;
    private TransDetailAdapter mTransDetailAdapter;
    private Map<Long, SMember> mMembers = null;

    /**
     * Because we are fetching both un-synced & synced(only if a manager) transactions,
     * we need to have 2 "types" of cursor data. We could implement this inside ContentProvider
     * but that would be an overkill. We would either need to implement a query that can
     * fetch data of both types, that would be a very complex query. Or use {@code MergeCursor}
     * at the ContentProvider side. But we also want the access control(only managers seeing history)
     * option to be "easily configurable" from here and not deep down in the content provider.
     * <p/>
     * So we hold on these 2 cursors of both types and use {@code MergeCursor} to join them
     * with the un-synced being on the top.
     */
    private Cursor mUn_syncedTransCursor = null;
    private Cursor mSyncedTransCursor = null;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        startLoader();
        super.onActivityCreated(savedInstanceState);
    }

    boolean hasUserManagerAccess() {
        return SPermission.getUserPermission(getActivity()).hasManagerAccess();
    }

    void startLoader() {
        getLoaderManager().initLoader(LoaderId.MainActivity.UN_SYNCED_TRANSACTION_HISTORY_LOADER, null, this);

        if (hasUserManagerAccess())
            getLoaderManager().initLoader(LoaderId.MainActivity.SYNCED_TRANSACTION_HISTORY_LOADER, null, this);
    }

    void restartLoader() {
        getLoaderManager().restartLoader(LoaderId.MainActivity.UN_SYNCED_TRANSACTION_HISTORY_LOADER, null, this);

        if (hasUserManagerAccess())
            getLoaderManager().restartLoader(LoaderId.MainActivity.SYNCED_TRANSACTION_HISTORY_LOADER, null, this);
    }

    boolean isUnSyncedTransaction(STransactionDetail transactionDetail) {
        return transactionDetail.trans.transaction_id < 0;
    }

    protected boolean displayUserName(STransactionDetail transactionDetail) {
        return !isUnSyncedTransaction(transactionDetail);
    }

    protected boolean displayDeleteButton(STransactionDetail transactionDetail) {
        return isUnSyncedTransaction(transactionDetail);
    }

    Map<Long, SMember> getMembers() {
        if (mMembers == null) {
            mMembers = new HashMap<>();

            long company_id = PrefUtil.getCurrentCompanyId(getActivity());

            String sortOrder = MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID);
            Cursor cursor = getActivity().getContentResolver().query(MemberEntry.buildBaseUri(company_id),
                    SMember.MEMBER_COLUMNS, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                mMembers = new HashMap<>();
                do {
                    SMember member = new SMember(cursor);
                    mMembers.put(member.member_id, member);
                } while (cursor.moveToNext());
            }
            if (cursor != null) {
                cursor.close();
            }

            // Add self to the map
            SMember self = new SMember();
            self.member_id = PrefUtil.getUserId(getActivity());
            self.member_name = PrefUtil.getUsername(getActivity());

            mMembers.put(self.member_id, self);
        }
        return mMembers;
    }

    String getMemberName(long member_id) {
        String username;
        if (getMembers().containsKey(member_id)) {
            username = getMembers().get(member_id).member_name;
        } else {
            // If it doesn't exist in member list, it must be the boss
            username = "Owner";
        }
        return username;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_trans_history, container, false);

        mTransList = (ListView) rootView.findViewById(R.id.list_view_trans_history);
        mTransDetailAdapter = new TransDetailAdapter(getContext());
        mTransDetailAdapter.setListener(new TransactionDeleteListener() {
            @Override
            public void deleteTransaction(final STransactionDetail transactionDetail) {
                final Activity activity = getActivity();
                new AlertDialog.Builder(getContext()).
                        setTitle("Delete Transaction").
                        setMessage("This will undo the transaction, Are You Sure?").
                        setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Thread t = new Thread() {
                                    @Override
                                    public void run() {
                                        TransactionUtil.reverseTransactionWithItems(activity,
                                                transactionDetail.trans,
                                                transactionDetail.affected_items);
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                // this will make the view be the correct one after a deletion
                                                restartLoader();
                                            }
                                        });
                                    }
                                };
                                t.start();
                            }
                        }).setNegativeButton("No", null).show();
            }
        });
        mTransList.setAdapter(mTransDetailAdapter);
        mTransList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                STransactionDetail detail = mTransDetailAdapter.getItem(position);
                TransDetailDialog dialog = new TransDetailDialog();
                boolean display = displayUserName(detail);
                dialog.setDisplayUsername(display);
                if (display)
                    dialog.setTransUsername(getMemberName(detail.trans.user_id));
                dialog.mTransDetail = detail;
                dialog.show(fm, "Detail");
            }
        });

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = "", selection = "";

        String column_trans_id = TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID);

        if (id == LoaderId.MainActivity.UN_SYNCED_TRANSACTION_HISTORY_LOADER) {
            /**
             * since the un-synced have a decreasing -ve ids, we are basically going down
             * the number line. So, newer transactions have a more -ve value. To make
             * the more recent's appear on the top, we should sort in ascending order (i.e:
             * from the more negative to the less negative).
             */
            sortOrder = column_trans_id + " ASC";
            selection = column_trans_id + " < 0";
        } else if (id == LoaderId.MainActivity.SYNCED_TRANSACTION_HISTORY_LOADER) {
            sortOrder = column_trans_id + " DESC";
            selection = column_trans_id + " > 0";
        }

        return new CursorLoader(getActivity(),
                TransItemEntry.buildTransactionItemsUri(
                        PrefUtil.getCurrentCompanyId(getContext()),
                        TransItemEntry.NO_TRANS_ID_SET),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS,
                selection,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LoaderId.MainActivity.UN_SYNCED_TRANSACTION_HISTORY_LOADER:
                mUn_syncedTransCursor = data;
                break;
            case LoaderId.MainActivity.SYNCED_TRANSACTION_HISTORY_LOADER:
                mSyncedTransCursor = data;
                break;
        }

        // the un-synced should necessarily finish loading
        if (mUn_syncedTransCursor != null) {
            // the synced list hasn't loaded and the user has the privileges, wait for them
            if (mSyncedTransCursor == null &&
                    hasUserManagerAccess()) {
                return;
            }

            // if we get here, we can show the data.
            // NOTE: a null cursor is valid for {@code MergeCursor}
            mTransDetailAdapter.setTransCursor(new MergeCursor(new Cursor[]{
                    // the un-synced at the top
                    mUn_syncedTransCursor,
                    mSyncedTransCursor
            }));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTransDetailAdapter.setTransCursor(null);
    }

    static class STransactionDetail {
        public STransaction trans;
        public double total_quantity;
        public boolean is_buying;
        public List<STransactionItem> affected_items;
    }

    public interface TransactionDeleteListener {
        void deleteTransaction(STransactionDetail transactionDetail);
    }

    public class TransDetailAdapter extends ArrayAdapter<STransactionDetail> {
        private TransactionDeleteListener mListener;

        public void setListener(TransactionDeleteListener listener) {
            mListener = listener;
        }

        public TransDetailAdapter(Context context) {
            super(context, 0);
        }

        public void setTransCursor(Cursor cursor) {
            setNotifyOnChange(false);
            clear();

            if (cursor != null && cursor.moveToFirst()) {
                // 0 is the only valid sentinel for both un-synced and synced transactions
                long invalid_trans_id = 0;

                long prev_trans_id = invalid_trans_id;
                STransactionDetail detail = null;
                do {
                    long trans_id = cursor.getLong(STransaction.COL_TRANS_ID);
                    if (trans_id != prev_trans_id) {
                        if (prev_trans_id != invalid_trans_id) {
                            super.add(detail);
                        }
                        prev_trans_id = trans_id;

                        detail = new STransactionDetail();
                        detail.trans = new STransaction(cursor);
                        detail.total_quantity = 0;
                        detail.affected_items = new ArrayList<>();
                    }

                    STransactionItem transItem = new STransactionItem(cursor,
                            STransaction.TRANSACTION_COLUMNS.length, true);
                    detail.is_buying = TransItemEntry.isIncrease(transItem.trans_type);
                    detail.affected_items.add(transItem);
                    detail.total_quantity += transItem.quantity;
                } while (cursor.moveToNext());
                if (detail != null)
                    super.add(detail);
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final STransactionDetail detail = getItem(position);

            TransDetailViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_trans_history, parent, false);
                holder = new TransDetailViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TransDetailViewHolder) convertView.getTag();
            }

            holder.trans_icon.setImageResource(
                    detail.is_buying ? R.drawable.ic_action_add_dark : R.drawable.ic_action_minus_dark);
            if (displayUserName(detail)) {
                holder.username.setVisibility(View.VISIBLE);
                holder.username.setText(getMemberName(detail.trans.user_id));
            } else {
                holder.username.setVisibility(View.GONE);
            }

            String note = detail.trans.transactionNote;
            /**
             * If the transaction note is empty and we only have 1 item
             * in the transaction, display the item's note as the
             * transaction note.
             */
            if (TextUtils.isEmpty(note) &&
                    (detail.affected_items.size() == 1)) {
                note = detail.affected_items.get(0).item_note;
            }

            // If after all that, it is still empty, hide it.
            if (TextUtils.isEmpty(note)) {
                holder.transNote.setVisibility(View.GONE);
            } else {
                holder.transNote.setVisibility(View.VISIBLE);
                holder.transNote.setText(note);
            }

            holder.total_qty.setText(Utils.formatDoubleForDisplay(detail.total_quantity));
            holder.date.setText(detail.trans.decodedDate);
            if (displayDeleteButton(detail)) {
                holder.deleteTrans.setVisibility(View.VISIBLE);
                if (mListener != null) {
                    holder.deleteTrans.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.deleteTransaction(detail);
                        }
                    });
                }
            } else {
                holder.deleteTrans.setVisibility(View.GONE);
            }

            return convertView;
        }

        private class TransDetailViewHolder {
            ImageView deleteTrans;
            ImageView trans_icon;
            TextView username;
            TextView transNote;
            TextView total_qty;
            TextView date;

            public TransDetailViewHolder(View view) {
                deleteTrans = (ImageView) view.findViewById(R.id.list_item_trans_history_delete);
                trans_icon = (ImageView) view.findViewById(R.id.list_item_trans_history_icon);
                username = (TextView) view.findViewById(R.id.list_item_trans_history_user_name);
                transNote = (TextView) view.findViewById(R.id.list_item_trans_history_trans_note);
                total_qty = (TextView) view.findViewById(R.id.list_item_trans_history_qty);
                date = (TextView) view.findViewById(R.id.list_item_trans_history_date);
            }
        }
    }

    public static class TransDetailDialog extends DialogFragment {
        public STransactionDetail mTransDetail;
        private boolean mDisplayUsername = true;
        private String mUsername;

        public TransDetailDialog() {
            super();
        }

        public void setDisplayUsername(boolean display_username) {
            mDisplayUsername = display_username;
        }

        public void setTransUsername(String username) {
            mUsername = username;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_trans_history, null);

            View layout = view.findViewById(R.id.dialog_trans_history_layout_details);
            if (!mTransDetail.affected_items.isEmpty()) {
                layout.setVisibility(View.VISIBLE);

                ListView itemList = (ListView) layout.findViewById(R.id.dialog_trans_history_list);
                TransDetailDialogAdapter adapter = new TransDetailDialogAdapter(getContext());
                for (STransactionItem transItem : mTransDetail.affected_items) {
                    adapter.add(transItem);
                }
                itemList.setAdapter(adapter);
            } else {
                layout.setVisibility(View.GONE);
            }

            TextView total_qty = (TextView) view.findViewById(R.id.dialog_trans_history_total_qty);
            total_qty.setText(
                    getString(R.string.placeholder_trans_history_total_qty, Utils.formatDoubleForDisplay(mTransDetail.total_quantity)));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(view);
            if (mDisplayUsername)
                builder.setTitle(mUsername);
            return builder.create();
        }

        public class TransDetailDialogAdapter extends ArrayAdapter<STransactionItem> {
            public TransDetailDialogAdapter(Context context) {
                super(context, 0);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                STransactionItem transItem = getItem(position);

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.list_item_trans_history_dialog, parent, false);
                }

                TextView itemName, sourceDetail, qty, itemNote;

                itemName = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_item_name);
                sourceDetail = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_source_detail);
                qty = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_qty);
                itemNote = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_item_note);

                itemName.setText(transItem.item.name);
                sourceDetail.setText(TransItemEntry.getStringForm(transItem.trans_type));
                qty.setText(Utils.formatDoubleForDisplay(transItem.quantity));
                if (!TextUtils.isEmpty(transItem.item_note)) {
                    itemNote.setVisibility(View.VISIBLE);
                    itemNote.setText(transItem.item_note);
                } else {
                    itemNote.setVisibility(View.GONE);
                }
                return convertView;
            }
        }
    }
}
