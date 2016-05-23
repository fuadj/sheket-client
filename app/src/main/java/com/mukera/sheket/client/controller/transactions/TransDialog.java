package com.mukera.sheket.client.controller.transactions;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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
import com.mukera.sheket.client.UnitsOfMeasurement;
import com.mukera.sheket.client.controller.util.NumberFormatter;
import com.mukera.sheket.client.controller.util.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.TransItemEntry;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.*;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by gamma on 3/5/16.
 */
public class TransDialog {
    private static final long SELECT_BRANCH_ID = -1;
    private static final String SELECT_BRANCH_NAME = "--Select Branch--";

    public static abstract class QtyDialog extends DialogFragment {
        protected TransQtyDialogListener mListener;
        protected SItem mItem;

        private Spinner mSpinnerUnitSelection;
        private LinearLayout mLayoutUnitSelection;
        private TextView mUnitExtension, mConversionFormula;
        private EditText mQtyEdit;

        protected List<TransBranch> mTransBranches;

        public void setListener(TransQtyDialogListener listener) {
            mListener = listener;
        }

        public void setItem(SItem item) {
            mItem = item;
        }

        public void setBranches(List<SBranch> branches) {
            mTransBranches = new ArrayList<>();
            mTransBranches.add(new TransBranch(SELECT_BRANCH_ID, SELECT_BRANCH_NAME));
            for (SBranch branch : branches) {
                 mTransBranches.add(new TransBranch(branch.branch_id, branch.branch_name));
            }
        }

        boolean isQuantitySet() {
            return !mQtyEdit.getText().toString().trim().isEmpty();
        }

        void hideKeyboard() {
            InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mQtyEdit.getApplicationWindowToken(), 0);
        }

        void configureUnitSelection(View view) {
            mQtyEdit = (EditText) view.findViewById(R.id.dialog_trans_qty_edit_text_qty);
            mQtyEdit.addTextChangedListener(new TextWatcherAdapter(){
                @Override
                public void afterTextChanged(Editable s) {
                    setOkBtnStatus();
                    updateConversionRateDisplay();
                }
            });

            mLayoutUnitSelection = (LinearLayout) view.findViewById(R.id.dialog_trans_qty_layout_units);
            mSpinnerUnitSelection = (Spinner) view.findViewById(R.id.dialog_trans_qty_spinner_units);
            mUnitExtension = (TextView) view.findViewById(R.id.dialog_trans_qty_text_unit_extension);
            mConversionFormula = (TextView) view.findViewById(R.id.dialog_trans_qty_text_conversion);

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
                    public void onNothingSelected(AdapterView<?> parent) { }
                });
            }
        }

        void updateConversionRateDisplay() {
            if (!mItem.has_derived_unit) return;

            String unit = (String)mSpinnerUnitSelection.getSelectedItem();
            mUnitExtension.setText(String.format("  %s  ", unit));
            if (mSpinnerUnitSelection.getSelectedItemPosition() == 0) {
                mConversionFormula.setVisibility(View.GONE);
            } else if (isQuantitySet()) {
                mConversionFormula.setVisibility(View.VISIBLE);

                String qty = mQtyEdit.getText().toString().trim();
                String base_unit = UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement);
                String derived = mItem.derived_name;
                String factor = NumberFormatter.formatDoubleForDisplay(mItem.derived_factor);

                String total = NumberFormatter.formatDoubleForDisplay(
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

        abstract void setOkBtnStatus();
    }

    public static QtyDialog newInstance(boolean is_buying) {
        QtyDialog dialog;
        if (is_buying) {
            dialog = new BuyQtyDialog();
        } else {
            dialog = new SellQtyDialog();
        }
        return dialog;
    }


    private static final int SOURCE_TYPE_SELECT_TYPE = -1;

    private static final HashMap<Integer, SourceType> sTransTypesHashMap;

    static {
        sTransTypesHashMap = new HashMap<>();

        sTransTypesHashMap.put(SOURCE_TYPE_SELECT_TYPE,
                new SourceType(SOURCE_TYPE_SELECT_TYPE, "--Select Source--"));

        sTransTypesHashMap.put(TransItemEntry.TYPE_INCREASE_PURCHASE,
                new SourceType(TransItemEntry.TYPE_INCREASE_PURCHASE, "Purchase Inventory"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_INCREASE_RETURN_ITEM,
                new SourceType(TransItemEntry.TYPE_INCREASE_RETURN_ITEM, "Return Inventory"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH,
                new SourceType(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH, "Transfer From Other"));

        sTransTypesHashMap.put(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH,
                new SourceType(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH, "Current Branch"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_DECREASE_DIRECT_PURCHASE,
                new SourceType(TransItemEntry.TYPE_DECREASE_DIRECT_PURCHASE, "Direct Sale"));
        sTransTypesHashMap.put(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER,
                new SourceType(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER, "Transfer To Other"));
    }

    public static class BuyQtyDialog extends QtyDialog {
        private Spinner mSpinnerSourceType, mSpinnerSourceBranch;
        private LinearLayout mLayoutSourceBranch;
        private Button mBtnOk;

        SourceType getSelectedSourceType() {
            return (SourceType) mSpinnerSourceType.getSelectedItem();
        }

        @Override
        void setOkBtnStatus() {
            SourceType type = getSelectedSourceType();
            if (type.type == SOURCE_TYPE_SELECT_TYPE) {
                mBtnOk.setEnabled(false);
                return;
            }

            if (!isQuantitySet()) {
                mBtnOk.setEnabled(false);
                return;
            }

            if (type.type != TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH) {
                mBtnOk.setEnabled(true);
                return;
            }

            TransBranch transBranch = (TransBranch) mSpinnerSourceBranch.getSelectedItem();
            if (transBranch.id == SELECT_BRANCH_ID) {
                mBtnOk.setEnabled(false);
                return;
            }
            mBtnOk.setEnabled(true);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().
                    inflate(R.layout.dialog_transaction_buy, null);

            Button cancelBtn;
            cancelBtn = (Button) view.findViewById(R.id.dialog_trans_qty_btn_cancel);
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.dialogCancel();
                }
            });

            mBtnOk = (Button) view.findViewById(R.id.dialog_trans_qty_btn_ok);
            mBtnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    double qty = getConvertedItemQuantity();
                    STransactionItem transItem = new STransactionItem();
                    transItem.item = mItem;
                    transItem.item_id = mItem.item_id;
                    transItem.quantity = qty;
                    transItem.trans_type = getSelectedSourceType().type;
                    transItem.company_id = PrefUtil.getCurrentCompanyId(getActivity());
                    transItem.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
                    if (transItem.trans_type == TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH) {
                        TransBranch other_branch = (TransBranch) mSpinnerSourceBranch.getSelectedItem();
                        transItem.other_branch_id = other_branch.id;
                    } else {
                        transItem.other_branch_id = SheketContract.BranchEntry.DUMMY_BRANCH_ID;
                    }

                    hideKeyboard();
                    mListener.dialogOk(transItem);
                }
            });

            List<SourceType> sources = new ArrayList<>();
            sources.add(sTransTypesHashMap.get(SOURCE_TYPE_SELECT_TYPE));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_INCREASE_PURCHASE));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_INCREASE_RETURN_ITEM));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH));

            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, sources.toArray());

            mSpinnerSourceType = (Spinner) view.findViewById(R.id.dialog_trans_buy_spinner_source_type);
            mSpinnerSourceType.setAdapter(adapter);
            mSpinnerSourceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SourceType type = (SourceType) parent.getAdapter().getItem(position);
                    if (type.type == TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH) {
                        mLayoutSourceBranch.setVisibility(View.VISIBLE);
                    } else {
                        mLayoutSourceBranch.setVisibility(View.GONE);
                    }
                    setOkBtnStatus();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            mSpinnerSourceBranch = (Spinner) view.findViewById(R.id.dialog_trans_buy_spinner_source_branch);
            ArrayAdapter branchAdapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, mTransBranches.toArray());
            mSpinnerSourceBranch.setAdapter(branchAdapter);
            mSpinnerSourceBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    setOkBtnStatus();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            mLayoutSourceBranch = (LinearLayout) view.findViewById(R.id.dialog_trans_buy_layout_source);

            configureUnitSelection(view);

            // start things off
            setOkBtnStatus();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(mItem.name);
            Dialog dialog = builder.setView(view).create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }

    public static class SellQtyDialog extends QtyDialog {
        private Spinner mSpinnerSourceType, mSpinnerDestBranch;
        private LinearLayout mLayoutDestBranch;
        private Button mBtnOk;

        SourceType getSelectedSourceType() {
            return (SourceType) mSpinnerSourceType.getSelectedItem();
        }

        @Override
        void setOkBtnStatus() {
            SourceType type = getSelectedSourceType();
            if (type.type == SOURCE_TYPE_SELECT_TYPE) {
                mBtnOk.setEnabled(false);
                return;
            }

            if (!isQuantitySet()) {
                mBtnOk.setEnabled(false);
                return;
            }

            if (type.type != TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER) {
                mBtnOk.setEnabled(true);
                return;
            }

            TransBranch transBranch = (TransBranch) mSpinnerDestBranch.getSelectedItem();
            if (transBranch.id == SELECT_BRANCH_ID) {
                mBtnOk.setEnabled(false);
                return;
            }
            mBtnOk.setEnabled(true);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().
                    inflate(R.layout.dialog_transaction_sell, null);

            Button cancelBtn;
            cancelBtn = (Button) view.findViewById(R.id.dialog_trans_qty_btn_cancel);
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.dialogCancel();
                }
            });

            mBtnOk = (Button) view.findViewById(R.id.dialog_trans_qty_btn_ok);
            mBtnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    double qty = getConvertedItemQuantity();
                    STransactionItem transItem = new STransactionItem();
                    transItem.item = mItem;
                    transItem.item_id = mItem.item_id;
                    transItem.quantity = qty;
                    transItem.company_id = PrefUtil.getCurrentCompanyId(getActivity());
                    transItem.trans_type = getSelectedSourceType().type;
                    transItem.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
                    if (transItem.trans_type == TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER) {
                        TransBranch other_branch = (TransBranch) mSpinnerDestBranch.getSelectedItem();
                        transItem.other_branch_id = other_branch.id;
                    } else {
                        transItem.other_branch_id = SheketContract.BranchEntry.DUMMY_BRANCH_ID;
                    }

                    hideKeyboard();
                    mListener.dialogOk(transItem);
                }
            });

            List<SourceType> sources = new ArrayList<>();
            sources.add(sTransTypesHashMap.get(SOURCE_TYPE_SELECT_TYPE));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_DECREASE_DIRECT_PURCHASE));
            sources.add(sTransTypesHashMap.get(TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER));

            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, sources.toArray());

            mSpinnerSourceType = (Spinner) view.findViewById(R.id.dialog_trans_sell_spinner_source_type);
            mSpinnerSourceType.setAdapter(adapter);
            mSpinnerSourceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    SourceType type = (SourceType) parent.getAdapter().getItem(position);
                    if (type.type == TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER) {
                        mLayoutDestBranch.setVisibility(View.VISIBLE);
                    } else {
                        mLayoutDestBranch.setVisibility(View.GONE);
                    }
                    setOkBtnStatus();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            mSpinnerDestBranch = (Spinner) view.findViewById(R.id.dialog_trans_sell_spinner_dest_branch);
            ArrayAdapter branchAdapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item, mTransBranches.toArray());
            mSpinnerDestBranch.setAdapter(branchAdapter);
            mSpinnerDestBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    setOkBtnStatus();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            mLayoutDestBranch = (LinearLayout) view.findViewById(R.id.dialog_trans_sell_layout_destination);

            configureUnitSelection(view);

            // start things off
            setOkBtnStatus();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(mItem.name);
            Dialog dialog = builder.setView(view).create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

    }

    public interface TransQtyDialogListener {
        void dialogCancel();

        void dialogOk(STransactionItem transactionItem);
    }

    private static class SourceType {
        public int type;
        public String name;

        public SourceType(int type, String name) {
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