package com.mukera.sheket.client.b_actions;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public class QtyDialog extends DialogFragment {
    public SItem item;
    public QtyDialog() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_qty, container);

        ImageButton cancelBtn, undoBtn, finishBtn, continueBtn;
        cancelBtn = (ImageButton) view.findViewById(R.id.dialog_btn_cancel);
        undoBtn = (ImageButton) view.findViewById(R.id.dialog_btn_back);
        finishBtn = (ImageButton) view.findViewById(R.id.dialog_btn_done);
        continueBtn = (ImageButton) view.findViewById(R.id.dialog_btn_continue);

        TextView name = (TextView) view.findViewById(R.id.dialog_text_view_item_name);
        name.setText(item.name);

        TextView qty = (TextView) view.findViewById(R.id.dialog_text_view_item_qty);
        qty.setText("" + item.qty_remain);

        return view;
    }
}

