package com.mukera.sheket.client.controller;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.mukera.sheket.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/31/16.
 */
public class SourceSelectDialog extends DialogFragment {
    private List<Source> mSourceList;
    private Spinner mSpinner;
    private SourceSelectListener mListener;

    public SourceSelectDialog(List<Source> sources) {
        super();
        mSourceList = new ArrayList<>(sources);
    }

    public void setListener(SourceSelectListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_source, container);

        ImageButton cancelBtn, finishBtn;
        cancelBtn = (ImageButton) view.findViewById(R.id.dialog_btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.dialogCancel();
            }
        });
        finishBtn = (ImageButton) view.findViewById(R.id.dialog_btn_done);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Source source = mSourceList.get(mSpinner.getSelectedItemPosition());
                if (mListener != null) {
                    mListener.dialogOk(source);
                }
            }
        });

        List<String> sourceNames = new ArrayList<>();
        for (Source source : mSourceList) {
            sourceNames.add(source.sourceName);
        }

        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, sourceNames.toArray());

        mSpinner = (Spinner) view.findViewById(R.id.dialog_spinner_source);
        mSpinner.setAdapter(adapter);

        getDialog().setCanceledOnTouchOutside(false);
        return view;
    }

    public interface SourceSelectListener {
        void dialogCancel();

        void dialogOk(Source source);
    }

    public static class Source {
        public String sourceName;
        public int sourceId;
        public int sourceType;
    }
}
