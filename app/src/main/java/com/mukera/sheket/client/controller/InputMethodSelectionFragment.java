package com.mukera.sheket.client.controller;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 3/4/16.
 */
public class InputMethodSelectionFragment extends Fragment {
    public static final int SELECTION_NONE = 0;
    public static final int SELECTION_BARCODE = 1;
    public static final int SELECTION_MANUAL = 2;

    private InputMethodSelectionListener mListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setSelectionMethodListener(InputMethodSelectionListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_selection, container, false);

        ImageButton scannerBtn = (ImageButton) rootView.findViewById(R.id.selection_btn_scanner);
        scannerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.inputMethodSelected(SELECTION_BARCODE);
            }
        });
        ImageButton manualBtn = (ImageButton) rootView.findViewById(R.id.selection_btn_manual);
        manualBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null)
                    mListener.inputMethodSelected(SELECTION_MANUAL);
            }
        });

        return rootView;
    }


    public interface InputMethodSelectionListener {
        void inputMethodSelected(int method);
    }
}
