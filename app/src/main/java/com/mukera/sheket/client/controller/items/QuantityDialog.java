package com.mukera.sheket.client.controller.items;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;
import com.mukera.sheket.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by fuad on 7/11/16.
 */
public class QuantityDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final long NO_BRANCH_SELECTED_ID = -1;
    private static final String NO_BRANCH_SELECTED_NAME = "--Select Branch--";

    private Spinner mSpinnerActionType, mSpinnerOtherBranch;
    private LinearLayout mLayoutOtherBranch;

    private Spinner mSpinnerUnitSelection;
    private LinearLayout mLayoutUnitSelection;

    private TextView mUnitExtension, mConversionFormula;
    private EditText mQtyEdit;
    private EditText mItemNote;

    private Button mBtnOk;

    private SItem mItem;

    private DialogListener mListener;

    private Long mCurrentBranch;

    public void setItem(SItem item) {
        mItem = item;
    }

    /**
     * We need to set this because we shouldn't display the current branch
     * in the "transfer-branches".
     */
    public void setCurrentBranch(Long branch_id) {
        mCurrentBranch = branch_id;
    }

    public void setListener(DialogListener listener) {
        mListener = listener;
    }

    boolean isQuantitySet() {
        return !mQtyEdit.getText().toString().trim().isEmpty();
    }

    double getConvertedItemQuantity() {
        if (!isQuantitySet()) return 0;

        double factor = 1;
        boolean is_derived_selected = mItem.has_derived_unit &&
                mSpinnerUnitSelection.getSelectedItemPosition() == 1;
        if (is_derived_selected) {
            factor = mItem.derived_factor;
        }

        return factor * Double.valueOf(mQtyEdit.getText().toString().trim());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.MainActivity.QUANTITY_DIALOG_BRANCH_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    String getItemNote() {
        return mItemNote.getText().toString().trim();
    }

    void setOkBtnStatus() {
        ActionType type = getSelectedActionType();
        if (type.type == ACTION_TYPE_NO_SELECTED) {
            mBtnOk.setEnabled(false);
            return;
        }

        if (!isQuantitySet()) {
            mBtnOk.setEnabled(false);
            return;
        }

        switch (type.type) {
            // if it either matches the two, then we have more things to check, so break out
            case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
            case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER:
                break;

            default:
                mBtnOk.setEnabled(true);
                return;
        }

        TransBranch transBranch = (TransBranch) mSpinnerOtherBranch.getSelectedItem();
        if (transBranch.id == NO_BRANCH_SELECTED_ID) {
            mBtnOk.setEnabled(false);
            return;
        }

        mBtnOk.setEnabled(true);
    }

    void updateConversionRateDisplay() {
        if (!mItem.has_derived_unit) return;

        String unit = (String) mSpinnerUnitSelection.getSelectedItem();
        mUnitExtension.setText(String.format("  %s  ", unit));
        if (mSpinnerUnitSelection.getSelectedItemPosition() == 0) {
            mConversionFormula.setVisibility(View.GONE);
        } else if (isQuantitySet()) {
            mConversionFormula.setVisibility(View.VISIBLE);

            String qty = mQtyEdit.getText().toString().trim();
            String base_unit = UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement);
            String derived = mItem.derived_name;
            String factor = Utils.formatDoubleForDisplay(mItem.derived_factor);

            String total = Utils.formatDoubleForDisplay(
                    Double.valueOf(qty) * mItem.derived_factor);

            mConversionFormula.setText(
                    String.format(" %s %s = %s * (%s * %s) = %s %s ",
                            qty, derived,
                            qty, factor, base_unit,
                            total, base_unit));
        } else {
            mConversionFormula.setVisibility(View.GONE);
        }
    }

    void configureUnitSelectionViews(View view) {
        mQtyEdit = (EditText) view.findViewById(R.id.dialog_qty_edit_text_qty);
        mQtyEdit.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                setOkBtnStatus();
                updateConversionRateDisplay();
            }
        });

        mItemNote = (EditText) view.findViewById(R.id.dialog_qty_edit_text_item_note);

        mLayoutUnitSelection = (LinearLayout) view.findViewById(R.id.dialog_qty_layout_units);
        mSpinnerUnitSelection = (Spinner) view.findViewById(R.id.dialog_qty_spinner_units);
        mUnitExtension = (TextView) view.findViewById(R.id.dialog_qty_text_unit_extension);
        mConversionFormula = (TextView) view.findViewById(R.id.dialog_qty_text_conversion);

        if (!mItem.has_derived_unit) {
            mLayoutUnitSelection.setVisibility(View.GONE);
            mSpinnerUnitSelection.setVisibility(View.GONE);
            mConversionFormula.setVisibility(View.GONE);

            mUnitExtension.setVisibility(View.VISIBLE);
            mUnitExtension.setText(UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement));
        } else {
            mLayoutUnitSelection.setVisibility(View.VISIBLE);
            mSpinnerUnitSelection.setVisibility(View.VISIBLE);
            mConversionFormula.setVisibility(View.VISIBLE);

            mUnitExtension.setVisibility(View.VISIBLE);

            List<String> units = new ArrayList<>();
            units.add(UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement));
            units.add(mItem.derived_name);
            mSpinnerUnitSelection.setAdapter(new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    units.toArray()));
            mSpinnerUnitSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateConversionRateDisplay();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    ActionType getSelectedActionType() {
        return (ActionType) mSpinnerActionType.getSelectedItem();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().
                inflate(R.layout.dialog_quantity, null);

        configureUnitSelectionViews(view);

        Button cancelBtn;
        cancelBtn = (Button) view.findViewById(R.id.dialog_qty_btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.dialogCancel(QuantityDialog.this);
            }
        });

        List<ActionType> sources = new ArrayList<>();
        sources.add(sTransTypesHashMap.get(ACTION_TYPE_NO_SELECTED));
        sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH));
        sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER));

        sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_INCREASE_PURCHASE));
        sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH));

        ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, sources.toArray());

        mSpinnerActionType = (Spinner) view.findViewById(R.id.dialog_qty_spinner_action_type);
        mSpinnerActionType.setAdapter(adapter);
        mSpinnerActionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ActionType action = (ActionType) parent.getAdapter().getItem(position);
                switch (action.type) {
                    case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                    case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER:
                        mLayoutOtherBranch.setVisibility(View.VISIBLE);
                        break;
                    default:
                        mLayoutOtherBranch.setVisibility(View.GONE);
                }
                setOkBtnStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSpinnerOtherBranch = (Spinner) view.findViewById(R.id.dialog_qty_spinner_other_branch);
        /*
        // TODO: @melaeke, you should change the {@code Spinner} to the one we discussed about
        ArrayAdapter branchAdapter = new ArrayAdapter(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, mTransBranches.toArray());
        mSpinnerOtherBranch.setAdapter(branchAdapter);
        */
        mSpinnerOtherBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setOkBtnStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mLayoutOtherBranch = (LinearLayout) view.findViewById(R.id.dialog_qty_layout_other_branch);

        mBtnOk = (Button) view.findViewById(R.id.dialog_qty_btn_ok);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = getConvertedItemQuantity();
                STransactionItem transItem = new STransactionItem();
                transItem.item = mItem;
                transItem.item_id = mItem.item_id;
                transItem.quantity = qty;
                transItem.trans_type = getSelectedActionType().type;
                transItem.item_note = getItemNote();
                transItem.company_id = PrefUtil.getCurrentCompanyId(getActivity());
                transItem.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
                switch (transItem.trans_type) {
                    case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                    case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER:
                        TransBranch other_branch = (TransBranch) mSpinnerOtherBranch.getSelectedItem();
                        transItem.other_branch_id = other_branch.id;
                        break;
                    default:
                        transItem.other_branch_id = SheketContract.BranchEntry.DUMMY_BRANCH_ID;
                }

                // dismiss keyboard
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mQtyEdit.getApplicationWindowToken(), 0);

                mListener.dialogOk(QuantityDialog.this, transItem);
            }
        });

        // start things off
        setOkBtnStatus();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mItem.name);
        Dialog dialog = builder.setView(view).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    /**
     * Loader Manager callbacks
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getActivity());

        return new CursorLoader(getActivity(),
                BranchItemEntry.buildItemInAllBranches(company_id, mItem.item_id),
                SItem.ITEM_WITH_BRANCH_DETAIL_COLUMNS,

                // We don't want the current branch to appear in the result
                BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " != ?",
                new String[]{String.valueOf(mCurrentBranch)},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // We've loaded the item, we now have ALL the branch information
        mItem = new SItem(data, true);

        // TODO: @melaeke, you should also make visible OR hide the transfer to other branch options
        // if there are NO OTHER branches
        // this tells you there are no other branches, so disable the "send to/ recieve from" options
        if (mItem.available_branches.isEmpty()) {

        } else {
            // TODO: @melaeke you can go through the branches the item exists in like this
            for (Pair<SBranchItem, SBranch> pair : mItem.available_branches) {
                SBranchItem branchItem = pair.first;
                SBranch branch = pair.second;

                // this means the item exists inside the branch
                if (branchItem != null) {
                    // you can get the quantity like this
                    double qty = branchItem.quantity;
                } else {
                    // this means the item doesn't exist in the branch
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This only occurs if there was a problem loading the data
        mItem = null;
    }

    /**
     * End: Loader Manager Callbacks
     */

    private static final int ACTION_TYPE_NO_SELECTED = -1;
    private static final HashMap<Integer, ActionType> sTransTypesHashMap;

    static {
        sTransTypesHashMap = new HashMap<>();

        sTransTypesHashMap.put(ACTION_TYPE_NO_SELECTED,
                new ActionType(ACTION_TYPE_NO_SELECTED, "--Select Source--"));

        sTransTypesHashMap.put(TransItemEntry.TYPE_INCREASE_PURCHASE,
                new ActionType(TransItemEntry.TYPE_INCREASE_PURCHASE, "Buy"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH,
                new ActionType(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH, "Receive"));

        sTransTypesHashMap.put(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH,
                new ActionType(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH, "Sell"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER,
                new ActionType(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER, "Send"));
    }


    public interface DialogListener {
        void dialogCancel(DialogFragment dialog);

        void dialogOk(DialogFragment dialog, STransactionItem transItem);
    }

    private static class ActionType {
        public int type;
        public String name;

        public ActionType(int type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TransBranch {
        public long id;
        public String name;

        public TransBranch(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
