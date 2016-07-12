package com.mukera.sheket.client.controller.items;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.models.STransaction.*;

import java.util.List;

/**
 * Created by fuad on 7/11/16.
 */
public class TransactionSummaryDialog extends DialogFragment {
    public SummaryListener mListener;
    private ListView mListViewItems;
    private ImageButton mCancel, mBack, mFinish;

    private SummaryListAdapter mAdapter;

    private List<STransactionItem> mItemList;

    public void setTransactionItems(List<STransactionItem> transactionItems) {
        mItemList = transactionItems;
    }

    public void setListener(SummaryListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = getActivity().getLayoutInflater().
                inflate(R.layout.dialog_summary, null);

        mFinish = (ImageButton) rootView.findViewById(R.id.dialog_summary_btn_finish);
        mFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.okSelected(TransactionSummaryDialog.this, mItemList);
            }
        });

        mBack = (ImageButton) rootView.findViewById(R.id.dialog_summary_btn_back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.backSelected(TransactionSummaryDialog.this);
            }
        });
        mCancel = (ImageButton) rootView.findViewById(R.id.dialog_summary_btn_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelSelected(TransactionSummaryDialog.this);
            }
        });

        mAdapter = new SummaryListAdapter(getActivity());
        mListViewItems = (ListView) rootView.findViewById(R.id.dialog_summary_list_items);
        mListViewItems.setAdapter(mAdapter);
        mListViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.editItemAtPosition(TransactionSummaryDialog.this, mItemList, position);

                // If the user changes anything, refresh
                refreshSummaryDialog();
            }
        });

        // to start things off
        refreshSummaryDialog();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Summary").
                setCancelable(false);
        Dialog dialog = builder.setView(rootView).create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    public void refreshSummaryDialog() {
        mAdapter.clear();
        for (STransactionItem item : mItemList) {
            mAdapter.add(item);
        }
        mAdapter.notifyDataSetChanged();
    }

    public class SummaryListAdapter extends ArrayAdapter<STransactionItem> {
        private Context mContext;
        public SummaryListAdapter(Context context) {
            super(context, 0);
            mContext = context;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            STransactionItem transItem = getItem(position);

            SummaryViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.list_item_summary, parent, false);
                holder = new SummaryViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (SummaryViewHolder) convertView.getTag();
            }

            holder.btnDeleteItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.deleteItemAtPosition(TransactionSummaryDialog.this, mItemList, position);
                }
            });
            holder.textItemName.setText(transItem.item.name);
            // TODO: check source and hide the view
            holder.textQuantity.setText(Double.toString(transItem.quantity));
            return convertView;
        }

        class SummaryViewHolder {
            ImageButton btnDeleteItem;
            TextView textItemName;
            TextView textSourceInfo;
            TextView textQuantity;

            public SummaryViewHolder(View view) {
                btnDeleteItem = (ImageButton) view.findViewById(R.id.summary_img_btn_delete);
                textItemName = (TextView) view.findViewById(R.id.summary_text_view_item_name);
                textSourceInfo = (TextView) view.findViewById(R.id.summary_text_view_item_source);
                textQuantity = (TextView) view.findViewById(R.id.summary_text_view_quantity);
            }
        }
    }

    public interface SummaryListener {
        void cancelSelected(DialogFragment dialog);
        void backSelected(DialogFragment dialog);
        void editItemAtPosition(DialogFragment dialog, List<STransactionItem> itemList, int position);
        void deleteItemAtPosition(DialogFragment dialog, List<STransactionItem> itemList, int position);
        void okSelected(DialogFragment dialog, List<STransaction.STransactionItem> list);
    }
}
