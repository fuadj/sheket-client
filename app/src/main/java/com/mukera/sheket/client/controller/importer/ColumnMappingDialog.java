package com.mukera.sheket.client.controller.importer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.mukera.sheket.client.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by fuad on 6/9/16.
 */
public class ColumnMappingDialog extends DialogFragment {
    public interface OnClickListener {
        void onColumnMappingDone(SimpleCSVReader reader, Map<Integer, Integer> columnMapping);
        void onCancelSelected();
    }

    private OnClickListener mListener;
    public void setListener(OnClickListener listener) { mListener = listener; }

    private SimpleCSVReader mReader;
    private Spinner mItemNameSpinner;
    private Spinner mItemCodeSpinner;
    private Spinner mCategorySpinner;
    private Spinner mBranchSpinner;
    private Spinner mQuantitySpinner;

    public static ColumnMappingDialog newInstance(SimpleCSVReader reader) {
        ColumnMappingDialog dialog = new ColumnMappingDialog();
        dialog.mReader = reader;
        return dialog;
    }

    public static final int DATA_ITEM_NAME = 1;
    public static final int DATA_ITEM_CODE = 2;
    public static final int DATA_CATEGORY = 3;
    public static final int DATA_BRANCH = 4;
    public static final int DATA_QUANTITY = 5;

    public static final int NO_DATA_FOUND = -1;

    /**
     * The first element in the spinner is the "--Not Set--" option,
     * if that is selected, then no data was set. Otherwise,
     * get a 0-based index into the reader's header array.
     */
    private int getMapping(Spinner spinner) {
        return spinner.getSelectedItemPosition() == 0 ?
                NO_DATA_FOUND :
                (spinner.getSelectedItemPosition() - 1);
    }

    private Map<Integer, Integer> getColumnMapping() {
        Map<Integer, Integer> mapping = new HashMap<>();

        mapping.put(DATA_ITEM_NAME, getMapping(mItemNameSpinner));
        mapping.put(DATA_ITEM_CODE, getMapping(mItemCodeSpinner));
        mapping.put(DATA_CATEGORY, getMapping(mCategorySpinner));
        mapping.put(DATA_QUANTITY, getMapping(mQuantitySpinner));
        mapping.put(DATA_BRANCH, getMapping(mBranchSpinner));

        return mapping;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_import_data, null);

        mItemNameSpinner = (Spinner) view.findViewById(R.id.dialog_import_item_name_spinner);
        mItemCodeSpinner = (Spinner) view.findViewById(R.id.dialog_import_item_code_spinner);
        mCategorySpinner = (Spinner) view.findViewById(R.id.dialog_import_category_spinner);
        mBranchSpinner = (Spinner) view.findViewById(R.id.dialog_import_branch_spinner);
        mQuantitySpinner = (Spinner) view.findViewById(R.id.dialog_import_quantity_spinner);

        Vector<String> options = new Vector<>(mReader.getHeaders());
        // The tabs at both ends will increase the width
        options.add(0, "\t--Not Selected--\t");

        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray());

        mItemNameSpinner.setAdapter(adapter);
        mItemCodeSpinner.setAdapter(adapter);
        mCategorySpinner.setAdapter(adapter);
        mBranchSpinner.setAdapter(adapter);
        mQuantitySpinner.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setCancelable(false);
        builder.setTitle("Import Data").
                setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.onColumnMappingDone(mReader,
                                            getColumnMapping());
                                }
                            }
                        }).
                setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.onCancelSelected();
                                }
                            }
                        });
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // It should initially be Disabled
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        });

        AdapterView.OnItemSelectedListener clickListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // only show the ok button if the item name has been set
                if (parent == mItemNameSpinner) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(position != 0 ? View.VISIBLE : View.GONE);
                }

                resetIfNotSelf(mItemNameSpinner, parent, position);
                resetIfNotSelf(mItemCodeSpinner, parent, position);
                resetIfNotSelf(mCategorySpinner, parent, position);
                resetIfNotSelf(mBranchSpinner, parent, position);
                resetIfNotSelf(mQuantitySpinner, parent, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        mItemCodeSpinner.setOnItemSelectedListener(clickListener);
        mItemNameSpinner.setOnItemSelectedListener(clickListener);
        mCategorySpinner.setOnItemSelectedListener(clickListener);
        mBranchSpinner.setOnItemSelectedListener(clickListener);
        mQuantitySpinner.setOnItemSelectedListener(clickListener);

        return dialog;
    }

    void resetIfNotSelf(Spinner spinner, AdapterView<?> parent, int position) {
        if (spinner != parent &&
                spinner.getSelectedItemPosition() == position) {

            // reset to the "--Not Set--" position
            spinner.setSelection(0);
        }
    }
}
