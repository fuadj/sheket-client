package com.mukera.sheket.client.controller;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/5/16.
 */
public class QtyDialog extends DialogFragment {
    public SItem item;
    private ItemQtyListener mListener;
    private EditText mQtyEdit;

    public QtyDialog() {
        super();
    }

    String formatDouble(double d) {
        return String.valueOf(d);
    }

    public void setListener(ItemQtyListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_qty, container);

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
                mListener.dialogOk(item, Double.valueOf(mQtyEdit.getText().toString()));
            }
        });

        Button changeSource = (Button) view.findViewById(R.id.dialog_btn_change_source);
        changeSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<SourceSelectDialog.Source> sources = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    SourceSelectDialog.Source source = new SourceSelectDialog.Source();
                    source.sourceName = "Name " + i;
                    source.sourceId = i;
                    sources.add(source);
                }
                final SourceSelectDialog dialog = new SourceSelectDialog(sources);
                dialog.setListener(new SourceSelectDialog.SourceSelectListener() {
                    @Override
                    public void dialogCancel() {
                        dialog.dismiss();
                    }

                    @Override
                    public void dialogOk(SourceSelectDialog.Source source) {
                        Toast.makeText(getContext(), source.sourceName,
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });
            }
        });
        mQtyEdit = (EditText) view.findViewById(R.id.dialog_edit_text_dialog_qty);
        TextView name = (TextView) view.findViewById(R.id.dialog_text_view_item_name);
        name.setText(item.name);

        //TextView qty = (TextView) view.findViewById(R.id.dialog_text_view_item_qty);
        //qty.setText(formatDouble(item.qty_remain));

        getDialog().setCanceledOnTouchOutside(false);

        return view;
    }

    public interface ItemQtyListener {
        void dialogCancel();
        void dialogOk(SItem item, double qty);
    }
}

