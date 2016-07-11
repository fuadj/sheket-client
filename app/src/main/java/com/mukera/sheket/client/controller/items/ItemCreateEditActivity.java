package com.mukera.sheket.client.controller.items;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.Utils;

import java.util.UUID;

/**
 * Created by gamma on 3/6/16.
 */
public class ItemCreateEditActivity extends AppCompatActivity {
    public static final String ITEM_ACTIVITY_MODE = "ITEM_ACTIVITY_MODE";
    public static final String ITEM_MODE_CREATE = "item_mode_create";
    public static final String ITEM_MODE_EDIT = "item_mode_edit";

    public static final String ITEM_PARCEL_EXTRA = "item_parcel_extra";

    public static Intent createIntent(Context context, boolean edit_item, SItem item) {
        Intent intent = new Intent(context, ItemCreateEditActivity.class);
        intent.putExtra(ITEM_ACTIVITY_MODE,
                edit_item ? ITEM_MODE_EDIT : ITEM_MODE_CREATE);
        if (edit_item) {
            intent.putExtra(ITEM_PARCEL_EXTRA, item);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        String mode = getIntent().
                getStringExtra(ITEM_ACTIVITY_MODE);

        Bundle arguments = new Bundle();
        if (mode.equals(ITEM_MODE_CREATE)) {
            arguments.putBoolean(ItemCreateEditFragment.KEY_IS_EDITING_ITEM, false);
        } else {
            arguments.putBoolean(ItemCreateEditFragment.KEY_IS_EDITING_ITEM, true);
            arguments.putParcelable(ItemCreateEditFragment.KEY_EDITING_ITEM_PARCEL,
                    getIntent().getExtras().getParcelable(ITEM_PARCEL_EXTRA));
        }

        ItemCreateEditFragment fragment = new ItemCreateEditFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.new_item_container, fragment)
                .commit();
    }

    public static class ItemCreateEditFragment extends Fragment {
        public static final String KEY_IS_EDITING_ITEM = "key_is_editing_item";
        public static final String KEY_EDITING_ITEM_PARCEL = "key_editing_item_parcel";

        private EditText mName, mCode, mReorderLevel, mModelYear, mPartNumber;
        private Spinner mUnitsSpinner;
        private CheckBox mHasDerived;
        private EditText mDerivedName, mDerivedFactor, mFormula;
        private TextView mTextBundleName, mTextBundleFactor;

        private Button mCategoryBtn;

        private Button mCancel, mOk;

        private long mSelectedCategoryId = SheketContract.CategoryEntry.ROOT_CATEGORY_ID;
        private SCategory mSelectedCategory = null;

        private boolean mIsEditing;
        private SItem mEditingItem;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            mIsEditing = args.getBoolean(KEY_IS_EDITING_ITEM);
            if (mIsEditing) {
                mEditingItem = args.getParcelable(KEY_EDITING_ITEM_PARCEL);
                mSelectedCategoryId = mEditingItem.category;
            }
        }

        boolean isEmpty(Editable e) {
            return e.toString().trim().isEmpty();
        }

        void setOkButtonStatus() {
            if (isEmpty(mCode.getText())) {
                mOk.setEnabled(false);
                return;
            }

            if (mHasDerived.isChecked()) {
                if (isEmpty(mDerivedName.getText()) || isEmpty(mDerivedFactor.getText())) {
                    mOk.setEnabled(false);
                    return;
                }
            }
            mOk.setEnabled(true);
        }

        String t(String str) {
            return str.trim();
        }

        void updateFormulaDisplay() {
            if (!mHasDerived.isChecked()) return;

            if (isEmpty(mDerivedName.getText()) || isEmpty(mDerivedFactor.getText())) {
                mFormula.setText("");
                return;
            }

            // this has the form " 1 bundle = 120 pcs "
            mFormula.setText(
                    String.format("  1 %s = %s %s  ",
                            mDerivedName.getText().toString().trim(), mDerivedFactor.getText().toString().trim(),
                            UnitsOfMeasurement.getUnitSymbol(mUnitsSpinner.getSelectedItemPosition())));
        }

        void updateDerivedUnitDisplay() {
            boolean is_checked = mHasDerived.isChecked();

            mTextBundleName.setEnabled(is_checked);
            mTextBundleFactor.setEnabled(is_checked);

            mDerivedName.setEnabled(is_checked);
            mDerivedFactor.setEnabled(is_checked);

            mFormula.setVisibility(is_checked ? View.VISIBLE : View.GONE);
        }

        String non_null(String s) {
            if (s == null) return "";
            return s;
        }

        /**
         * Use the editing item to setup the UI. Only called if it is editing.
         */
        void setPreviousValuesAsInitialValues() {
            mCode.setText(non_null(mEditingItem.item_code));

            mName.setText(non_null(mEditingItem.name));
            mUnitsSpinner.setSelection(mEditingItem.unit_of_measurement);
            if (mEditingItem.has_derived_unit) {
                mHasDerived.setChecked(true);
                mDerivedName.setText(non_null(mEditingItem.derived_name));
                mDerivedFactor.setText(Double.toString(mEditingItem.derived_factor));

                updateFormulaDisplay();
            } else {
                mHasDerived.setChecked(false);
                // we don't need to do any other thing, {@code updateDerivedUnitDisplay} will take care of it
            }

            mReorderLevel.setText(non_null(Utils.formatDoubleForDisplay(mEditingItem.reorder_level)));
            mModelYear.setText(non_null(mEditingItem.model_year));
            mPartNumber.setText(non_null((mEditingItem.part_number)));
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_item, container, false);

            mName = (EditText) rootView.findViewById(R.id.edit_text_new_item_description);

            mUnitsSpinner = (Spinner) rootView.findViewById(R.id.spinner_new_item_unit_selection);
            mUnitsSpinner.setAdapter(new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    UnitsOfMeasurement.getAllUnits().toArray()));

            mTextBundleName = (TextView) rootView.findViewById(R.id.text_view_new_item_bundle_name);
            mDerivedName = (EditText) rootView.findViewById(R.id.edit_text_new_item_bundle_name);
            mTextBundleFactor = (TextView) rootView.findViewById(R.id.text_view_new_item_bundle_factor);
            mDerivedFactor = (EditText) rootView.findViewById(R.id.edit_text_new_item_bundle_factor);
            mFormula = (EditText) rootView.findViewById(R.id.edt_text_new_item_formula);
            mHasDerived = (CheckBox) rootView.findViewById(R.id.check_box_new_item_has_bundle);

            mCode = (EditText) rootView.findViewById(R.id.edit_text_new_item_manual_code);
            mReorderLevel = (EditText) rootView.findViewById(R.id.edit_text_new_item_reorder_level);
            mModelYear = (EditText) rootView.findViewById(R.id.edit_text_new_item_model_year);
            mPartNumber = (EditText) rootView.findViewById(R.id.edit_text_new_item_part_number);

            mCategoryBtn = (Button) rootView.findViewById(R.id.btn_new_item_category_selector);
            mOk = (Button) rootView.findViewById(R.id.btn_new_item_ok);
            mCancel = (Button) rootView.findViewById(R.id.btn_new_item_cancel);

            if (mIsEditing)
                setPreviousValuesAsInitialValues();
            else
                mFormula.setEnabled(false);

            mName.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) { setOkButtonStatus(); }
            });

            mUnitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateFormulaDisplay();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            mDerivedName.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateFormulaDisplay();
                    setOkButtonStatus();
                }
            });

            mDerivedFactor.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    updateFormulaDisplay();
                    setOkButtonStatus();
                }
            });

            mHasDerived.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDerivedUnitDisplay();
                    setOkButtonStatus();
                }
            });


            final AppCompatActivity activity = (AppCompatActivity) getActivity();

            if (mSelectedCategoryId == SheketContract.CategoryEntry.ROOT_CATEGORY_ID ||
                    mSelectedCategory == null) {
                mCategoryBtn.setText("Not Set");
            } else {
                mCategoryBtn.setText(mSelectedCategory.name);
            }
            mCategoryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);

                    ItemCategorySelectionFragment fragment = ItemCategorySelectionFragment.
                            newInstance(mSelectedCategoryId);
                    fragment.setListener(new ItemCategorySelectionFragment.SelectionListener() {
                        @Override
                        public void okSelected(long category_id, SCategory category) {
                            mSelectedCategoryId = category_id;
                            mSelectedCategory = category;
                            activity.getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void cancelSelected() {
                            activity.getSupportFragmentManager().popBackStack();
                        }
                    });
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.new_item_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            mOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final long item_id;
                    if (mIsEditing)
                        item_id = mEditingItem.item_id;
                    else {
                        long new_item_id = PrefUtil.getNewItemId(getActivity());
                        PrefUtil.setNewItemId(getActivity(), new_item_id);

                        item_id = new_item_id;
                    }

                    final String name = t(mName.getText().toString());

                    final int unit = mUnitsSpinner.getSelectedItemPosition();
                    final int has_derived_unit = SheketContract.toInt(
                            mHasDerived.isChecked());
                    final String derived_name = t(mDerivedName.getText().toString());

                    final long category_id = mSelectedCategoryId;

                    String factor = t(mDerivedFactor.getText().toString());
                    final double derived_factor = factor.isEmpty() ? 0 : Double.valueOf(factor);

                    String level = t(mReorderLevel.getText().toString());
                    final double reorder_level = level.isEmpty() ? 0 : Double.valueOf(level);

                    final String model_year = t(mModelYear.getText().toString());
                    final String part_number = t(mPartNumber.getText().toString());
                    final String code = t(mCode.getText().toString());
                    final String bar_code = "";

                    final String uuid;
                    final int change_status;
                    if (mIsEditing) {
                        uuid = mEditingItem.client_uuid;
                        change_status = mEditingItem.change_status == SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED ?
                                SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED :
                                SheketContract.ChangeTraceable.CHANGE_STATUS_UPDATED;
                    } else {
                        uuid = UUID.randomUUID().toString();
                        change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED;
                    }

                    final long company_id = PrefUtil.getCurrentCompanyId(getContext());
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

                            if (mIsEditing) {
                                activity.getContentResolver().update(ItemEntry.buildBaseUri(company_id), values,
                                        ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                                        new String[]{Long.toString(item_id)});
                            } else {
                                values.put(ItemEntry.COLUMN_ITEM_ID, item_id);
                                activity.getContentResolver().insert(
                                        ItemEntry.buildBaseUri(company_id), values);
                            }

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activity.finish();
                                }
                            });
                        }
                    };
                    t.start();
                }
            });
            mCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.finish();
                }
            });

            // To start things off
            updateDerivedUnitDisplay();
            setOkButtonStatus();

            return rootView;
        }

    }
}
