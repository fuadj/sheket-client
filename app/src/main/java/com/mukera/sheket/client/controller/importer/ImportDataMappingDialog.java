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
public class ImportDataMappingDialog extends DialogFragment {
    public interface OnClickListener {
        void onOkSelected(SimpleCSVReader reader, Map<Integer, Integer> dataMapping);
        void onCancelSelected();
    }

    private OnClickListener mListener;
    public void setListener(OnClickListener listener) { mListener = listener; }

    private SimpleCSVReader mReader;
    private Spinner mItemNameSpinner;
    private Spinner mItemCodeSpinner;
    private Spinner mCategorySpinner;
    private Spinner mLocationSpinner;
    private Spinner mBalanceSpinner;

    public static ImportDataMappingDialog newInstance(SimpleCSVReader reader) {
        ImportDataMappingDialog dialog = new ImportDataMappingDialog();
        dialog.mReader = reader;
        return dialog;
    }

    public static final int DATA_ITEM_NAME = 1;
    public static final int DATA_ITEM_CODE = 2;
    public static final int DATA_CATEGORY = 3;
    public static final int DATA_LOCATION = 4;
    public static final int DATA_BALANCE = 5;

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

    private Map<Integer, Integer> getDataMapping() {
        Map<Integer, Integer> mapping = new HashMap<>();

        mapping.put(DATA_ITEM_NAME, getMapping(mItemNameSpinner));
        mapping.put(DATA_ITEM_CODE, getMapping(mItemCodeSpinner));
        mapping.put(DATA_CATEGORY, getMapping(mCategorySpinner));
        mapping.put(DATA_BALANCE, getMapping(mBalanceSpinner));
        mapping.put(DATA_LOCATION, getMapping(mLocationSpinner));

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
        mLocationSpinner = (Spinner) view.findViewById(R.id.dialog_import_location_spinner);
        mBalanceSpinner = (Spinner) view.findViewById(R.id.dialog_import_balance_spinner);

        Vector<String> options = new Vector<>(mReader.getHeaders());
        // The tabs at both ends will increase the width
        options.add(0, "\t--Not Set--\t");

        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, options.toArray());

        mItemNameSpinner.setAdapter(adapter);
        mItemCodeSpinner.setAdapter(adapter);
        mCategorySpinner.setAdapter(adapter);
        mLocationSpinner.setAdapter(adapter);
        mBalanceSpinner.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setCancelable(false);
        builder.setTitle("Import Data").
                setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.onOkSelected(ImportDataMappingDialog.this.mReader,
                                            getDataMapping());
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
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        AdapterView.OnItemSelectedListener clickListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // tie the OK button's "enabled-state" with the item name being set
                if (parent == mItemNameSpinner) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(position != 0);
                }
                resetIfNotSelf(mItemNameSpinner, parent, position);
                resetIfNotSelf(mItemCodeSpinner, parent, position);
                resetIfNotSelf(mCategorySpinner, parent, position);
                resetIfNotSelf(mLocationSpinner, parent, position);
                resetIfNotSelf(mBalanceSpinner, parent, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        mItemCodeSpinner.setOnItemSelectedListener(clickListener);
        mItemNameSpinner.setOnItemSelectedListener(clickListener);
        mCategorySpinner.setOnItemSelectedListener(clickListener);
        mLocationSpinner.setOnItemSelectedListener(clickListener);
        mBalanceSpinner.setOnItemSelectedListener(clickListener);

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
