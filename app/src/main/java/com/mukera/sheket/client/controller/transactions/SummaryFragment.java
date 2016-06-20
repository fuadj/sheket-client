package com.mukera.sheket.client.controller.transactions;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.STransaction.STransactionItem;

import java.util.List;

/**
 * Created by gamma on 3/5/16.
 */
public class SummaryFragment extends Fragment {
    public SummaryListener mListener;
    private ListView mListViewItems;
    private Button mCancel, mBack, mOk;

    private SummaryListAdapter mAdapter;

    // the data used is stored in this public member,
    public List<STransactionItem> mItemList;

    public void setListener(SummaryListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_summary, container, false);

        mOk = (Button) rootView.findViewById(R.id.summary_btn_ok);
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.okSelected(mItemList);
            }
        });

        mBack = (Button) rootView.findViewById(R.id.summary_btn_back);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.backSelected();
            }
        });
        mCancel = (Button) rootView.findViewById(R.id.summary_btn_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelSelected();
            }
        });

        mAdapter = new SummaryListAdapter(getContext());
        mListViewItems = (ListView) rootView.findViewById(R.id.summary_list_view);
        mListViewItems.setAdapter(mAdapter);
        mListViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.editItemAtPosition(mItemList, position);

                // If the user changes anything, refresh
                refreshAdapter();
            }
        });

        // to start things off
        refreshAdapter();

        return rootView;
    }

    public void refreshAdapter() {
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
                    mListener.deleteItemAtPosition(mItemList, position);
                    SummaryFragment.this.refreshAdapter();
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
        void cancelSelected();
        void backSelected();
        void editItemAtPosition(List<STransactionItem> itemList, int position);
        void deleteItemAtPosition(List<STransactionItem> itemList, int position);
        void okSelected(List<STransactionItem> list);
    }
}
