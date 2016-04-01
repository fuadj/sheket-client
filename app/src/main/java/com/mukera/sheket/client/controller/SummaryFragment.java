package com.mukera.sheket.client.controller;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.STransaction;

import java.util.List;

/**
 * Created by gamma on 3/5/16.
 */
public class SummaryFragment extends Fragment {
    public SummaryListener mListener;
    public List<STransaction.STransactionItem> mItems;
    private TextView mHeaderLabel;
    private ListView mItemList;
    private Button mCancel, mOk;

    public void setListener(SummaryListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_summary, container, false);

        mHeaderLabel = (TextView) rootView.findViewById(R.id.summary_header_label);
        String header = String.format("%d item%s selected.", mItems.size(),
                (mItems.size() == 1 ? "" : "s"));
        mHeaderLabel.setText(header);
        mOk = (Button) rootView.findViewById(R.id.summary_btn_ok);
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.okSelected(mItems);
            }
        });

        mCancel = (Button) rootView.findViewById(R.id.summary_btn_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.cancelSelected();
            }
        });
        mItemList = (ListView) rootView.findViewById(R.id.summary_list_view);
        ArrayAdapter<STransaction.STransactionItem> adapter = new
                ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mItems);
        mItemList.setAdapter(adapter);

        return rootView;
    }

    public interface SummaryListener {
        void cancelSelected();
        void okSelected(List<STransaction.STransactionItem> list);
    }
}
