package com.mukera.sheket.client.controller.items;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.BranchEntry;
import com.mukera.sheket.client.data.SheketContract.BranchItemEntry;
import com.mukera.sheket.client.data.SheketContract.TransItemEntry;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction.STransactionItem;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;
import com.mukera.sheket.client.utils.Utils;

/**
 * Created by fuad on 7/11/16.
 */
public class QuantityDialog extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * We made it an abstract class instead of an interface so the
     * {@code Parcelable} methods could be implemented here.
     */
    public static abstract class QuantityListener implements Parcelable {
        abstract void dialogCancel(DialogFragment dialog);

        abstract void dialogOk(DialogFragment dialog, STransactionItem transItem);

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    }

    private ImageButton mImgBtnAction;

    private View mLayoutOtherBranchSelector;
    private TextView mTextTransferType;
    private Button mBtnSelectOtherBranch;

    private View mLayoutOtherBranchQty;
    private TextView mTextOtherBranchName;
    // only 1 of these 2 is visible at any time
    private TextView mTextOtherBranchQty;
    private ImageView mImgOtherBranchEmpty;

    private View mLayoutUnitSelection;
    private RadioGroup mRadioGroup;
    private RadioButton mRadioStandard, mRadioDerived;

    private EditText mQtyEdit;
    // This displays the unit type to the right of the quantity
    private TextView mTextUnitExtension;
    private TextView mTextConversionFormula;

    private EditText mEditItemNote;

    private Button mBtnOk, mBtnCancel;

    private SItem mItem;
    private Long mCurrentBranch;

    private Pair<SBranchItem, SBranch> mOtherBranchItem = null;

    private QuantityListener mListener;

    private enum ActionType {NOT_SET, BUY, SELL, SEND_TO, RECEIVE_FROM}

    private ActionType mActionType = ActionType.NOT_SET;

    private static final String KEY_ARG_ITEM = "key_arg_item";
    private static final String KEY_ARG_BRANCH = "key_arg_branch";
    private static final String KEY_ARG_LISTENER = "key_arg_listener";

    public static QuantityDialog newInstance(SItem item, long branch, QuantityListener listener) {
        Bundle args = new Bundle();
        args.putLong(KEY_ARG_BRANCH, branch);
        args.putParcelable(KEY_ARG_ITEM, item);
        args.putParcelable(KEY_ARG_LISTENER, listener);

        QuantityDialog dialog = new QuantityDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCurrentBranch = args.getLong(KEY_ARG_BRANCH);
        mItem = args.getParcelable(KEY_ARG_ITEM);
        mListener = args.getParcelable(KEY_ARG_LISTENER);
    }

    boolean isQuantitySet() {
        return !mQtyEdit.getText().toString().trim().isEmpty();
    }

    boolean isDerivedSelected() {
        return mRadioGroup.getCheckedRadioButtonId() == mRadioDerived.getId();
    }

    double getConvertedItemQuantity() {
        if (!isQuantitySet()) return 0;

        double factor = 1;
        boolean is_derived_selected = mItem.has_derived_unit &&
                isDerivedSelected();
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

    void updateOkBtnStatus() {
        if (mActionType == ActionType.NOT_SET) {
            mBtnOk.setEnabled(false);
            return;
        }

        if (!isQuantitySet()) {
            mBtnOk.setEnabled(false);
            return;
        }

        switch (mActionType) {
            case SELL:
            case BUY:
                mBtnOk.setEnabled(true);
                return;
        }

        if (mOtherBranchItem == null) {
            mBtnOk.setEnabled(false);
            return;
        }

        mBtnOk.setEnabled(true);
    }

    void updateConversionRateDisplay() {
        if (mItem.has_derived_unit) {
            mTextUnitExtension.setText(
                    UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement));
            return;
        }

        String unit;
        if (isDerivedSelected()) {
            unit = String.format("  %s  ", mRadioDerived.getText());
        } else {
            unit = String.format("  %s  ", mRadioStandard.getText());
        }
        mTextUnitExtension.setText(unit);

        if (!isDerivedSelected()) {
            mTextConversionFormula.setVisibility(View.GONE);
        } else if (isQuantitySet()) {
            mTextConversionFormula.setVisibility(View.VISIBLE);


            String qty = mQtyEdit.getText().toString().trim();
            String base_unit = UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement);
            String derived = mItem.derived_name;
            String factor = Utils.formatDoubleForDisplay(mItem.derived_factor);

            String total = Utils.formatDoubleForDisplay(
                    Double.valueOf(qty) * mItem.derived_factor);

            mTextConversionFormula.setText(
                    String.format(" %s %s = %s * (%s * %s) = %s %s ",
                            qty, derived,
                            qty, factor, base_unit,
                            total, base_unit));
        } else {
            mTextConversionFormula.setVisibility(View.GONE);
        }
    }

    void linkMembersToViews(View view) {
        mEditItemNote = (EditText) view.findViewById(R.id.dialog_qty_edit_text_item_note);
        mImgBtnAction = (ImageButton) view.findViewById(R.id.dialog_qty_img_btn_action);

        mLayoutOtherBranchSelector = view.findViewById(R.id.dialog_qty_layout_other_branch);
        mTextTransferType = (TextView) view.findViewById(R.id.dialog_qty_text_transfer_type);
        mBtnSelectOtherBranch = (Button) view.findViewById(R.id.dialog_qty_btn_select_branch);

        mLayoutOtherBranchQty = view.findViewById(R.id.dialog_qty_layout_other_branch_qty);
        mTextOtherBranchName = (TextView) view.findViewById(R.id.dialog_qty_text_selected_branch_name);
        mTextOtherBranchQty = (TextView) view.findViewById(R.id.dialog_qty_text_other_branch_qty);
        mImgOtherBranchEmpty = (ImageView) view.findViewById(R.id.dialog_qty_img_item_not_exist);

        mLayoutUnitSelection = view.findViewById(R.id.dialog_qty_layout_unit_selection);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.dialog_qty_radio_group);

        mRadioStandard = (RadioButton) view.findViewById(R.id.dialog_qty_radio_standard);
        mRadioDerived = (RadioButton) view.findViewById(R.id.dialog_qty_radio_derived);
        mRadioStandard.setText(UnitsOfMeasurement.getUnitSymbol(mItem.unit_of_measurement));
        if (mItem.has_derived_unit) {
            mRadioDerived.setText(mItem.derived_name);
        }
        // Make the "Standard" unit be the "default" selected
        mRadioGroup.check(mRadioStandard.getId());

        mQtyEdit = (EditText) view.findViewById(R.id.dialog_qty_edit_text_qty);
        mTextUnitExtension = (TextView) view.findViewById(R.id.dialog_qty_text_unit_extension);

        mTextConversionFormula = (TextView) view.findViewById(R.id.dialog_qty_text_conversion);

        mEditItemNote = (EditText) view.findViewById(R.id.dialog_qty_edit_text_item_note);

        mBtnCancel = (Button) view.findViewById(R.id.dialog_qty_btn_cancel);
        mBtnOk = (Button) view.findViewById(R.id.dialog_qty_btn_ok);
    }

    void updateVisibility() {
        int visibility;
        boolean is_transfer;

        switch (mActionType) {
            case SEND_TO:
            case RECEIVE_FROM:
                visibility = View.VISIBLE;
                is_transfer = true;
                break;
            default:
                visibility = View.GONE;
                is_transfer = false;
        }

        mLayoutOtherBranchSelector.setVisibility(visibility);
        if (is_transfer &&
                // you should've also specified another branch
                mOtherBranchItem != null) {
            mLayoutOtherBranchQty.setVisibility(View.VISIBLE);
        } else {
            mLayoutOtherBranchQty.setVisibility(View.GONE);
        }

        mLayoutUnitSelection.setVisibility(
                mItem.has_derived_unit ? View.VISIBLE : View.GONE);
        if (!mItem.has_derived_unit)
            mTextConversionFormula.setVisibility(View.GONE);
    }

    void updateViews() {
        // if we've not yet selected an action, then set the Action Image Btn to an image
        // showing the options available.
        if (mActionType == ActionType.NOT_SET) {
            // don't show the transfer options
            if (mItem.available_branches.isEmpty()) {

            } else {

            }
        }
        boolean is_transfer = false;
        switch (mActionType) {
            case SEND_TO:
                mTextTransferType.setText("Send To");
                is_transfer = true;
                break;
            case RECEIVE_FROM:
                mTextTransferType.setText("Receive From");
                is_transfer = true;
                break;
        }

        if (is_transfer &&
                mOtherBranchItem != null) {
            SBranchItem branchItem = mOtherBranchItem.first;
            SBranch branch = mOtherBranchItem.second;

            mTextOtherBranchName.setText(branch.branch_name);
            if (branchItem != null) {
                mTextOtherBranchQty.setVisibility(View.VISIBLE);
                mImgOtherBranchEmpty.setVisibility(View.GONE);
                mTextOtherBranchQty.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
            } else {
                mTextOtherBranchQty.setVisibility(View.GONE);
                mImgOtherBranchEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().
                inflate(R.layout.dialog_quantity, null);

        linkMembersToViews(view);
        updateVisibility();
        updateViews();

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateConversionRateDisplay();
            }
        });

        mQtyEdit.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                updateOkBtnStatus();
                updateConversionRateDisplay();
            }
        });

        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.dialogCancel(QuantityDialog.this);
            }
        });

        mImgBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionSelectDialog dialog = new ActionSelectDialog();
                dialog.mShowTransferActions = !mItem.available_branches.isEmpty();
                dialog.setListener(new ActionSelectDialog.OnActionSelectListener() {
                    @Override
                    public void onActionSelect(Dialog actionDialog, ActionType type) {
                        actionDialog.dismiss();
                        mActionType = type;

                        updateVisibility();
                        updateOkBtnStatus();
                    }
                });
                dialog.show(getActivity().getSupportFragmentManager(), null);
            }
        });

        mBtnSelectOtherBranch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = getActivity().getLayoutInflater().
                        inflate(R.layout.dialog_other_branch_select, null);
                ListView branchList = (ListView) view.findViewById(R.id.dialog_other_branch_list_view);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(view);
                final AlertDialog dialog = builder.create();

                final ArrayAdapter<Pair<SBranchItem, SBranch>> branchItemAdapter = new OtherBranchItemAdapter(getContext());

                branchList.setAdapter(branchItemAdapter);
                for (Pair<SBranchItem, SBranch> branch : mItem.available_branches) {
                    branchItemAdapter.add(branch);
                }

                branchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Pair<SBranchItem, SBranch> selected = branchItemAdapter.getItem(position);
                        if (mActionType == ActionType.RECEIVE_FROM) {
                            // we don't allow to receive from a branch which doesn't contain the item
                            // so ignore
                            if (selected.first == null) {

                            } else {
                                mOtherBranchItem = selected;
                                dialog.dismiss();
                                updateVisibility();
                                updateViews();
                            }
                        } else if (mActionType == ActionType.SEND_TO) {
                            mOtherBranchItem = selected;
                            dialog.dismiss();
                            updateVisibility();
                            updateViews();
                        }
                    }
                });
                dialog.show();
            }
        });

        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double qty = getConvertedItemQuantity();
                STransactionItem transItem = new STransactionItem();
                transItem.item = mItem;
                transItem.item_id = mItem.item_id;
                transItem.quantity = qty;
                transItem.trans_type = getTransactionType();
                transItem.item_note = mEditItemNote.getText().toString().trim();
                transItem.company_id = PrefUtil.getCurrentCompanyId(getActivity());
                transItem.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
                switch (transItem.trans_type) {
                    case TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH:
                    case TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER:
                        transItem.other_branch_id = mOtherBranchItem.second.branch_id;
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
        updateOkBtnStatus();

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mItem.name);
        Dialog dialog = builder.setView(view).create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    static class OtherBranchItemAdapter extends ArrayAdapter<Pair<SBranchItem, SBranch>> {
        public OtherBranchItemAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Pair<SBranchItem, SBranch> pair = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_dialog_other_branch_qty, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.branchName.setText(pair.second.branch_name);
            if (pair.first == null) {       // the branch doesn't contain the item
                holder.notExist.setVisibility(View.VISIBLE);
                holder.qty.setVisibility(View.GONE);
            } else {
                holder.notExist.setVisibility(View.GONE);
                holder.qty.setVisibility(View.VISIBLE);

                holder.qty.setText(Utils.formatDoubleForDisplay(pair.first.quantity));
            }

            return convertView;
        }

        static class ViewHolder {
            TextView branchName;

            TextView qty;
            ImageView notExist;

            public ViewHolder(View view) {
                branchName = (TextView) view.findViewById(R.id.list_item_dialog_other_branch_text_name);
                qty = (TextView) view.findViewById(R.id.list_item_dialog_other_branch_text_qty);
                notExist = (ImageView) view.findViewById(R.id.list_item_dialog_other_branch_img_not_exist);
            }
        }
    }

    int getTransactionType() {
        switch (mActionType) {
            case BUY:
                return TransItemEntry.TYPE_INCREASE_PURCHASE;
            case SELL:
                return TransItemEntry.TYPE_DECREASE_CURRENT_BRANCH;
            case RECEIVE_FROM:
                return TransItemEntry.TYPE_INCREASE_TRANSFER_FROM_OTHER_BRANCH;
            case SEND_TO:
                return TransItemEntry.TYPE_DECREASE_TRANSFER_TO_OTHER;
            default:
                Log.e("QuantityDialog", "Transaction Type can't be of type " + mActionType);
                return -1;
        }
    }

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
        if (!data.moveToFirst()) {
            // we can't show shit if there isn't an item
            getDialog().dismiss();
            return;
        }

        mItem = new SItem(data, true);

        updateViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // we can't show shit if there isn't an item
        //getDialog().dismiss();
    }

    public static class ActionSelectDialog extends DialogFragment {
        public boolean mShowTransferActions;
        public OnActionSelectListener mListener;

        public interface OnActionSelectListener {
            void onActionSelect(Dialog dialog, ActionType type);
        }

        public void setListener(OnActionSelectListener listener) {
            mListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().
                    inflate(R.layout.dialog_action_select, null);

            View layoutTransfer = view.findViewById(R.id.dialog_action_select_layout_transfer);
            layoutTransfer.setVisibility(mShowTransferActions ? View.VISIBLE : View.GONE);

            final ImageButton btnReceive = (ImageButton) view.findViewById(R.id.dialog_action_select_img_btn_receive);
            final ImageButton btnSend = (ImageButton) view.findViewById(R.id.dialog_action_select_img_btn_send);
            final ImageButton btnBuy = (ImageButton) view.findViewById(R.id.dialog_action_select_img_btn_buy);
            final ImageButton btnSell = (ImageButton) view.findViewById(R.id.dialog_action_select_img_btn_sell);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int view_id = v.getId();
                    ActionType actionType = ActionType.NOT_SET;
                    if (view_id == btnReceive.getId()) {
                        actionType = ActionType.RECEIVE_FROM;
                    } else if (view_id == btnSend.getId()) {
                        actionType = ActionType.SEND_TO;
                    } else if (view_id == btnBuy.getId()) {
                        actionType = ActionType.BUY;
                    } else if (view_id == btnSell.getId()) {
                        actionType = ActionType.SELL;
                    }

                    mListener.onActionSelect(getDialog(), actionType);
                }
            };

            btnReceive.setOnClickListener(clickListener);
            btnSend.setOnClickListener(clickListener);
            btnBuy.setOnClickListener(clickListener);
            btnSell.setOnClickListener(clickListener);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(view);
            return builder.create();
        }
    }
}
