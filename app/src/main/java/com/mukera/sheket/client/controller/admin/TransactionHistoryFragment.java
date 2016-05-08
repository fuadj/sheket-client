package com.mukera.sheket.client.controller.admin;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.widget.*;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.NumberFormatter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private ListView mTransList;
    private TransDetailAdapter mTransDetailAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.TRANSACTION_HISTORY_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_trans_history, container, false);

        mTransList = (ListView) rootView.findViewById(R.id.list_view_trans_history);
        mTransDetailAdapter = new TransDetailAdapter(getContext());
        mTransList.setAdapter(mTransDetailAdapter);
        mTransList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                STransactionDetail detail = mTransDetailAdapter.getItem(position);
                TransDetailDialog dialog = new TransDetailDialog();
                dialog.mTransDetail = detail;
                dialog.show(fm, "Detail");
            }
        });

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = TransactionEntry._full(TransactionEntry.COLUMN_TRANS_ID) + " ASC";
        return new CursorLoader(getActivity(),
                TransItemEntry.buildTransactionItemsUri(
                        PrefUtil.getCurrentCompanyId(getContext()),
                        TransItemEntry.NO_TRANS_ID_SET),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS,
                null, null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTransDetailAdapter.setTransCursor(data);
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

    public static class TransDetailAdapter extends ArrayAdapter<STransactionDetail> {
        public TransDetailAdapter(Context context) {
            super(context, 0);
        }

        public void setTransCursor(Cursor cursor) {
            setNotifyOnChange(false);
            clear();

            if (cursor != null && cursor.moveToFirst()) {
                long prev_trans_id = -1;
                STransactionDetail detail = null;
                do {
                    long trans_id = cursor.getLong(STransaction.COL_TRANS_ID);
                    if (trans_id != prev_trans_id) {
                        if (prev_trans_id != -1) {
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
            STransactionDetail detail = getItem(position);

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
                    detail.is_buying ? R.mipmap.ic_action_download : R.mipmap.ic_action_refresh);
            holder.username.setText("Username " + position * 14);
            holder.total_qty.setText(NumberFormatter.formatDoubleForDisplay(detail.total_quantity));
            return convertView;
        }

        private static class TransDetailViewHolder {
            ImageView trans_icon;
            TextView username;
            TextView total_qty;

            public TransDetailViewHolder(View view) {
                trans_icon = (ImageView) view.findViewById(R.id.list_item_trans_history_icon);
                username = (TextView) view.findViewById(R.id.list_item_trans_history_user_name);
                total_qty = (TextView) view.findViewById(R.id.list_item_trans_history_qty);
            }
        }
    }

    public static class TransDetailDialog extends DialogFragment {
        public STransactionDetail mTransDetail;

        public TransDetailDialog() {
            super();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialog_trans_history, container);

            TextView user_name = (TextView) view.findViewById(R.id.dialog_trans_history_user_name);

            View layout = view.findViewById(R.id.dialog_trans_history_layout_details);
            if (!mTransDetail.affected_items.isEmpty()) {
                layout.setVisibility(View.VISIBLE);

                ListView itemList = (ListView) layout.findViewById(R.id.dialog_trans_history_list);
                TransDetailAdapter adapter = new TransDetailAdapter(getContext());
                for (STransactionItem transItem : mTransDetail.affected_items) {
                    adapter.add(transItem);
                }
                itemList.setAdapter(adapter);
            } else {
                layout.setVisibility(View.GONE);
            }

            TextView total_qty = (TextView) view.findViewById(R.id.dialog_trans_history_total_qty);
            total_qty.setText("Total Qty: " + NumberFormatter.formatDoubleForDisplay(mTransDetail.total_quantity));
            return view;
        }

        public static class TransDetailAdapter extends ArrayAdapter<STransactionItem> {
            public TransDetailAdapter(Context context) {
                super(context, 0);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                STransactionItem transItem = getItem(position);

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.list_item_trans_history_dialog, parent, false);
                }

                TextView itemName, sourceDetail, qty;

                itemName = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_item_name);
                sourceDetail = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_source_detail);
                qty = (TextView) convertView.findViewById(R.id.list_item_trans_history_dialog_qty);

                itemName.setText(transItem.item.name);
                sourceDetail.setText(TransItemEntry.getStringForm(transItem.trans_type));
                qty.setText(NumberFormatter.formatDoubleForDisplay(transItem.quantity));
                return convertView;
            }
        }
    }
}
