package com.mukera.sheket.client.controller.items;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.models.SBranchItem;

/**
 * Created by gamma on 4/22/16.
 */
public class ItemLocationDialog extends DialogFragment {
    public interface ItemLocationListener {
        void cancelSelected();
        void locationSelected(String location);
    }

    private ItemLocationListener mListener;
    private SBranchItem mBranchItem = null;

    public void setListener(ItemLocationListener listener) {
        mListener = listener;
    }

    public void setBranchItem(SBranchItem branchItem) {
        mBranchItem = branchItem;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Set Item Location");
        if (mBranchItem != null)
            builder.setMessage(mBranchItem.item.name);

        final EditText editLocation = new EditText(getContext());
        editLocation.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        if (!TextUtils.isEmpty(mBranchItem.item_location))
            editLocation.setText(mBranchItem.item_location);
        builder.setView(editLocation);

        builder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(editLocation.getApplicationWindowToken(), 0);
                        if (mListener != null) {
                            mListener.locationSelected(editLocation.getText().toString().trim());
                        }
                    }
                });

        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.cancelSelected();
                        }
                    }
                });

        final AlertDialog dialog = builder.create();
        editLocation.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                /*
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
                        !TextUtils.isEmpty(s)
                );
                */
            }
        });
        return dialog;
    }
}
