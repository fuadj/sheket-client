package com.mukera.sheket.client.controller.items;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;

import java.util.ArrayList;
import java.util.UUID;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by gamma on 3/6/16.
 */
public class ItemCreateEditDialog extends DialogFragment {
    public static final String ARG_EDIT_MODE = "arg_edit_mode";
    public static final String ARG_CURRENT_CATEGORY = "arg_current_category";
    public static final String ARG_ITEM = "arg_item";

    private EditText mItemName;
    private EditText mItemCode;
    private FancyButton mBtnUnits;

    private SwitchCompat mSwitchHasBundle;
    private View mLayoutBundle;

    private EditText mBundleName;
    private EditText mBundleFactor;

    private TextView mConversionRate;

    private int mCurrentCategory;
    private SItem mEditItem;
    private boolean mIsEditMode;
    private int mCurrentSelectedUnit = 0;   // default is 0, which is "pcs"

    static final String[] measurement_unit_names;
    static {
        ArrayList<String> units = UnitsOfMeasurement.getAllUnits();
        measurement_unit_names = new String[units.size()];
        units.toArray(measurement_unit_names);
    }

    /**
     * Instantiates this dialog. If {@code item} is not null, it means it is in edit mode.
     * Otherwise it is in create mode.
     */
    public static ItemCreateEditDialog newInstance(int current_category, SItem item) {
        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_CATEGORY, current_category);
        args.putBoolean(ARG_EDIT_MODE, item != null);
        if (item != null) {
            args.putParcelable(ARG_ITEM, item);
        }
        ItemCreateEditDialog dialog = new ItemCreateEditDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCurrentCategory = args.getInt(ARG_CURRENT_CATEGORY);
        mIsEditMode = args.getBoolean(ARG_EDIT_MODE);
        if (mIsEditMode)
            mEditItem = args.getParcelable(ARG_ITEM);
    }

    void linkViews(View view) {
        mItemName = (EditText) view.findViewById(R.id.dialog_item_c_e_edit_text_name);
        mItemCode = (EditText) view.findViewById(R.id.dialog_item_c_e_edit_text_code);

        mBtnUnits = (FancyButton) view.findViewById(R.id.dialog_item_c_e_btn_unit);

        mSwitchHasBundle = (SwitchCompat) view.findViewById(R.id.dialog_item_c_e_switch_has_bundle);

        mLayoutBundle = view.findViewById(R.id.dialog_item_c_e_layout_bundle);

        mBundleName = (EditText) view.findViewById(R.id.dialog_item_c_e_edit_text_bundle_name);
        mBundleFactor = (EditText) view.findViewById(R.id.dialog_item_c_e_edit_text_bundle_factor);

        mConversionRate = (TextView) view.findViewById(R.id.dialog_item_c_e_text_conversion_rate);
    }

    String getItemName() { return mItemName.getText().toString().trim(); }
    String getItemCode() { return mItemCode.getText().toString().trim(); }

    String getBundleName() { return mBundleName.getText().toString().trim(); }
    String getBundleFactor() { return mBundleFactor.getText().toString().trim(); }

    boolean isBundleValid() {
        return !getBundleName().isEmpty() &&
                !getBundleFactor().isEmpty();
    }

    void updateViews() {
        boolean show_bundle_layout = mSwitchHasBundle.isChecked();
        mLayoutBundle.setVisibility(
                 show_bundle_layout ? View.VISIBLE : View.GONE);
        if (show_bundle_layout && isBundleValid()) {
            mConversionRate.setVisibility(View.VISIBLE);
            mConversionRate.setText(
                    String.format("  1 %s = %s %s  ",
                            getBundleName(), getBundleFactor(),
                            measurement_unit_names[mCurrentSelectedUnit]));
        } else {
            mConversionRate.setVisibility(View.GONE);
        }

        boolean show_done_btn = true;
        if (getItemName().isEmpty()) {
            show_done_btn = false;
        } else if (show_bundle_layout &&
                !isBundleValid()) {
            show_done_btn = false;
        }

        ((AlertDialog) getDialog()).
                getButton(AlertDialog.BUTTON_POSITIVE).
                setVisibility(show_done_btn ? View.VISIBLE : View.GONE);
        mBtnUnits.setText(measurement_unit_names[mCurrentSelectedUnit]);
    }

    String non_null(String s) { return s != null ? s : ""; }

    void bindPreviousItemValuesToUI() {
        mItemName.setText(non_null(mEditItem.name));
        mItemCode.setText(non_null(mEditItem.item_code));
        mCurrentSelectedUnit = mEditItem.unit_of_measurement;

        mSwitchHasBundle.setChecked(mEditItem.has_derived_unit);
        if (mEditItem.has_derived_unit) {
            mBundleName.setText(non_null(mEditItem.derived_name));
            mBundleFactor.setText(non_null(String.valueOf(mEditItem.derived_factor)));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().
                inflate(R.layout.dialog_item_create_edit, null);

        linkViews(view);
        if (mIsEditMode)
            bindPreviousItemValuesToUI();

        mSwitchHasBundle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateViews();
            }
        });
        TextWatcherAdapter textWatcher = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                updateViews();
            }
        };
        mItemName.addTextChangedListener(textWatcher);
        mBundleName.addTextChangedListener(textWatcher);
        mBundleFactor.addTextChangedListener(textWatcher);

        mBtnUnits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setSingleChoiceItems(
                        measurement_unit_names,
                        mCurrentSelectedUnit,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mCurrentSelectedUnit = which;
                                dialog.dismiss();
                                updateViews();
                            }
                        }).show();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view).
                setCancelable(false).
                setTitle(mIsEditMode ?
                        R.string.dialog_item_c_e_title_edit:
                        R.string.dialog_item_c_e_title_new);
        builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                final ProgressDialog progressDialog =
                        ProgressDialog.show(getActivity(),
                                mIsEditMode ? "Editing Item" : "Creating Item",
                                "Please Wait...",
                                true);
                applyItemOperation(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        progressDialog.dismiss();
                    }
                });
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            // we use neutral instead of negative so it the button is left aligned
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final Dialog createEditDialog = builder.create();
        createEditDialog.setCanceledOnTouchOutside(false);

        createEditDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // to start things off
                updateViews();
            }
        });
        return createEditDialog;
    }

    void applyItemOperation(final Runnable runOnFinish) {
        final int item_id;
        if (mIsEditMode)
            item_id = mEditItem.item_id;
        else {
            int new_item_id = PrefUtil.getNewItemId(getActivity());
            PrefUtil.setNewItemId(getActivity(), new_item_id);

            item_id = new_item_id;
        }

        final String name = getItemName();

        final int unit = mCurrentSelectedUnit;
        final String code = getItemCode();

        final int has_derived_unit = SheketContract.toInt(mSwitchHasBundle.isChecked());
        final String derived_name = getBundleName();

        final int category_id = mCurrentCategory;

        String factor = getBundleFactor();
        final double derived_factor = factor.isEmpty() ? 0 : Double.valueOf(factor);

        final double reorder_level = 0;

        final String model_year = "";
        final String part_number = "";
        final String bar_code = "";

        final String uuid;
        final int change_status;
        if (mIsEditMode) {
            uuid = mEditItem.client_uuid;
            change_status = mEditItem.change_status == SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED ?
                    SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED :
                    SheketContract.ChangeTraceable.CHANGE_STATUS_UPDATED;
        } else {
            uuid = UUID.randomUUID().toString();
            change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
        }

        final int company_id = PrefUtil.getCurrentCompanyId(getContext());
        Thread t = new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(ItemEntry.COLUMN_NAME, name);
                values.put(ItemEntry.COLUMN_CATEGORY_ID, category_id);

                values.put(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT, unit);
                values.put(ItemEntry.COLUMN_HAS_DERIVED_UNIT, has_derived_unit);
                values.put(ItemEntry.COLUMN_DERIVED_UNIT_NAME, derived_name);
                values.put(ItemEntry.COLUMN_DERIVED_UNIT_FACTOR, derived_factor);

                values.put(ItemEntry.COLUMN_REORDER_LEVEL, reorder_level);

                values.put(ItemEntry.COLUMN_BAR_CODE, bar_code);
                values.put(ItemEntry.COLUMN_ITEM_CODE, code);
                values.put(ItemEntry.COLUMN_HAS_BAR_CODE, !bar_code.isEmpty());
                values.put(ItemEntry.COLUMN_MODEL_YEAR, model_year);
                values.put(ItemEntry.COLUMN_PART_NUMBER, part_number);
                values.put(ItemEntry.COLUMN_COMPANY_ID, PrefUtil.getCurrentCompanyId(getActivity()));
                values.put(SheketContract.UUIDSyncable.COLUMN_UUID, uuid);
                values.put(SheketContract.ChangeTraceable.COLUMN_CHANGE_INDICATOR, change_status);

                if (mIsEditMode) {
                    getActivity().getContentResolver().update(ItemEntry.buildBaseUri(company_id), values,
                            ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                            new String[]{Long.toString(item_id)});
                } else {
                    values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
                    getActivity().getContentResolver().insert(
                            ItemEntry.buildBaseUri(company_id), values);
                }

                getActivity().runOnUiThread(runOnFinish);
            }
        };
        t.start();
    }
}
